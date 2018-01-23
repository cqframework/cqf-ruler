library OpioidCDS_DSTU2 version '0.1.0'

using FHIR version '1.0.2'

include OMTKLogic version '0.1.0' called OMTKLogic

parameter UserID String
parameter Orders List<MedicationOrder>

context Patient

// IsForChronicPain
// TODO: Capture process decisions for long-term opioid use
define IsForChronicPain: true

// HasMetastaticCancer
// TODO: Capture process decisions for metastatic cancer
define HasMetastaticCancer: false

define function ToCode(coding FHIR.Coding):
    System.Code {
      code: coding.code.value,
      system: coding.system.value,
      version: coding.version.value,
      display: coding.display.value
    }

define function ToQuantity(quantity FHIR.Quantity):
    System.Quantity { value: quantity.value.value, unit: quantity.unit.value }

// Prescriptions: rxNormCode, isDraft, isPRN, prescription String, dose Quantity, dosesPerDay Decimal
define Prescriptions:
  Orders O
    let
      rxNormCode: ToCode(O.medicationCodeableConcept.coding[0]), // NOTE: Assuming medication is specified as a CodeableConcept with a single RxNorm code
      medicationName: OMTKLogic.GetMedicationName(rxNormCode),
      dosageInstruction: O.dosageInstruction[0], // NOTE: Assuming a single dosage instruction element
      repeat: dosageInstruction.timing.repeat,
      frequency: Coalesce(repeat.frequencyMax.value, repeat.frequency.value),
      period: System.Quantity { value: repeat.period.value, unit: repeat.periodUnits.value },
      doseDescription:
        ToString(ToQuantity(dosageInstruction.doseQuantity as SimpleQuantity)) // There should be a conversion from FHIR.SimpleQuantity to System.Quantity
      ,
      frequencyDescription:
        ToString(dosageInstruction.timing.repeat.frequency.value) +
          Coalesce(
            '-' + ToString(dosageInstruction.timing.repeat.frequencyMax.value),
            ''
          )
    return {
      rxNormCode: rxNormCode,
      isDraft: O.status.value = 'draft',
      isPRN: dosageInstruction.asNeededBoolean, // NOTE: Assuming asNeeded is expressed as a boolean
      prescription:
        if dosageInstruction.text is not null then
          medicationName + ' ' + dosageInstruction.text.value
        else
          medicationName + ' ' + doseDescription + ' q' + frequencyDescription + (if dosageInstruction.asNeededBoolean.value then ' PRN' else ''), // TODO: Shouldn't need the .value here on asNeededBoolean
      dose: ToQuantity(dosageInstruction.doseQuantity), // TODO: Shouldn't need the ToQuantity here...
      dosesPerDay: Coalesce(OMTKLogic.ToDaily(frequency, period), 1.0)
    }

// MME - Milligram Morphine Equivalents as a list of tuples:
// List<Tuple { rxNormCode: Code, isDraft Boolean, isPRN Boolean, prescription String, dailyDose String, conversionFactor Decimal, mme System.Quantity }>
// function CalculateMMEs(medications List<Tuple { rxNormCode Code, doseQuantity System.Quantity, dosesPerDay Decimal }>):
define MME:
  Prescriptions P
    let mme: SingletonFrom(OMTKLogic.CalculateMMEs({ { rxNormCode: P.rxNormCode, doseQuantity: P.dose, dosesPerDay: P.dosesPerDay } }))
    return {
      rxNormCode: P.rxNormCode,
      isDraft: P.isDraft,
      isPRN: P.isPRN,
      prescription: P.prescription,
      dailyDose: mme.dailyDoseDescription,
      conversionFactor: mme.conversionFactor,
      mme: mme.mme
    }
    sort by if isDraft then 0 else 1, rxNormCode.code

/*
define MME: {
  {
    Prescription: 'Oxycodone-acetaminophen 5-325mg (percocet) PO Tablet 2 tabs qid PRN pain',
    DailyDose: 'Oxycodone PO: 10mg * 4 = 40mg',
    ConversionFactor: 1.5,
    MME: 60 'mg/d'
  }
}
*/

// TODO: Capture process decisions for MME calculation
// TotalMME - Sum of all MME for currently and about-to-be prescribed opioid medications
define TotalMME: System.Quantity { value: Sum(MME M return M.mme.value), unit: 'mg/d' }

define IsMME50OrMore:
  if Orders is null then false else TotalMME >= 50 'mg/d'

/*
Argonaut profiles only support MedicationOrder and MedicationStatement
Epic's fhir documentation indicates the same... https://open.epic.com/Interface/FHIR
Cener's fhir documentation indicates the same... http://fhir.cerner.com/millenium/dstu2

Argonaut's Medication Code is bound to:
http://build.fhir.org/ig/argonautproject/data-query/ValueSet-medication-codes.html
The Argonaut MedicationOrder profile does not require any dosage information
The Argonaut MedicationStatement profile does not require any dosage information
*/

/*
MedicationOrder
  dosageInstruction
    text
    timing
      code
      repeat
        count
        frequency
        period
        when
    asNeeded
    dose
    rate
    maxDosePerPeriod
  dispenseRequest
    numberOfRepeatsAllowed
    quantity
    expectedSupplyDuration

*/

/*
MedicationDispense
  quantity
  daysSupply
  dosageInstruction
    text
    timing
      code
      repeat
        count
        frequency
        period
        when
    asNeeded
    dose
    rate
    maxDosePerPeriod

*/

/*
MedicationAdministration
  dosage
    quantity
    rate
*/

/*
MedicationStatement
  dosage
    text
    timing
      code
      repeat
        count
        frequency
        period
        when
    asNeeded
    quantity
    rate
    maxDosePerPeriod
*/
