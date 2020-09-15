#!/bin/sh

# TODO: we probably want to test the existence of the various variables before writing them to a file
# datasource.url= $DATASOURCE_URL
# ensure the config directory / file is created, strip out any old server address, pre-prend new server address
echo "Configuring Docker container"
echo "DSTU3 address: " $SERVER_ADDRESS_DSTU3

echo "R4 address: " $SERVER_ADDRESS_R4
cd /var/lib/jetty/webapps/config

echo server_address=$SERVER_ADDRESS_DSTU3 > temp-dstu3.properties
touch dstu3.properties
grep -v "server_address" dstu3.properties >> temp-dstu3.properties
cp temp-dstu3.properties dstu3.properties
rm temp-dstu3.properties

echo server_address=$SERVER_ADDRESS_R4 > temp-r4.properties
touch r4.properties
grep -v "server_address" r4.properties >> temp-r4.properties
cp temp-r4.properties r4.properties
rm temp-r4.properties

cd /var/lib/jetty
exec /docker-entrypoint.sh "$@"