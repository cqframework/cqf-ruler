FROM openjdk:18-slim-bullseye

RUN apt-get update && apt-get upgrade -y && rm -rf /var/lib/apt/lists/*

RUN mkdir server
RUN mkdir plugin
COPY ./server/target/cqf-ruler-server-*.war server/ROOT.war

RUN mkdir extlib
COPY ./server/target/cqf-ruler-server-0.5.0-SNAPSHOT/WEB-INF/lib/org.eclipse.persistence.core-2.7.7.jar extlib/org.eclipse.persistence.core-2.7.7.jar
COPY ./server/target/cqf-ruler-server-0.5.0-SNAPSHOT/WEB-INF/lib/org.eclipse.persistence.asm-2.7.7.jar extlib/org.eclipse.persistence.asm-2.7.7.jar
COPY ./server/target/cqf-ruler-server-0.5.0-SNAPSHOT/WEB-INF/lib/org.eclipse.persistence.moxy-2.7.7.jar extlib/org.eclipse.persistence.moxy-2.7.7.jar
COPY ./server/target/cqf-ruler-server-0.5.0-SNAPSHOT/WEB-INF/lib/jaxb-api-2.3.1.jar extlib/jaxb-api-2.3.1.jar
COPY ./server/target/cqf-ruler-server-0.5.0-SNAPSHOT/WEB-INF/lib/javax.activation-api-1.2.0.jar extlib/javax.activation-api-1.2.0.jar

EXPOSE 8080
CMD ["java", "--module-path", "extlib/org.eclipse.persistence.core-2.7.7.jar:extlib/org.eclipse.persistence.asm-2.7.7.jar:extlib/org.eclipse.persistence.moxy-2.7.7.jar:extlib/jaxb-api-2.3.1.jar:extlib/javax.activation-api-1.2.0.jar", "--add-modules", "ALL-MODULE-PATH", "-cp", "server/ROOT.war", "-Dloader.path=WEB-INF/classes,WEB-INF/lib,WEB-INF/lib-provided,plugin", "org.springframework.boot.loader.PropertiesLauncher"]
