FROM svenruppert/adopt:1.8.212-04
MAINTAINER sven.ruppert@gmail.com
ARG USER_HOME_DIR="/root"
COPY 03_demo/target/vaadin-app.jar .
EXPOSE 8899
CMD ["java", "-jar", "vaadin-app.jar"]