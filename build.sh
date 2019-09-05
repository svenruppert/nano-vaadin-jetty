#!/bin/bash
docker build -t nanovaadin/jetty .
#docker tag nanovaadin/jetty:latest nanovaadin/jetty:20190826-001
#docker push nanovaadin/jetty:20190826-001
docker push nanovaadin/jetty:latest
