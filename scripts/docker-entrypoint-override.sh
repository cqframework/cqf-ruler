#!/bin/sh

# TODO: we probably want to test the existence of the various variables before writing them to a file
# datasource.url= $DATASOURCE_URL
(
cat<<EOF
server_address=$SERVER_ADDRESS_DSTU3
EOF
) >> /var/lib/jetty/webapps/dstu3.properties

(
cat<<EOF
server_address=$SERVER_ADDRESS_R4
EOF
) >> /var/lib/jetty/webapps/r4.properties

exec /docker-entrypoint.sh "$@"