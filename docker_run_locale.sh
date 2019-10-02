#!/bin/bash

docker run \
       -it \
       -p 8899:8899 \
       --rm \
       --name run \
       -v "$(pwd)":/usr/src/mymaven \
       -w /usr/src/mymaven \
       svenruppert/adopt:1.8.212-04:latest \
       java -jar 03_demo/target/vaadin-app.jar
