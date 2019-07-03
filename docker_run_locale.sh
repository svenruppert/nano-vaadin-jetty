#!/bin/bash

docker run \
       -it \
       -p 8899:8899 \
       --rm \
       --name run \
       -v "$(pwd)":/usr/src/mymaven \
       -w /usr/src/mymaven \
       working/vaadin-v14-prepared:latest \
       java -jar target/vaadin-app.jar

#docker run -it \
#      -p 8080:8080 \
#      -p 5005:5005 \
#      --rm \
#      --name run \
#      -v "$(pwd)":/usr/src/mymaven \
#      -w /usr/src/mymaven \
#      svenruppert/maven-3.5-jdk-oracle-08 \
#      mvn meecrowave:bake \
#      -DargLine="-Xmx256m -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"