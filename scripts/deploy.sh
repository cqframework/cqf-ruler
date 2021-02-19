#!/usr/bin/env bash

# Fail if any command fails or if there are unbound variables and check syntax
set -euxo pipefail
bash -n "$0"

if [[ "$TRAVIS_BRANCH" != "master" && -z "$TRAVIS_TAG" ]]
then
  echo "Not on the master branch or a git tag. Skipping deploy."
  exit 0
fi

if [[ ! -z "$TRAVIS_TAG" ]]
then
  export MAVEN_PROJECT_VERSION="$(mvn org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.version -q -DforceStdout)"
  echo "Maven project version is: $MAVEN_PROJECT_VERSION"
  if [[ "$TRAVIS_TAG" != "v$MAVEN_PROJECT_VERSION" ]]
  then
    echo "ERROR: Maven project version and tag do not match (expected tag v$MAVEN_PROJECT_VERSION)"
    exit 1
  fi
fi

# Import maven settings
cp .travis.settings.xml $HOME/.m2/settings.xml

CMD="mvn deploy -DskipTests=true -Dmaven.test.skip=true -T 4 -B -P ci"

# Import signing key and publish a release on a tag
if [[ ! -z "$TRAVIS_TAG" ]]
then
    echo "Publishing Maven Central release for tag: $TRAVIS_TAG"
    echo $GPG_SECRET_KEYS | base64 --decode| $GPG_EXECUTABLE --import;
    echo $GPG_OWNERTRUST | base64 --decode | $GPG_EXECUTABLE --import-ownertrust;
    # Activate the release profile
    CMD="$CMD,release"
else
   echo "Publishing Maven Central snapshot / pre-release for branch: $TRAVIS_BRANCH"
fi 

eval $CMD

echo "Building and publishing docker image"
# Set up for docker publish
DOCKER_IMAGE="contentgroup/cqf-ruler"
DOCKER_TAG=$TRAVIS_BRANCH
if [[ "$TRAVIS_BRANCH" == master ]]; then
  DOCKER_TAG="latest"
elif [[ ! -z "$TRAVIS_TAG" ]]; then
  DOCKER_TAG="$TRAVIS_TAG"
fi

# Push image to registry
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
docker build . -t "$DOCKER_IMAGE:$DOCKER_TAG"  && docker push "$DOCKER_IMAGE:$DOCKER_TAG"