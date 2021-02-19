#!/usr/bin/env bash

# Fail if any command fails or if there are unbound variables and check syntax
set -euxo pipefail
bash -n "$0"

mvn versions:update-properties versions:use-releases versions:use-latest-releases