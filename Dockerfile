FROM maven:3.5.3-jdk-8 as builder

COPY . /build
WORKDIR /build
RUN mvn package -Dmaven.test.skip=true

FROM jetty:9-jre8-alpine as runner
USER jetty:jetty
RUN mkdir -p /var/lib/jetty/target
COPY --from=builder ./build/target/cqf-ruler.war /var/lib/jetty/webapps/cqf-ruler.war
EXPOSE 8080

ENV SERVER_ADDRESS="http://localhost:8080/cqf-ruler/baseDstu3"

# TODO: Handle these. We probably want some convention to map between the ENV variables and the 
# ENV SERVER_BASE="/cqf-ruler/baseDstu3"
# ENV HIBERNATE_DIALECT="hibernate.dialect=ca.uhn.fhir.jpa.util.DerbyTenSevenHapiFhirDialect"
# ENV DATASOURCE_DRIVER="org.apache.derby.jdbc.EmbeddedDriver"
# ENV DATASOURCE_URL="jdbc:derby:directory:target/jpaserver_derby_files;create=true"
# ENV DATASOURCE_URL= DATASOURCE_USERNAME=
ENV JAVA_OPTIONS="-Dhapi.properties=/var/lib/jetty/webapps/hapi.properties"

COPY --chown=jetty:jetty ./scripts/docker-entrypoint-override.sh /docker-entrypoint-override.sh
ENTRYPOINT [ "/docker-entrypoint-override.sh" ]

# Assumes the existence of a stu3 directory
# TODO: runtime mounting. Need to use gosu or similar to handle permisions correctly.
FROM runner as test-data
COPY --chown=jetty:jetty ./target/stu3  /var/lib/jetty/target/stu3