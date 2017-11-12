library Appropriateness version '1.0'

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
define "Is Extremely Appropriate": CodingToCode(Order.code.coding[0]) in "Extremely Appropriate"
define "Is Appropriate": CodingToCode(Order.code.coding[0]) in "Appropriate"
define "Is Probably Appropriate": CodingToCode(Order.code.coding[0]) in "Probably Appropriate"
define "Is Probably Uncertain Appropriate": CodingToCode(Order.code.coding[0]) in "Probably Uncertain Appropriate"
define "Is Uncertain": CodingToCode(Order.code.coding[0]) in "Uncertain"
define "Is Uncertain Inappropriate": CodingToCode(Order.code.coding[0]) in "Uncertain Inappropriate"
define "Is Probably Inappropriate": CodingToCode(Order.code.coding[0]) in "Probably Inappropriate"
define "Is Inappropriate": CodingToCode(Order.code.coding[0]) in "Inappropriate"
define "Is Extremely Inappropriate": CodingToCode(Order.code.coding[0]) in "Extremely Inappropriate"

define "Is Appropriate Category":
  "Is Extremely Appropriate"
    or "Is Appropriate"
    or "Is Probably Appropriate"

define "Is Uncertain Category":
  "Is Probably Uncertain Appropriate"
    or "Is Uncertain"
    or "Is Uncertain Inappropriate"

define "Is Inappropriate Category":
  "Is Probably Inappropriate"
    or "Is Inappropriate"
    or "Is Extremely Inappropriate"

define "Get Appropriateness Score":
  if "Is Extremely Appropriate" then 9.0
  else if "Is Appropriate" then 8.0
  else if "Is Probably Appropriate" then 7.0
  else if "Is Probably Uncertain Appropriate" then 6.0
  else if "Is Uncertain" then 5.0
  else if "Is Uncertain Inappropriate" then 4.0
  else if "Is Probably Inappropriate" then 3.0
  else if "Is Inappropriate" then 2.0
  else 1.0

// TODO - add other properties
define "Get ID": Order.id.value
define "Get Status": Order.status.value
define "Get Intent": Order.intent.value
define "Get Priority": Order.priority.value

define function CodingToCode(coding FHIR.Coding):
	System.Code {
		code: coding.code.value,
		system: coding.system.value,
		version: coding.version.value,
		display: coding.display.value
	}
