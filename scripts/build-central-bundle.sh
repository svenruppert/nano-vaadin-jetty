#!/usr/bin/env bash
# Build a Sonatype Central Portal bundle ZIP without uploading it.
#
# Produces a ZIP under the Maven-standard
# groupId/artifactId/version/ layout containing main jar, sources jar,
# javadoc jar, pom, plus GPG signatures (.asc) and MD5/SHA1/SHA256/SHA512
# checksums for each. Drop the ZIP onto the Central Portal at
# https://central.sonatype.com/publishing/deployments for manual upload.
#
# Compare scripts/publish-to-central.sh, which packages the same artefacts
# and uploads them via central-publishing-maven-plugin in one shot (and
# additionally enforces a test + PIT mutation gate).
#
# Usage:
#   scripts/build-central-bundle.sh                  build bundle for release version
#   scripts/build-central-bundle.sh --allow-snapshot permit -SNAPSHOT version
#   scripts/build-central-bundle.sh --keep-workdir   keep the staging dir for inspection
#   scripts/build-central-bundle.sh --help           print this usage

set -euo pipefail

readonly SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
readonly PROJECT_ROOT="$( cd "${SCRIPT_DIR}/.." && pwd )"
readonly MVN="${PROJECT_ROOT}/mvnw"

allow_snapshot=0
keep_workdir=0

for arg in "$@"; do
  case "$arg" in
    --allow-snapshot) allow_snapshot=1 ;;
    --keep-workdir)   keep_workdir=1 ;;
    -h|--help)
      sed -n '2,18p' "${BASH_SOURCE[0]}" | sed 's/^# \?//'
      exit 0
      ;;
    *)
      echo "unknown argument: $arg" >&2
      exit 64
      ;;
  esac
done

step() { printf '\n\033[1;34m> %s\033[0m\n' "$*"; }
ok()   { printf '\033[1;32mOK  %s\033[0m\n' "$*"; }
die()  { printf '\033[1;31mERR %s\033[0m\n' "$*" >&2; exit 1; }

# Hash helpers — work on both macOS (md5, shasum) and Linux (md5sum, sha*sum).
md5_hex()    { command -v md5sum   >/dev/null && md5sum   "$1" | awk '{print $1}' || md5 -q "$1"; }
sha1_hex()   { command -v sha1sum  >/dev/null && sha1sum  "$1" | awk '{print $1}' || shasum -a 1   "$1" | awk '{print $1}'; }
sha256_hex() { command -v sha256sum >/dev/null && sha256sum "$1" | awk '{print $1}' || shasum -a 256 "$1" | awk '{print $1}'; }
sha512_hex() { command -v sha512sum >/dev/null && sha512sum "$1" | awk '{print $1}' || shasum -a 512 "$1" | awk '{print $1}'; }

pom_field() {
  # Project-level field directly (skips the <parent> block); avoids a
  # Maven JVM cold-start per coordinate. Falls back to mvn if xmllint
  # is unavailable.
  if command -v xmllint >/dev/null; then
    xmllint --xpath "/*[local-name()='project']/*[local-name()='$1']/text()" \
            "${PROJECT_ROOT}/pom.xml" 2>/dev/null
  else
    "$MVN" -q -f "${PROJECT_ROOT}/pom.xml" \
      help:evaluate -Dexpression="project.$1" -DforceStdout 2>/dev/null \
      | tail -1 | sed -E 's/^\[[A-Z]+\] \[stdout\] //'
  fi
}

# ---------------------------------------------------------------- Pre-flight

step "Pre-flight"

[[ -x "$MVN" ]] || die "mvnw not found or not executable at $MVN"

if ! gpg --list-secret-keys --keyid-format=long >/dev/null 2>&1 || \
   [[ -z "$(gpg --list-secret-keys --keyid-format=long 2>/dev/null)" ]]; then
  die "no GPG secret key available — Central artefacts must be signed"
fi
ok "GPG secret key present"

GROUP_ID="$(pom_field groupId)"
ARTIFACT_ID="$(pom_field artifactId)"
VERSION="$(pom_field version)"
[[ -n "$GROUP_ID" && -n "$ARTIFACT_ID" && -n "$VERSION" ]] \
  || die "failed to read coordinates from pom.xml"

if [[ "$VERSION" == *-SNAPSHOT && "$allow_snapshot" -eq 0 ]]; then
  die "version is $VERSION — Central rejects SNAPSHOT; pass --allow-snapshot if intended"
fi
ok "coordinates: $GROUP_ID:$ARTIFACT_ID:$VERSION"

# ----------------------------------------------------------- Build artefacts

step "Build artefacts (sources + javadoc + signatures)"
( cd "$PROJECT_ROOT" && "$MVN" -B -DskipTests -P _release_prepare,_release_sign-artifacts clean package )

readonly TARGET="${PROJECT_ROOT}/target"
readonly JAR="${TARGET}/${ARTIFACT_ID}-${VERSION}.jar"
readonly SOURCES_JAR="${TARGET}/${ARTIFACT_ID}-${VERSION}-sources.jar"
readonly JAVADOC_JAR="${TARGET}/${ARTIFACT_ID}-${VERSION}-javadoc.jar"
readonly POM_SRC="${PROJECT_ROOT}/pom.xml"

for f in "$JAR" "$SOURCES_JAR" "$JAVADOC_JAR" \
         "${JAR}.asc" "${SOURCES_JAR}.asc" "${JAVADOC_JAR}.asc"; do
  [[ -f "$f" ]] || die "missing build output: $f"
done
ok "jar / sources / javadoc + signatures present"

# -------------------------------------------------------------- Stage bundle

step "Stage Maven-Central layout"

WORKDIR="$(mktemp -d "${TARGET}/central-bundle.XXXXXX")"
cleanup() {
  if (( keep_workdir == 0 )); then
    rm -rf "$WORKDIR"
  else
    echo "    workdir kept at $WORKDIR"
  fi
}
trap cleanup EXIT

readonly LAYOUT="${WORKDIR}/$(echo "$GROUP_ID" | tr '.' '/')/${ARTIFACT_ID}/${VERSION}"
mkdir -p "$LAYOUT"

# Each entry: <staged-filename>:<source-path>
stage_entries=(
  "${ARTIFACT_ID}-${VERSION}.jar:${JAR}"
  "${ARTIFACT_ID}-${VERSION}-sources.jar:${SOURCES_JAR}"
  "${ARTIFACT_ID}-${VERSION}-javadoc.jar:${JAVADOC_JAR}"
  "${ARTIFACT_ID}-${VERSION}.pom:${POM_SRC}"
)

for entry in "${stage_entries[@]}"; do
  name="${entry%%:*}"
  src="${entry#*:}"
  dst="${LAYOUT}/${name}"

  cp "$src" "$dst"

  if [[ -f "${src}.asc" ]]; then
    cp "${src}.asc" "${dst}.asc"
  else
    gpg --detach-sign --armor --batch --yes -o "${dst}.asc" "$dst"
  fi

  printf '%s' "$(md5_hex    "$dst")" > "${dst}.md5"
  printf '%s' "$(sha1_hex   "$dst")" > "${dst}.sha1"
  printf '%s' "$(sha256_hex "$dst")" > "${dst}.sha256"
  printf '%s' "$(sha512_hex "$dst")" > "${dst}.sha512"

  printf '    %s\n' "$name"
done
ok "staged 4 artefacts × {file, .asc, .md5, .sha1, .sha256, .sha512}"

# ------------------------------------------------------------------- Zip up

step "Zip bundle"

readonly BUNDLE_ZIP="${TARGET}/central-bundle-${VERSION}.zip"
rm -f "$BUNDLE_ZIP"
( cd "$WORKDIR" && zip -qr "$BUNDLE_ZIP" . )
ok "wrote $BUNDLE_ZIP ($(du -h "$BUNDLE_ZIP" | awk '{print $1}'))"

# -------------------------------------------------------------- ZIP summary

step "Bundle contents"
unzip -l "$BUNDLE_ZIP" | tail -n +4 | head -n -2

step "Next steps"
cat <<EOF
    1. Inspect the bundle:
         unzip -l $BUNDLE_ZIP
    2. Verify a signature, e.g.:
         unzip -p $BUNDLE_ZIP ${GROUP_ID//.//}/${ARTIFACT_ID}/${VERSION}/${ARTIFACT_ID}-${VERSION}.jar.asc \\
           | gpg --verify - <(unzip -p $BUNDLE_ZIP ${GROUP_ID//.//}/${ARTIFACT_ID}/${VERSION}/${ARTIFACT_ID}-${VERSION}.jar)
    3. Upload at https://central.sonatype.com/publishing/deployments
       (or use scripts/publish-to-central.sh for the maven-plugin upload path).
EOF
