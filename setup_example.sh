#!/bin/sh

# Post Patient resource
curl -s 'https://www.hl7.org/fhir/us/core/Patient-example.json' | curl -v -s -X PUT -H 'Content-Type: application/fhir+json' 'http://localhost:8080/cqf-ruler-r4/fhir/Patient/example' -d@-

# Post PlanDefinition Bundle
curl -s -X POST -H 'Content-Type: application/fhir+json' -d @example_plandef_bundle.json http://localhost:8080/cqf-ruler-r4/fhir
