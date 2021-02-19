# cqf-ruler

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.opencds.cqf/cqf-ruler-r4/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.opencds.cqf/cqf-ruler-r4) [![Build Status](https://www.travis-ci.com/DBCG/cqf-ruler.svg?branch=master)](https://www.travis-ci.com/DBCG/cqf-ruler) [![docker image](https://img.shields.io/docker/v/contentgroup/cqf-ruler/latest?style=flat&color=brightgreen&label=docker%20image)](https://hub.docker.com/r/contentgroup/cqf-ruler/tags) [![project chat](https://img.shields.io/badge/zulip-join_chat-brightgreen.svg)](https://chat.fhir.org/#narrow/stream/179220-cql)

The CQF Ruler is a set of plugins for the [HAPI FHIR Server](https://github.com/hapifhir/hapi-fhir-jpaserver-starter) that provides an implementation of FHIR's [Clinical Reasoning Module](
http://hl7.org/fhir/clinicalreasoning-module.html) and serves as a
knowledge artifact repository and a [cds-hooks](https://cds-hooks.org/) compatible clinical decision support service. It does this via integrating a number of other CQL-related projects, which are listed below.

## Usage

### Docker

The easiest way to get started with the cqf-ruler is to pull and run the docker image

```bash
docker pull contentgroup/cqf-ruler
docker run -p 8080:8080 contentgroup/cqf-ruler
```

This will make the cqf-ruler available on `http://localhost:8080/cqf-ruler-r4`

Other options for deployment are listed on the [wiki](https://github.com/DBCG/cqf-ruler/wiki) for more documentation.

## Development

### Dependencies

#### Java

Go to [http://www.oracle.com/technetwork/java/javase/downloads/](
http://www.oracle.com/technetwork/java/javase/downloads/) and download the
latest (version 11 or higher) JDK for your platform, and install it.

#### Apache Maven 3.5.3

Go to [https://maven.apache.org](https://maven.apache.org), visit the main
"Download" page, and under "Files" download the 3.5.3 binary.  Then unpack that archive file and follow the installation
instructions in its README.txt.  The end result of this should be that the
binary "mvn" is now in your path.

### Build

`mvn package`

Builds the project war file (cqf-ruler-dstu3.war and cqf-ruler-r4.war in the projects' target directory)

Visit the [wiki](https://github.com/DBCG/cqf-ruler/wiki) for more documentation.

### Run

To run the cqf-ruler directory from this project use:

#### DSTU3

`mvn jetty:run -am --projects cqf-ruler-dstu3`

The cqf-ruler will be available at `http://localhost:8080/cqf-ruler-dstu3/`

#### R4

`mvn jetty:run -am --projects cqf-ruler-r4`

The cqf-ruler will be available at `http://localhost:8080/cqf-ruler-r4/`

## Commit Policy

All new development takes place on `<feature>` branches off `master`. Once feature development on the branch is complete, the feature branch is submitted to `master` as a PR. The PR is reviewed by maintainers and regression testing by the CI build occurs.

Changes to the `master` branch must be done through an approved PR. Delete branches after merging to keep the repository clean.

Merges to `master` trigger a deployment to the Maven Snapshots repositories. Once ready for a release, the `master` branch is updated with the correct version number and is tagged. Tags trigger a full release to Maven Central and a corresponding release to Github. Releases SHALL NOT have a SNAPSHOT version, nor any SNAPSHOT dependencies.

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

[CQL Support for Atom](https://atom.io/packages/language-cql) - Open source CQL IDE with syntax highlighting, linting, and local CQL evaluation.

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
