/*
Breast Cancer Screening (BCS)
*/

library BCS_FHIR version '1.0.0'

using FHIR version '3.0.0'

valueset "Absence of Left Breast Value Set": '2.16.840.1.113883.3.464.1004.1329.1' // ICD10CM
valueset "Absence of Right Breast Value Set": '2.16.840.1.113883.3.464.1004.1330.2' // ICD10CM

valueset "Bilateral Mastectomy Value Set": '2.16.840.1.113883.3.464.1004.1042' // Grouping
// valueset "Bilateral Mastectomy Value Set": '2.16.840.1.113883.3.464.1004.1042.3' // ICD10PCS
// valueset "Bilateral Mastectomy Value Set": 'urn:oid:2.16.840.1.113883.3.464.1004.1042.4' // ICD9CMProc

valueset "Bilateral Modifier Value Set": '2.16.840.1.113883.3.464.1004.1043.5' // CPT-Mod
valueset "History of Bilateral Mastectomy Value Set": '2.16.840.1.113883.3.464.1004.1331.6' // ICD10CM
valueset "Left Modifier Value Set": '2.16.840.1.113883.3.464.1004.1148.7' // CPT-Mod

valueset "Mammography Value Set": '2.16.840.1.113883.3.464.1004.1168' // Grouping
// valueset "Mammography Value Set": '2.16.840.1.113883.3.464.1004.1168.8' // CPT
// valueset "Mammography Value Set": 'urn:oid:2.16.840.1.113883.3.464.1004.1168.9' // HCPCS
// valueset "Mammography Value Set": 'urn:oid:2.16.840.1.113883.3.464.1004.1168.10' // ICD9CMProc
// valueset "Mammography Value Set": 'urn:oid:2.16.840.1.113883.3.464.1004.1168.11' // RevCode

valueset "Right Modifier Value Set": '2.16.840.1.113883.3.464.1004.1230.12' // CPT-Mod

valueset "Unilateral Mastectomy Value Set": '2.16.840.1.113883.3.464.1004.1256' // Grouping
// valueset "Unilateral Mastectomy Value Set": '2.16.840.1.113883.3.464.1004.1256.13' // CPT
// valueset "Unilateral Mastectomy Value Set": 'urn:oid:2.16.840.1.113883.3.464.1004.1256.14' // ICD9CMProc

valueset "Unilateral Mastectomy Left Value Set": '2.16.840.1.113883.3.464.1004.1334.15' // ICD10PCS
valueset "Unilateral Mastectomy Right Value Set": '2.16.840.1.113883.3.464.1004.1335.16' // ICD10PCS

define "First Predecessor Year":
	Interval[Today() - 2 years, Today() - 1 years)

define "Second Predecessor Year":
	Interval[Today() - 3 years, Today() - 2 years)

define "Third Predecessor Quarter":
	Interval[Today() - 3 years - 3 months, Today() - 3 years)

define "Lookback Interval 27 More Months":
	Interval[Today() - 27 months, Today())

context Patient

define "Does Patient Qualify?":
	"Is Female"
		and "Is Age 52 to 74 at End"
		and if "Has Double Mastectomy" is null
		        then true
		    else not "Has Double Mastectomy"

define "Has Double Mastectomy":
    "Is Lacking Both Breasts"
        or ("Is Lacking Left Breast"
        and "Is Lacking Right Breast")

define "Is Female":
	Patient.gender.value = 'female'

define "Is Age 52 to 74 at End":
	AgeInYears() between 52 and 74

/*
Administrative Specification
*/

define "Needs Mammogram":
	if "Is Mammogram In Last 39 Months" is null
	    then true
	else not "Is Mammogram In Last 39 Months"

define "Is Mammogram In Last 39 Months":
	exists(
		[Procedure: "Mammography Value Set"] Proc
			where Proc.status.value = 'completed'
				and PeriodToIntervalOfDT(Proc.performed) included in "Lookback Interval 27 More Months"
	)

define "Is Lacking Both Breasts":
	"Is Bilateral Mastectomy"
		or "Is History Of Bilateral Mastectomy"
		or "Is Unilateral Mastectomy With Bilateral Modifier"
		or "Is Unilateral Mastectomy Twice Spread Two Weeks"

define "Is Lacking Left Breast":
	"Is Unilateral Mastectomy With Left Modifier"
		or "Is Unilateral Mastectomy Left"
		or "Is Absence Of Left Breast"

define "Is Lacking Right Breast":
	"Is Unilateral Mastectomy With Right Modifier"
		or "Is Unilateral Mastectomy Right"
		or "Is Absence Of Right Breast"

define "Is Bilateral Mastectomy":
	exists(
		[Procedure: "Bilateral Mastectomy Value Set"] Proc
			where Proc.status.value = 'completed'
				and end of PeriodToIntervalOfDT(Proc.performed) same day or before Today()
	)

define "Is History Of Bilateral Mastectomy":
	exists(
		[Condition: "History of Bilateral Mastectomy Value Set"] Cond
			where Cond.verificationStatus.value = 'confirmed'
				and Cond.assertedDate.value same day or before Today()
	)

define "Is Unilateral Mastectomy With Bilateral Modifier":
	exists(
		[Claim] Claim
			where Claim.status.value = 'active'
				and exists(
				Claim.procedure BbElem
					with (
						[Procedure: "Unilateral Mastectomy Value Set"] Proc
							where Proc.status.value = 'completed'
								and end of PeriodToIntervalOfDT(Proc.performed) same day or before Today()
					) Proc1
						such that
							(if BbElem.procedure is FHIR.Reference
								then (BbElem.procedure as FHIR.Reference).identifier ~ Proc1.identifier
								else false)
					with (
						[Procedure: "Bilateral Modifier Value Set"] Proc
							where Proc.status.value = 'completed'
								and end of PeriodToIntervalOfDT(Proc.performed) same day or before Today()
					) Proc2
						such that
							(if BbElem.procedure is FHIR.Reference
								then (BbElem.procedure as FHIR.Reference).identifier ~ Proc2.identifier
								else false)
			)
	)

define "Is Unilateral Mastectomy Twice Spread Two Weeks":
	exists(
		(
			[Procedure: "Unilateral Mastectomy Value Set"] Proc
				where Proc.status.value = 'completed'
				return PeriodToIntervalOfDT(Proc.performed)
		) WhenUM1
			with (
				[Procedure: "Unilateral Mastectomy Value Set"] Proc
					where Proc.status.value = 'completed'
					return PeriodToIntervalOfDT(Proc.performed)
			) WhenUM2
				such that (((difference in days between start of WhenUM1 and start of WhenUM2) >= 14)
					and end of WhenUM1 same day or before Today()
					and end of WhenUM2 same day or before Today())
	)

define "Is Unilateral Mastectomy With Left Modifier":
	exists(
		[Claim] Claim
			where Claim.status.value = 'active'
				and exists(
				Claim.procedure BbElem
					with (
						[Procedure: "Unilateral Mastectomy Value Set"] Proc
							where Proc.status.value = 'completed'
								and end of PeriodToIntervalOfDT(Proc.performed) same day or before Today()
					) Proc1
						such that
							(if BbElem.procedure is FHIR.Reference
								then (BbElem.procedure as FHIR.Reference).identifier ~ Proc1.identifier
								else false)
					with (
						[Procedure: "Left Modifier Value Set"] Proc
							where Proc.status.value = 'completed'
								and end of PeriodToIntervalOfDT(Proc.performed) same day or before Today()
					) Proc2
						such that
							(if BbElem.procedure is FHIR.Reference
								then (BbElem.procedure as FHIR.Reference).identifier ~ Proc2.identifier
								else false)
			)
	)

define "Is Unilateral Mastectomy Left":
	exists(
		[Procedure: "Unilateral Mastectomy Left Value Set"] Proc
			where Proc.status.value = 'completed'
				and end of PeriodToIntervalOfDT(Proc.performed) same day or before Today()
	)

define "Is Absence Of Left Breast":
	exists(
		[Observation: "Absence of Left Breast Value Set"] Obs
			where Obs.status.value in { 'final', 'amended' }
				and Obs.effective.value same day or before Today()
	)

define "Is Unilateral Mastectomy With Right Modifier":
	exists(
		[Claim] Claim
			where Claim.status.value = 'active'
				and exists(
				Claim.procedure BbElem
					with (
						[Procedure: "Unilateral Mastectomy Value Set"] Proc
							where Proc.status.value = 'completed'
								and end of PeriodToIntervalOfDT(Proc.performed) same day or before Today()
					) Proc1
						such that
							(if BbElem.procedure is FHIR.Reference
								then (BbElem.procedure as FHIR.Reference).identifier ~ Proc1.identifier
								else false)
					with (
						[Procedure: "Right Modifier Value Set"] Proc
							where Proc.status.value = 'completed'
								and end of PeriodToIntervalOfDT(Proc.performed) same day or before Today()
					) Proc2
						such that
							(if BbElem.procedure is FHIR.Reference
								then (BbElem.procedure as FHIR.Reference).identifier ~ Proc2.identifier
								else false)
			)
	)

define "Is Unilateral Mastectomy Right":
	exists(
		[Procedure: "Unilateral Mastectomy Right Value Set"] Proc
			where Proc.status.value = 'completed'
				and end of PeriodToIntervalOfDT(Proc.performed) same day or before Today()
	)

define "Is Absence Of Right Breast":
	exists(
		[Observation: "Absence of Right Breast Value Set"] Obs
			where Obs.status.value in { 'final', 'amended' }
				and end of PeriodToIntervalOfDT(Obs.effective) same day or before Today()
	)


/*
Utility Functions
*/

define function PeriodToIntervalOfDT(value FHIR.Period):
	Interval[value."start".value, value."end".value]

define function CodingToCode(coding FHIR.Coding):
	System.Code {
		code: coding.code.value,
		system: coding.system.value,
		version: coding.version.value,
		display: coding.display.value
	}
	// From FHIRHelpers
