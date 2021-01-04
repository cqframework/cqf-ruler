FROM jetty:9-jre11

USER jetty:jetty

# Default database directory
RUN mkdir -p /var/lib/jetty/target

#Default config directory
RUN mkdir -p /var/lib/jetty/webapps/config

COPY --chown=jetty:jetty ./cqf-ruler-dstu3/target/cqf-ruler-dstu3.war /var/lib/jetty/webapps/cqf-ruler-dstu3.war
COPY --chown=jetty:jetty ./cqf-ruler-r4/target/cqf-ruler-r4.war /var/lib/jetty/webapps/cqf-ruler-r4.war
EXPOSE 8080

ENV SERVER_ADDRESS_DSTU3="http://localhost:8080/cqf-ruler-dstu3/fhir"
ENV SERVER_ADDRESS_R4="http://localhost:8080/cqf-ruler-r4/fhir"
ENV JAVA_OPTIONS=""

COPY --chown=jetty:jetty ./scripts/docker-entrypoint-override.sh /docker-entrypoint-override.sh
ENTRYPOINT [ "/docker-entrypoint-override.sh" ]


# FROM runner as test-data
# COPY --chown=jetty:jetty ./target/jpaserver_derby_files  /var/lib/jetty/target/jpaserver_derby_files