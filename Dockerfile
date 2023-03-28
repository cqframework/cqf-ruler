FROM maven:3.8.7-openjdk-18 AS MAVEN_BUILD
RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
ADD . /usr/src/app

RUN unset MAVEN_CONFIG && env &&  ./mvnw package
RUN  mv server/target/*.war /app.war



FROM openjdk:18-slim-bullseye

ARG COMMIT_HASH
LABEL COMMIT_HASH ${COMMIT_HASH}
ENV COMMIT_HASH ${COMMIT_HASH}

RUN apt-get update && apt-get upgrade -y && rm -rf /var/lib/apt/lists/*

RUN mkdir server
RUN mkdir plugin
COPY --from=MAVEN_BUILD /app.war server/ROOT.war

EXPOSE 8080
CMD ["java", "-cp", "server/ROOT.war", "-Dloader.path=WEB-INF/classes,WEB-INF/lib,WEB-INF/lib-provided,plugin", "org.springframework.boot.loader.PropertiesLauncher"]
~                                                                                                                                                                          