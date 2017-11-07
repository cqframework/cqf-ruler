library RadiologyAppropriateness version '1.0'

using FHIR version '3.0.0'

valueset "Extremely Appropriate": 'extremely.appropriate'
valueset "Appropriate": 'appropriate'
valueset "Probably Appropriate": 'probably.appropriate'
valueset "Probably Uncertain Appropriate": 'uncertain.appropriate'
valueset "Uncertain": 'uncertain'
valueset "Uncertain Inappropriate": 'uncertain.inappropriate'
valueset "Probably Inappropriate": 'probably.inappropriate'
valueset "Inappropriate": 'inappropriate'
valueset "Extremely Inappropriate": 'extremely.inappropriate'

parameter Order ProcedureRequest

context Patient

/*
  This is meant to be a generic library for determining the appropriateness of an order.
  Users must populate the above valuesets according to their metrics.
*/
define "Get Extremely Appropriate Score": 9.0
define "Get Appropriate Score": 8.0
define "Get Probably Appropriate Score": 7.0
define "Get Uncertain Appropriate Score": 6.0
define "Get Uncertain Score": 5.0
define "Get Uncertain Inappropriate Score": 4.0
define "Get Probably Inappropriate Score": 3.0
define "Get Inappropriate Score": 2.0
define "Get Extremely Inappropriate Score": 1.0

define "Is Extremely Appropriate": CodingToCode(Order.code.coding[0]) in "Extremely Appropriate"
define "Is Appropriate": CodingToCode(Order.code.coding[0]) in "Appropriate"
define "Is Probably Appropriate": CodingToCode(Order.code.coding[0]) in "Probably Appropriate"
define "Is Probably Uncertain Appropriate": CodingToCode(Order.code.coding[0]) in "Probably Uncertain Appropriate"
define "Is Uncertain": CodingToCode(Order.code.coding[0]) in "Uncertain"
define "Is Uncertain Inappropriate": CodingToCode(Order.code.coding[0]) in "Uncertain Inappropriate"
define "Is Probably Inappropriate": CodingToCode(Order.code.coding[0]) in "Probably Inappropriate"
define "Is Inappropriate": CodingToCode(Order.code.coding[0]) in "Inappropriate"
define "Is Extremely Inappropriate": CodingToCode(Order.code.coding[0]) in "Extremely Inappropriate"

define function CodingToCode(coding FHIR.Coding):
	System.Code {
		code: coding.code.value,
		system: coding.system.value,
		version: coding.version.value,
		display: coding.display.value
	}
