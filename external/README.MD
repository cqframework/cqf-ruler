# Readme

## Overview

This project only exists to copy and compile the sources from the [HAPI FHIR JPA Server Starter](https://github.com/hapifhir/hapi-fhir-jpaserver-starter) project. The cqf-ruler builds on the starter server, but class jars are not regularly published for that project so we create a `cqf-ruler-external` library here. If the starter server becomes regularly published, this module will be retired.

The `hapi-fhir-jpaserver-starter` project is added as a git sub-module. It's pinned to specific tag. From there, the src code is copied as a `generated-sources` for the `cql-ruler-external` project and minimally processed to suppress warnings and change the package name so that we don't publish anything that would collide with Smile's work.

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

## Future Work

Eventually, it'd be ideal for the cqf-ruler components to be able to be dropped into the `hapi-fhir-server` directly. There are a few dependencies that exist in the cqf-ruler projects that make that not possible for the time being. We need to coordinate with the Smile/HAPI Team to identify of subset of this code that could be migrated to the HAPI base server. For example:

* Condition Annotations (e.g. `OnDstuCondition.class`)
