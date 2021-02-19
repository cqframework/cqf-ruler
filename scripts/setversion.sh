#!/usr/bin/env sh

# usage ./scripts/setversion.sh N.N.N (or N.N.N-SNAPSHOT)
mvn versions:set -DnewVersion=$1