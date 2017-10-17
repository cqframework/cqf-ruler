# cqf-ruler
The CQF Ruler is an implementation of FHIR's [Clinical Reasoning Module](http://hl7.org/fhir/clinicalreasoning-module.html) and serves as a knowledge artifact repository and clinical decision support service.

## Getting Started
You will need to have PostgreSQL installed on your system. If you do not have PostgreSQL, you can download it [here](https://www.postgresql.org/download/). Once PostgreSQL is installed, create a database named "fhir". You can then either create a role for the fhir database that matches the user/pass in the [server configuration](https://github.com/DBCG/cqf-ruler/blob/master/src/main/java/org/opencds/cqf/config/FhirServerConfigDstu3.java#L45-L46) or change the user/pass to whatever you like.

Creating a Role to match the config:
```
$ psql fhir
fhir=# CREATE ROLE hapi WITH LOGIN PASSWORD ‘hapi'
```

## Usage 
 - `$ mvn install`
 - `$ mvn -Djetty.http.port=XXXX jetty:run`
 
Visit the [wiki](https://github.com/DBCG/cqf-ruler/wiki) for more documentation.
