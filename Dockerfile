FROM maven:3.5.3-jdk-8 as builder

COPY . /build
WORKDIR /build
RUN mvn package -Dmaven.test.skip=true

FROM jetty:9-jre8-alpine as runner
USER jetty:jetty
RUN mkdir -p /var/lib/jetty/target
COPY --from=builder /build/cqf-ruler-dstu3/target/cqf-ruler-dstu3.war /var/lib/jetty/webapps/cqf-ruler-dstu3.war
COPY --from=builder /build/cqf-ruler-r4/target/cqf-ruler-r4.war /var/lib/jetty/webapps/cqf-ruler-r4.war
EXPOSE 8080

ENV SERVER_ADDRESS_DSTU3="http://localhost:8080/cqf-ruler-dstu3/fhir"
ENV SERVER_ADDRESS_R4="http://localhost:8080/cqf-ruler-r4/fhir"
# TODO: Handle these. We probably want some convention to map between the ENV variables and the hapi.properties
# ENV SERVER_BASE="/cqf-ruler/baseDstu3"
# ENV HIBERNATE_DIALECT="hibernate.dialect=ca.uhn.fhir.jpa.util.DerbyTenSevenHapiFhirDialect"
# ENV DATASOURCE_DRIVER="org.apache.derby.jdbc.EmbeddedDriver"
# ENV DATASOURCE_URL="jdbc:derby:directory:target/jpaserver_derby_files;create=true"
# ENV DATASOURCE_URL= DATASOURCE_USERNAME=
ENV JAVA_OPTIONS="-Dhapi.properties.DSTU3=/var/lib/jetty/webapps/dstu3.properties -Dhapi.properties.R4=/var/lib/jetty/webapps/r4.properties"

COPY --chown=jetty:jetty ./scripts/docker-entrypoint-override.sh /docker-entrypoint-override.sh
ENTRYPOINT [ "/docker-entrypoint-override.sh" ]


# FROM runner as test-data
# COPY --chown=jetty:jetty ./target/jpaserver_derby_files  /var/lib/jetty/target/jpaserver_derby_files