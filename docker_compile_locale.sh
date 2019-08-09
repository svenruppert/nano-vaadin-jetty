#!/usr/bin/env bash

docker run \
      --rm \
      --name compile \
      -v "$(pwd)":/usr/src/mymaven \
      -w /usr/src/mymaven \
      svenruppert/maven-3.6.1-adopt:1.8.212-04:latest \
      mvn clean install -Dmaven.test.skip=true -Dvaadin-install-nodejs

