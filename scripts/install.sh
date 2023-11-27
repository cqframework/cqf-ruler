#!/usr/bin/env bash

# Fail if any command fails or if there are unbound variables and check syntax
set -euxo pipefail
bash -n "$0"
# install base
mvn install -U -DskipTests=true -Dmaven.javadoc.skip=true -T 4 -B -V
# install plugin
cd ./ecr
mvn package -U -DskipTests=true -Dmaven.javadoc.skip=true -T 4 -B -V