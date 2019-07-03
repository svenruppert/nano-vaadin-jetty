FROM svenruppert/maven-3.6.1-openjdk:1.11.0-2
MAINTAINER sven.ruppert@gmail.com

ARG USER_HOME_DIR="/root"

#RUN curl -sL https://deb.nodesource.com/setup_12.x | bash - \
#RUN curl -sL https://deb.nodesource.com/setup_11.x | bash - \
RUN curl -sL https://deb.nodesource.com/setup_10.x | bash - \
    && apt-get install -y nodejs

WORKDIR /build
#do not delete it, it is like a reference
COPY pom.xml .
RUN mvn dependency:go-offline

ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"

CMD ["mvn"]