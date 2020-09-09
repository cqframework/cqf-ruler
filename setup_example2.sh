#!/bin/sh

# Post Bundle
curl -s -X POST -H 'Content-Type: application/fhir+json' -d @example_plandef_bundle.json http://localhost:8080/cqf-ruler-r4/fhir
curl -s -X POST -H 'Content-Type: application/fhir+json' -d @example_nested_plandef.json http://localhost:8080/cqf-ruler-r4/fhir