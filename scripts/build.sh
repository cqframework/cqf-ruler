#!/usr/bin/env bash

# Fail if any command fails or if there are unbound variables and check syntax
set -euxo pipefail
bash -n "$0"

# Run tests
CMD="mvn test -T 4 -B"
if [[ "$TRAVIS_BRANCH" =~ master* ]]; then CMD="$CMD -P release"; fi
eval $CMD


# Run Deploy
CMD="mvn deploy -DskipTests=true -T 4 -B"

# Import maven settings
cp .travis.settings.xml $HOME/.m2/settings.xml

# Import signing key
if [[ "$TRAVIS_BRANCH" =~ master* ]]; then
    echo $GPG_SECRET_KEYS | base64 --decode| $GPG_EXECUTABLE --import;
    echo $GPG_OWNERTRUST | base64 --decode | $GPG_EXECUTABLE --import-ownertrust;
    CMD="$CMD -P release"
fi 

eval $CMD

# Set up for docker publish
DOCKER_IMAGE="contentgroup/cqf-ruler"
DOCKER_TAG=$TRAVIS_BRANCH
if [[ "$TRAVIS_BRANCH" =~ master* ]]; then
  DOCKER_TAG="latest"
fi

# Push image to registry
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
docker build . -t "$DOCKER_IMAGE:$DOCKER_TAG"  && docker push "$DOCKER_IMAGE:$DOCKER_TAG"