#!/usr/bin/env bash

docker run \
      --rm \
      --name compile \
      -v "$(pwd)":/usr/src/mymaven \
      -w /usr/src/mymaven \
      working/vaadin-v14-prepared:latest \
      mvn clean install -Dmaven.test.skip=true -DactivateNodeJS_NPM=true

#      svenruppert/maven-3.6.1-openjdk:1.11.0-2
#      svenruppert/nodejs-maven-3.6.1-adopt:1.8.212-04 \
#      mvn com.github.eirslett:frontend-maven-plugin:1.7.6:install-node-and-npm \
#      -DnodeVersion="v10.16.0"  \
#      clean install -Dmaven.test.skip=true
