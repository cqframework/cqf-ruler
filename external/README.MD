# Readme

## Overview

This project only exists to copy and compile the sources from the [HAPI FHIR JPA Server Starter](https://github.com/hapifhir/hapi-fhir-jpaserver-starter) project. The cqf-ruler builds on the starter server, but class jars are not regularly published for that project so we create a `cqf-ruler-external` library here.

The `hapi-fhir-jpaserver-starter` project is added as a git sub-module. It's pinned to specific tag. From there, the src code is copied as a `generated-sources` for the `cql-ruler-external` project and minimally processed to suppress warnings.

The `resources` content is embedded into the jar, while the `webapp` content is not.

## Setting up

If you've already cloned the repository use the following command to update the submodules

```bash
git submodule update --init --recursive
```

## Updating

First, check out the version of the `hapi-fhir-jpaserver-starter` project you want:

```bash
cd external/hapi-fhir-jpaserver-starter
git checkout <tag>
```

Then manually copy any `webapp` content as necessary. There templates for the tester page there, among other things. Some of these are customized so it requires a manual merge.

Next, attempt to build the project. All the dependency versions in the `cql-ruler-external` pom are managed by the root `cqf-ruler` pom. You may need to add dependencies, update versions, etc. in order to get it to build correctly.
