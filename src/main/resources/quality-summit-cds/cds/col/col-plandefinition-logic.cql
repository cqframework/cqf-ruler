library ColPlanDefinitionLogic version '1.0'

using FHIR version '3.0.0'

valueset "FOBT Value Set (CPT)": '2.16.840.1.113883.3.464.1004.1093.41' // CPT
valueset "FOBT Value Set (HCPCS)": '2.16.840.1.113883.3.464.1004.1093.42' // HCPCS
valueset "FOBT Value Set (ICD9)": '2.16.840.1.113883.3.464.1004.1093.43' // ICD9CMProc

valueset "Flexible Sigmoidoscopy Value Set (CPT)": '2.16.840.1.113883.3.464.1004.1102.38' // CPT
valueset "Flexible Sigmoidoscopy Value Set (HCPCS)": '2.16.840.1.113883.3.464.1004.1102.39' // HCPCS
valueset "Flexible Sigmoidoscopy Value Set (ICD9)": '2.16.840.1.113883.3.464.1004.1102.40' // ICD9CMProc

valueset "FIT-DNA Value Set (CPT)": '2.16.840.1.113883.3.464.1004.1420.35' // CPT
valueset "FIT-DNA Value Set (HCPCS)": '2.16.840.1.113883.3.464.1004.1420.36' // HCPCS
valueset "FIT-DNA Value Set (LOINC)": '2.16.840.1.113883.3.464.1004.1420.37' // LOINC

valueset "Colonoscopy Value Set (CPT)": '2.16.840.1.113883.3.464.1004.1064.29' // CPT
valueset "Colonoscopy Value Set (HCPCS)": '2.16.840.1.113883.3.464.1004.1064.30' // HCPCS
valueset "Colonoscopy Value Set (ICD9)": '2.16.840.1.113883.3.464.1004.1064.31' // ICD9CMProc

valueset "CT Colonography Value Set": '2.16.840.1.113883.3.464.1004.1421.34' // CPT

valueset "Colorectal Cancer Value Set (ICD10)": '2.16.840.1.113883.3.464.1004.1065.32' // ICD10CM
valueset "Colorectal Cancer Value Set (ICD9)": '2.16.840.1.113883.3.464.1004.1065.33' // ICD9CM

valueset "Total Colectomy Value Set (CPT)": '2.16.840.1.113883.3.464.1004.1250.44' // CPT
valueset "Total Colectomy Value Set (ICD10)": '2.16.840.1.113883.3.464.1004.1250.45' // ICD10PCS
valueset "Total Colectomy Value Set (ICD9)": '2.16.840.1.113883.3.464.1004.1250.46' // ICD9CMProc

context Patient

define "Lookback Interval One Year":
	Interval[Today() - 1 years, Today())

define "Lookback Interval Three Years":
	Interval[Today() - 3 years, Today())

define "Lookback Interval Five Years":
	Interval[Today() - 5 years, Today())

define "Lookback Interval Ten Years":
    Interval[Today() - 10 years, Today())

define "Does Patient Qualify?":
    "Is in Age Range"
        and not "Has Total Colectomy"
        and not "Has Colorectal Cancer"

define "Is in Age Range":
	AgeInYears() between 51 and 75

define "FOBT Observations":
	[Observation: "FOBT Value Set (CPT)"]
		union [Observation: "FOBT Value Set (HCPCS)"]
		union [Observation: "FOBT Value Set (ICD9)"]

define "Needs Fecal Occult Blood Test":
	not exists(
		"FOBT Observations" Obs
			where Obs.status.value in { 'final', 'amended' }
				and Obs.effective.value in "Lookback Interval One Year"
	)

define "Flexible Sigmoidoscopy Reports":
	[DiagnosticReport: "Flexible Sigmoidoscopy Value Set (CPT)"]
		union [DiagnosticReport: "Flexible Sigmoidoscopy Value Set (HCPCS)"]
		union [DiagnosticReport: "Flexible Sigmoidoscopy Value Set (ICD9)"]

define "Needs Flexible Sigmoidoscopy":
	not exists(
		"Flexible Sigmoidoscopy Reports" DiagRep
			where DiagRep.status.value in { 'preliminary', 'final', 'amended', 'corrected', 'appended' }
				and PeriodToIntervalOfDT(DiagRep.effective) included in "Lookback Interval Five Years"
	)

define "Colonoscopy Reports":
	[DiagnosticReport: "Colonoscopy Value Set (CPT)"]
		union [DiagnosticReport: "Colonoscopy Value Set (HCPCS)"]
		union [DiagnosticReport: "Colonoscopy Value Set (ICD9)"]

define "Needs Colonoscopy":
	not exists(
		"Colonoscopy Reports" DiagRep
			where DiagRep.status.value in { 'preliminary', 'final', 'amended', 'corrected', 'appended' }
				and PeriodToIntervalOfDT(DiagRep.effective) included in "Lookback Interval Ten Years"
	)

define "Needs CT Colonography":
	not exists(
		[DiagnosticReport: "CT Colonography Value Set"] DiagRep
			where DiagRep.status.value in { 'preliminary', 'final', 'amended', 'corrected', 'appended' }
				and PeriodToIntervalOfDT(DiagRep.effective) included in "Lookback Interval Five Years"
	)

define "FIT-DNA Observations":
	[Observation: "FIT-DNA Value Set (CPT)"]
		union [Observation: "FIT-DNA Value Set (HCPCS)"]
		union [Observation: "FIT-DNA Value Set (LOINC)"]

define "Needs FIT-DNA Test":
	not exists(
		"FIT-DNA Observations" Obs
			where Obs.status.value in { 'final', 'amended' }
				and Obs.effective.value in "Lookback Interval Three Years"
	)

define "Colorectal Cancer Conditions":
	[Condition: "Colorectal Cancer Value Set (ICD10)"]
		union [Condition: "Colorectal Cancer Value Set (ICD9)"]

define "Has Colorectal Cancer":
	exists(
		"Colorectal Cancer Conditions" Cond
			where Cond.verificationStatus.value = 'confirmed'
				and Cond.assertedDate.value before day of end of "Lookback Interval One Year"
	)

define "Total Colectomy Procedures":
	[Procedure: "Total Colectomy Value Set (CPT)"]
		union [Procedure: "Total Colectomy Value Set (ICD10)"]
		union [Procedure: "Total Colectomy Value Set (ICD9)"]

define "Has Total Colectomy":
	exists(
		"Total Colectomy Procedures" Proc
			where Proc.status.value = 'completed'
				and end of PeriodToIntervalOfDT(Proc.performed) before day of end of "Lookback Interval One Year"
	)

// Conversion functions
define function PeriodToIntervalOfDT(value FHIR.Period):
	Interval[value."start".value, value."end".value]