library FHIRHelpers version '1.0.2'

using FHIR version '1.0.2'

define function ToInterval(period FHIR.Period):
    Interval[period."start".value, period."end".value]

define function ToQuantity(quantity FHIR.Quantity):
    System.Quantity { value: quantity.value.value, unit: quantity.unit.value }

define function ToQuantity(quantity FHIR.SimpleQuantity):
    System.Quantity { value: quantity.value.value, unit: quantity.unit.value }

define function ToInterval(range FHIR.Range):
    Interval[ToQuantity(range.low), ToQuantity(range.high)]

define function ToCode(coding FHIR.Coding):
    System.Code {
      code: coding.code.value,
      system: coding.system.value,
      version: coding.version.value,
      display: coding.display.value
    }

define function ToConcept(concept FHIR.CodeableConcept):
    System.Concept {
        codes: concept.coding C return ToCode(C),
        display: concept.text.value
    }

define function ToString(value FHIR.AddressType): value.value
define function ToString(value FHIR.string): value.value
define function ToString(value FHIR.id): value.value
define function ToInteger(value FHIR.integer): value.value
define function ToString(value FHIR.code): value.value
define function ToBoolean(value FHIR.boolean): value.value
define function ToString(value FHIR.uri): value.value
define function ToTime(value FHIR.time): value.value
define function ToInteger(value FHIR.unsignedInt): value.value
define function ToString(value FHIR.oid): value.value
