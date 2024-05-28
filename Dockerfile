FROM openjdk:18-slim-bullseye

ARG COMMIT_HASH
LABEL COMMIT_HASH ${COMMIT_HASH}
ENV COMMIT_HASH ${COMMIT_HASH}

ENV VERSION=0.15.0-SNAPSHOT
RUN apt-get update && apt-get upgrade -y && rm -rf /var/lib/apt/lists/*
RUN groupadd -r cqfruler && useradd -r -g cqfruler cqfruler
USER cqfruler
WORKDIR /home/cqfruler

RUN mkdir server
RUN mkdir plugin
COPY /server/target/cqf-ruler-server-${VERSION}.war server/ROOT.war

EXPOSE 8080
CMD ["java", "-cp", "server/ROOT.war", "-Dloader.path=WEB-INF/classes,WEB-INF/lib,WEB-INF/lib-provided,plugin", "org.springframework.boot.loader.launch.PropertiesLauncher"]
