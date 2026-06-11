#!/usr/bin/env bash
# Publish nano-vaadin-jetty to Sonatype Central Portal — the automated path.
#
# Runs the test suite, the PIT mutation gate, and finally the parent-bundled
# _deploy / _release_prepare / _release_sign-artifacts profiles, which
# upload the signed artefacts via central-publishing-maven-plugin. The
# artefacts land in the Central Portal as a staged deployment.
#
# Compare scripts/build-central-bundle.sh, which builds the same artefacts
# into a ZIP under target/ without uploading — for inspection or manual
# upload through https://central.sonatype.com/publishing/deployments.
#
# Usage:
#   scripts/publish-to-central.sh                  full run
#   scripts/publish-to-central.sh --skip-tests     skip surefire (PIT still runs)
#   scripts/publish-to-central.sh --skip-mutation  skip PIT (surefire still runs)
#   scripts/publish-to-central.sh --allow-snapshot permit -SNAPSHOT version
#   scripts/publish-to-central.sh --dry-run        everything except `deploy`
#   scripts/publish-to-central.sh --help           print this usage

set -euo pipefail

readonly SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
readonly PROJECT_ROOT="$( cd "${SCRIPT_DIR}/.." && pwd )"
readonly MVN="${PROJECT_ROOT}/mvnw"
readonly SETTINGS="${HOME}/.m2/settings.xml"
readonly REQUIRED_MUTATION_COVERAGE=100

skip_tests=0
skip_mutation=0
allow_snapshot=0
dry_run=0

for arg in "$@"; do
  case "$arg" in
    --skip-tests)     skip_tests=1 ;;
    --skip-mutation)  skip_mutation=1 ;;
    --allow-snapshot) allow_snapshot=1 ;;
    --dry-run)        dry_run=1 ;;
    -h|--help)
      sed -n '2,19p' "${BASH_SOURCE[0]}" | sed 's/^# \?//'
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

# ---------------------------------------------------------------- Pre-flight

step "Pre-flight checks"

[[ -x "$MVN" ]] || die "mvnw not found or not executable at $MVN"

if ! git -C "$PROJECT_ROOT" diff-index --quiet HEAD --; then
  die "working tree not clean — commit or stash modifications first"
fi
if [[ -n "$(git -C "$PROJECT_ROOT" ls-files --others --exclude-standard)" ]]; then
  die "untracked files present — gitignore or commit them first"
fi
ok "git working tree clean"

if ! gpg --list-secret-keys --keyid-format=long >/dev/null 2>&1 || \
   [[ -z "$(gpg --list-secret-keys --keyid-format=long 2>/dev/null)" ]]; then
  die "no GPG secret key available — Central requires signed artefacts"
fi
ok "GPG secret key present"

if [[ ! -f "$SETTINGS" ]] || ! grep -q '<id>central</id>' "$SETTINGS"; then
  die "$SETTINGS missing or does not declare <server id=\"central\">"
fi
ok "Central server credentials present in $SETTINGS"

if command -v xmllint >/dev/null; then
  VERSION="$(xmllint --xpath "/*[local-name()='project']/*[local-name()='version']/text()" \
                     "${PROJECT_ROOT}/pom.xml" 2>/dev/null)"
else
  VERSION="$("$MVN" -q -f "${PROJECT_ROOT}/pom.xml" \
              help:evaluate -Dexpression=project.version -DforceStdout 2>/dev/null \
              | tail -1 | sed -E 's/^\[[A-Z]+\] \[stdout\] //')"
fi
[[ -n "$VERSION" ]] || die "could not read project.version from pom.xml"

if [[ "$VERSION" == *-SNAPSHOT && "$allow_snapshot" -eq 0 ]]; then
  die "project version is $VERSION — bump to a release version, or pass --allow-snapshot"
fi
ok "project version: $VERSION"

# ----------------------------------------------------------- Quality gates

if (( skip_tests == 0 )); then
  step "Tests"
  ( cd "$PROJECT_ROOT" && "$MVN" -B clean test )
  ok "tests green"
fi

if (( skip_mutation == 0 )); then
  step "Mutation coverage gate (target >= ${REQUIRED_MUTATION_COVERAGE}%)"
  ( cd "$PROJECT_ROOT" && "$MVN" -B org.pitest:pitest-maven:mutationCoverage )
  report="${PROJECT_ROOT}/target/pit-reports/mutations.xml"
  [[ -f "$report" ]] || die "PIT report missing at $report"
  generated=$(grep -c '<mutation ' "$report" || true)
  killed=$(grep -c "status='KILLED'" "$report" || true)
  pct=0
  if (( generated > 0 )); then
    pct=$(( 100 * killed / generated ))
  fi
  printf '    killed %d/%d (%d%%)\n' "$killed" "$generated" "$pct"
  (( pct >= REQUIRED_MUTATION_COVERAGE )) || \
    die "mutation coverage $pct%% < ${REQUIRED_MUTATION_COVERAGE}%% — investigate target/pit-reports/index.html"
  ok "mutation coverage met"
fi

# ---------------------------------------------------------------- Deploy

step "Deploy $VERSION to Sonatype Central Portal"
if (( dry_run == 1 )); then
  echo "    [dry-run] would run:"
  echo "    (cd $PROJECT_ROOT && $MVN -B -P _deploy,_release_prepare,_release_sign-artifacts deploy)"
  ok "dry-run finished"
  exit 0
fi

( cd "$PROJECT_ROOT" && "$MVN" -B -P _deploy,_release_prepare,_release_sign-artifacts deploy )
ok "uploaded $VERSION to Central"

# --------------------------------------------------------- Post-deploy hint

step "Next steps"
cat <<EOF
    1. Tag the release:   git tag -s "v$VERSION" -m "Release $VERSION"
    2. Push the tag:      git push origin "v$VERSION"
    3. Bump pom.xml to the next -SNAPSHOT and commit.
    4. Watch the Central Portal UI; the upload is staged, not yet published.
EOF
