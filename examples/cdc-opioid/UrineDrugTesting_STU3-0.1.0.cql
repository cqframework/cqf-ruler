library UrineDrugTesting_STU3 version '0.1.0'

using FHIR version '3.0.0'

codesystem "LOINC": 'http://loinc.org'
codesystem "SNOMED": 'http://snomed.info/sct'

valueset "Positive Qualifiers": 'TODO'

code "Oxycodone in urine": '10998-3' from "LOINC"
code "Drugs in urine": '12286-1' from "LOINC"
code "Opioids in urine": '19295-5' from "LOINC"
code "Positive": '10828004' from "SNOMED"

parameter Orders List<MedicationRequest>

context Patient

// TODO - make this more robust - maybe put all chronic pain codes in a valueset?
define "Has Prescription for Chronic Pain":
    exists (
        Orders O
            where O.reasonCode is not null
            and O.reasonCode.coding[0].code.value = '82423001'
    )

define "Drug Screenings":
    "Illicit Drug Screening in Past 12 Months" union "Opioid Drug Screening in Past 12 Months"

define "Illicit Drug Screening in Past 12 Months":
    [Observation: "Drugs in urine"] IllicitDrugScreening
        where IllicitDrugScreening.effective.value same day or after Today() - 1 year

define "Opioid Drug Screening in Past 12 Months":
    ([Observation: "Opioids in urine"] union [Observation: "Oxycodone in urine"]) OpioidDrugScreening
        where OpioidDrugScreening.effective.value same day or after Today() - 1 year

define "Drug Screening Component Codes":
    "Drug Screenings" Screenings
        where
            Screenings.component is not null
        return
            CodingToCode(Screenings.component.code.coding)

define "No Screenings in Past Year":
    not exists (
        "Drug Screenings"
    )

define "Has Positive Screening":
    exists (
        "Drug Screenings" Screenings
            where
                (Screenings.component is not null
                and Screenings.component.interpretation is not null
                and Screenings.component.interpretation.coding[0].code.value = 'POS')
                // or
                // (Screenings.component is not null
                // and CodingToCode(Screenings.component.code.coding) in "Positive Qualifiers")
    )

define "Prescribed Medications":
    Orders Rx
        return CodingToCode(Rx.medication.coding)

define "Found Unprescribed Opioid in Screening":
    exists (
        "Opioid Drug Screening in Past 12 Months" OpioidScreening
                    where
                        OpioidScreening.component is not null
                        and
                        not (CodingToCode(OpioidScreening.component.code.coding) in "Prescribed Medications")
    )

define "Did Not Find Prescribed Opioid":
    exists (
        "Prescribed Medications" Rx
                    where
                        not (Rx in "Drug Screening Component Codes")
    )

define "Found Illicit Drugs in Screening":
    exists ("Illicit Drug Screening in Past 12 Months")

define "Illicit Drugs":
    "Illicit Drug Screening in Past 12 Months" IllicitDrugs
        where IllicitDrugs.component is not null
        return
            CodingToCode(IllicitDrugs.component.code.coding).system + ' '
                + CodingToCode(IllicitDrugs.component.code.coding).code + ' '
                + CodingToCode(IllicitDrugs.component.code.coding).display

define "Unprescribed Opioids":
    "Opioid Drug Screening in Past 12 Months" OpioidScreening
        where
            OpioidScreening.component is not null
            and
                not (CodingToCode(OpioidScreening.component.code.coding) in "Prescribed Medications")
            return
                CodingToCode(OpioidScreening.component.code.coding).system + ' '
                    + CodingToCode(OpioidScreening.component.code.coding).code + ' '
                    + CodingToCode(OpioidScreening.component.code.coding).display

define "Missing Opioids":
    "Prescribed Medications" Rx
        where
            not (Rx in "Drug Screening Component Codes")
        return
            Rx.system + ' ' + Rx.code + ' ' + Rx.display

define "Suggest Drug Screening":
    'A urine drug screen could not be found for the patient in the past 12 months. Patient should have a urine drug test performed.'

define "Get Indicator":
    'warning'

define "Inconsistent Illicit Drugs":
    'Found drugs other than what is prescribed in urine drug screen: ' + "Illicit Drugs"

define "Inconsistent Unprescribed Opioid Drugs":
    'Found opioids other than what is prescribed in urine drug screen: ' + "Unprescribed Opioids"

define "Inconsistent Missing Opioid Drugs":
    'Did not find prescribed opioids in urine drug screen: ' + "Missing Opioids"

define function CodingToCode(coding FHIR.Coding):
	System.Code {
		code: coding.code.value,
		system: coding.system.value,
		version: coding.version.value,
		display: coding.display.value
	}