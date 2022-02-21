library SimpleR4Library

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

context Patient

define simpleBooleanExpression: true

define observationRetrieve: [Observation]

define observationHasCode: not IsNull(([Observation]).code)

define "Initial Population": observationHasCode

define "Denominator": "Initial Population"

define "Numerator": "Denominator"
