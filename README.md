# cqf-ruler

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.opencds.cqf/cqf-ruler-server/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.opencds.cqf/cqf-ruler-server) [![Build Status](https://www.travis-ci.com/github/DBCG/cqf-ruler.svg?branch=master)](https://www.travis-ci.com/github/DBCG/cqf-ruler) [![docker image](https://img.shields.io/docker/v/alphora/cqf-ruler/latest?style=flat&color=brightgreen&label=docker%20image)](https://hub.docker.com/r/alphora/cqf-ruler/tags) [![project chat](https://img.shields.io/badge/zulip-join_chat-brightgreen.svg)](https://chat.fhir.org/#narrow/stream/179220-cql)

The cqf-ruler is based on the [HAPI FHIR JPA Server Starter](https://github.com/hapifhir/hapi-fhir-jpaserver-starter) and adds a set of plugins that provide an implementation of FHIR's [Clinical Reasoning Module](
http://hl7.org/fhir/clinicalreasoning-module.html), serve as a
knowledge artifact repository, and a [cds-hooks](https://cds-hooks.org/) compatible clinical decision support service. The cqf-ruler provides an [extensibility API](#plugins) to allow adding custom FHIR operations without the need to fork or clone the entire project.

See the [wiki](https://github.com/DBCG/cqf-ruler/wiki/Home) for more information

## Usage

### Public Sandbox

The public sandbox is not persistent, has no authentication, and is regularly reset. Do not store any sensitive data, PHI, or anything you need to be persistent on the sandbox:

[GUI](https://cloud.alphora.com/sandbox/r4/cqm/)

[Open API Docs](https://cloud.alphora.com/sandbox/r4/cqm/fhir/api-docs)

[Swagger UI](https://cloud.alphora.com/sandbox/r4/cqm/fhir/swagger-ui/)

### Docker

The easiest way to get started with the cqf-ruler is to pull and run the docker image

```bash
docker pull alphora/cqf-ruler
docker run -p 8080:8080 alphora/cqf-ruler
```

This will make the cqf-ruler available on <http://localhost:8080>

Other options for deployment are listed on the [wiki](https://github.com/DBCG/cqf-ruler/wiki/Deployment) for more documentation.

### GUI

The cqf-ruler provides the HAPI FHIR Tester UI which allows FHIR Resource CRUD at the server base url. That's <http://localhost:8080> if you're running with the Docker command above.

### Swagger / Open API

The cqf-ruler provides Swagger UI for the REST API and test interface at [http://localhost:8080/fhir/swagger-ui/](http://localhost:8080/fhir/swagger-ui/).

Similarly, Open API docs are available at [http://localhost:8080/fhir/api-docs](http://localhost:8080/fhir/api-docs).

### Example Operations

The [wiki](https://github.com/DBCG/cqf-ruler/wiki) provides walkthroughs of several of the features of the cqf-ruler such as [Quality Measure Processing](https://github.com/DBCG/cqf-ruler/wiki/Quality-Measure-Processing).

In general, you need to load the cqf-ruler with FHIR Resources prior to using it. One way to do this is to start a cqf-ruler server and use the FHIR REST API to load resources. See [Resource Loading](https://github.com/DBCG/cqf-ruler/wiki/Resource-Loading) on the wiki for a description of how to do that.

## Development

### Dependencies

#### Git Submodules

This project includes the `hapi-fhir-jpaserver-starter` project as a submodule and includes the compiled classes as a jar called `cqf-ruler-external`. Be sure to use the following command when cloning this repository to ensure the submodules are initialized correctly:

`git clone --recurse-submodules https://github.com/DBCG/cqf-ruler.git`

or if you've already checked out the repo, use

`git submodule update --init --recursive`

to clean up any unneeded or unused files, use:

`git clean -fdx`

#### Java

Go to [http://www.oracle.com/technetwork/java/javase/downloads/](
http://www.oracle.com/technetwork/java/javase/downloads/) and download the
latest (version 11 or higher) JDK for your platform, and install it.

#### Apache Maven

This project uses the [Maven wrapper](https://github.com/apache/maven-wrapper) to ensure the correct version of Maven is available on your machine. Use `./mvnw` to invoke it.

### Build

`./mvnw package`

### Run

To run the cqf-ruler directory from this project use:

`java -jar server/target/cqf-ruler-server-*.war`

### Contributing

See [Contributing](https://github.com/DBCG/cqf-ruler/wiki/Contributing) on the wiki for more information about developing and contributing to the cqf-ruler project.

## Plugins

The cqf-ruler offers a basic plugin framework to allow the addition of custom FHIR operations. See [Plugins](https://github.com/DBCG/cqf-ruler/wiki/Architecture#Plugins) on the wiki for more information.

A complete example of a dynamically loaded plugin is available in the [example](/example) directory.

## Architecture

See [Architecture](https://github.com/DBCG/cqf-ruler/wiki/Architecture) on the wiki.

## Getting Help

Additional documentation is on the [wiki](https://github.com/DBCG/cqf-ruler/wiki).

Bugs and feature requests can be filed with [Github Issues](https://github.com/cqframework/cqf-ruler/issues).

The implementers are active on the official FHIR [Zulip chat for CQL](https://chat.fhir.org/#narrow/stream/179220-cql).

Inquires for commercial support can be directed to [info@alphora.com](info@alphora.com).

## Related Projects

[HAPI FHIR](https://github.com/hapifhir) - Provides the FHIR API and server upon which the cqf-ruler is built.

[Clinical Quality Language](https://github.com/cqframework/clinical_quality_language) - Tooling in support of the CQL specification, including the CQL verifier/translator used in this project.

[CQL Evaluator](https://github.com/DBCG/cql-evaluator) - Provides the CQL execution environment used by the cqf-ruler.

[CQF Tooling](https://github.com/cqframework/cqf-tooling) - Provides several operations that the cqf-ruler exposes are services, such as $refresh-generated content.

[CQL Support for VS Code](https://marketplace.visualstudio.com/items?itemName=cqframework.cql) - CQL IDE plugin with syntax highlighting, linting, and local CQL evaluation.

## License

Copyright 2019+ Dynamic Content Group, LLC (dba Alphora)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
