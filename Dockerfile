FROM maven:3.5.3-jdk-8 as builder

COPY . /build
WORKDIR /build
RUN mvn package -Dmaven.test.skip=true

FROM jetty:9-jre8-alpine as runner
USER jetty:jetty
COPY --from=builder ./build/target/cqf-ruler.war /var/lib/jetty/webapps/root.war
EXPOSE 8080
ENV JAVA_OPTIONS="-Dfhir.baseurl.r4=http://localhost:8080/baseR4 -Dfhir.baseurl.dstu3=http://localhost:8080/baseDstu3 -Dfhir.baseurl.dstu2=http://localhost:8080/baseDstu2"