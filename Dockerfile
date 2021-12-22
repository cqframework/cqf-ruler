FROM openjdk:11

RUN mkdir server
RUN mkdir plugin
COPY ./server/target/cqf-ruler-server-*.war server/ROOT.war
COPY ./plugin/**/target/cqf-ruler-plugin-*.jar plugin
EXPOSE 8080
CMD ["java", "-cp", "server/ROOT.war", "-Dloader.path=WEB-INF/classes,WEB-INF/lib,WEB-INF/lib-provided$,plugin", "org.springframework.boot.loader.PropertiesLauncher"]
