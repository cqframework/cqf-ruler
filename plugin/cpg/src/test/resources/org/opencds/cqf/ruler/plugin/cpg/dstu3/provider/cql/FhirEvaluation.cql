library FhirEvaluation version '1.0.000'

using FHIR version '3.0.0'

context Patient

define "MedicationRequest": [MedicationRequest]

define "Condition": [Condition]
