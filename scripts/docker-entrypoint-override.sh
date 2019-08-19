#!/bin/sh

# TODO: we probably want to test the existence of the various variables before writing them to a file
# datasource.url= $DATASOURCE_URL
(
cat<<EOF
server_address=$SERVER_ADDRESS
EOF
) >> /var/lib/jetty/webapps/hapi.properties

/docker-entrypoint.sh "$@"