library OpioidCDS_STU3 version '0.1.0'

using FHIR version '3.0.0'

include OMTKLogic version '0.1.0' called OMTKLogic

parameter UserID String
parameter Orders List<MedicationRequest>

context Patient

// NOTE: These functions have been moved here from the FHIRHelpers STU3 library to boost performance
define function ToCode(coding FHIR.Coding):
    System.Code {
      code: coding.code.value,
      system: coding.system.value,
      version: coding.version.value,
      display: coding.display.value
    }

define function ToQuantity(quantity FHIR.Quantity):
    System.Quantity { value: quantity.value.value, unit: quantity.unit.value }

// IsForChronicPain
// TODO: Capture process decisions for long-term opioid use
define IsForChronicPain: true

// HasMetastaticCancer
// TODO: Capture process decisions for metastatic cancer
define HasMetastaticCancer: false

// Prescriptions: rxNormCode, isDraft, isPRN, prescription String, dose Quantity, dosesPerDay Decimal
define Prescriptions:
  Orders O
    let
      // NOTE: Assuming medication is specified as a CodeableConcept with a single RxNorm code
      rxNormCode: ToCode(O.medication.coding[0]),
      medicationName: OMTKLogic.GetMedicationName(rxNormCode),
      // NOTE: Assuming a single dosage instruction element
      dosageInstruction: O.dosageInstruction[0],
      repeat: dosageInstruction.timing.repeat,
      frequency: Coalesce(repeat.frequencyMax.value, repeat.frequency.value),
      period: System.Quantity { value: repeat.period.value, unit: repeat.periodUnit.value },
      doseDescription:
        Coalesce(
          // There should be a conversion from FHIR.SimpleQuantity to System.Quantity
          if dosageInstruction.dose is FHIR.Range
            then ToString(ToQuantity(dosageInstruction.dose.low))
                          + '-' + ToString(ToQuantity(dosageInstruction.dose.high))
                          + dosageInstruction.dose.high.unit.value
            else ToString(ToQuantity(dosageInstruction.dose))
        ),
      frequencyDescription:
        ToString(dosageInstruction.timing.repeat.frequency.value) +
          Coalesce(
            '-' + ToString(dosageInstruction.timing.repeat.frequencyMax.value),
            ''
          )
    return {
      rxNormCode: rxNormCode,
      isDraft: O.status.value = 'draft',
      // NOTE: Assuming asNeeded is expressed as a boolean
      isPRN: dosageInstruction.asNeeded.value,
      prescription:
        if dosageInstruction.text is not null then
          medicationName + ' ' + dosageInstruction.text.value
        else
          // TODO: Shouldn't need the .value here on asNeededBoolean
          medicationName + ' ' + doseDescription + ' q' + frequencyDescription + (if dosageInstruction.asNeeded.value then ' PRN' else ''),
      // TODO: Shouldn't need the ToQuantity here...
      dose: if dosageInstruction.dose is FHIR.Range
                then ToQuantity(dosageInstruction.dose.high)
                else ToQuantity(dosageInstruction.dose),
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

// TODO: Capture process decisions for MME calculation
// TotalMME - Sum of all MME for currently and about-to-be prescribed opioid medications
define TotalMME: System.Quantity { value: Sum(MME M return M.mme.value), unit: 'mg/d' }

define IsMME50OrMore: TotalMME >= 50 'mg/d'

define getIndicator:
    if IsMME50OrMore
        then 'warning'
    else 'success'

define getSummary:
    if IsMME50OrMore
        then 'High risk for opioid overdose - '
                 + case when TotalMME.value >= 90
                     then 'taper now'
                     else 'consider tapering'
                   end
    else 'MME is within the recommended range.'

define getDetail:
    if IsMME50OrMore
        then 'Total morphine milligram equivalent (MME) is ' + ToString(TotalMME) + '. Taper to less than 50.'
    else 'Total morphine milligram equivalent (MME) is ' + ToString(TotalMME) + '. This falls within the accepted range.'

define Results:
  IsMME50OrMore MMEOver50
  return {
    mmeOver50: MMEOver50,
    title:
      if MMEOver50
      then 'High risk for opioid overdose - '
            + case when TotalMME.value >= 90
                then 'taper now'
                else 'consider tapering'
              end
      else 'MME is within the recommended range.',
      description:
        if MMEOver50
        then 'Total morphine milligram equivalent (MME) is ' + ToString(TotalMME) + '. Taper to less than 50.'
        else 'Total morphine milligram equivalent (MME) is ' + ToString(TotalMME) + '. This falls within the accepted range.'
  }

/*
USCore profiles only support MedicationRequest and MedicationStatement, same as Argonaut Data Query
See the DSTU2 Library for more information

USCore's Medication Code is bound to:
http://www.hl7.org/fhir/us/core/ValueSet-us-core-medication-codes.html
The USCore MedicationRequest and MedicationStatement profiles do not require any dosage or frequency information
*/
