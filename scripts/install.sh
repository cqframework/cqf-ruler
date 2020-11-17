#!/usr/bin/env sh

mvn install -U -DskipTests=true -Dmaven.javadoc.skip=true -T 4 -B -V