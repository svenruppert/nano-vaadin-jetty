#!/bin/bash

docker run \
       -it \
       -p 8899:8899 \
       --rm \
       --name nanovaadin-jetty \
       nanovaadin/jetty:latest

