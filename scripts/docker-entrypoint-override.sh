#!/bin/sh

# ensure the config directory / file is created, strip out any old server address append new server address
echo "configuring docker container"
echo "dstu3 address: " $SERVER_ADDRESS_DSTU3
echo "r4 address: " $SERVER_ADDRESS_R4

echo server_address=$SERVER_ADDRESS_DSTU3 >> /var/lib/jetty/webapps/dstu3.properties
if [ -e /var/lib/jetty/webapps/config/dstu3.properties ]; then 
    echo "importing dstu3 properties"
    sed '/server_address/d' /var/lib/jetty/webapps/config/dstu3.properties >> /var/lib/jetty/webapps/dstu3.properties
fi

echo server_address=$SERVER_ADDRESS_R4 >> /var/lib/jetty/webapps/r4.properties
if [ -e /var/lib/jetty/webapps/config/r4.properties ]; then 
    echo "importing r4 properties"
    sed '/server_address/d' /var/lib/jetty/webapps/config/r4.properties >> /var/lib/jetty/webapps/r4.properties
fi

# set Java options
DEFAULT_OPTIONS=" -XX:+UseContainerSupport -XX:MaxRAMPercentage=80.0 -Dhapi.properties.DSTU3=/var/lib/jetty/webapps/dstu3.properties -Dhapi.properties.R4=/var/lib/jetty/webapps/r4.properties"
if [ -z "$JAVA_OPTIONS" ]  
then  
    export JAVA_OPTIONS="$DEFAULT_OPTIONS"
else  
    export JAVA_OPTIONS="$JAVA_OPTIONS:$DEFAULT_OPTIONS"
fi  

exec /docker-entrypoint.sh "$@"