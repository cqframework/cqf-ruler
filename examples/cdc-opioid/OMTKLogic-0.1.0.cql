library OMTKLogic version '0.1.0'

using OMTK version '0.1.0'

codesystem RxNorm: 'http://www.nlm.nih.gov/research/umls/rxnorm'

//define MED_DOSE_FORMS: [MED_DOSE_FORM]

// Given an RxNorm Code:
// NON_SURGICAL_OPIOID_TO_INCLUDE.DRUG_RXCUI
// MED_DRUG.DRUG_RXCUI
// MED_SCDC_FOR_DRUG.DRUG_RXCUI -> SCDC_RXCUI
// MED_SCDC.SCDC_RXCUI (STRENGTH, STRENGTH_VALUE, STRENGTH_UNIT)
// MED_INGREDIENT_FOR_SCDC.SCDC_RXCUI -> INGREDIENT_RXCUI
// MED_INGREDIENT.INGREDIENT_RXCUI
// MED_INGREDIENT_TYPE.INGREDIENT_RXCUI (INGREDIENT_TYPE = 'NonSurgicalOpioid')

/*
 SQL ->
select D.DRUG_RXCUI, D.DRUG_NAME, DF.DOSE_FORM_NAME, SCDCD.SCDC_RXCUI, SCDC.SCDC_NAME, SCDC.STRENGTH, SCDC.STRENGTH_VALUE, SCDC.STRENGTH_UNIT, I.INGREDIENT_RXCUI, I.INGREDIENT_NAME
  from MED_DRUG D
    join NON_SURGICAL_OPIOID_TO_INCLUDE NSO on D.DRUG_RXCUI = NSO.DRUG_RXCUI
    join MED_SCDC_FOR_DRUG SCDCD on D.DRUG_RXCUI = SCDCD.DRUG_RXCUI
    join MED_SCDC SCDC on SCDCD.SCDC_RXCUI = SCDC.SCDC_RXCUI
    join MED_INGREDIENT_FOR_SCDC SCDCI on SCDC.SCDC_RXCUI = SCDCI.SCDC_RXCUI
    join MED_INGREDIENT I on SCDCI.INGREDIENT_RXCUI = I.INGREDIENT_RXCUI
    join MED_INGREDIENT_TYPE IT on I.INGREDIENT_RXCUI = IT.INGREDIENT_RXCUI
	left join MED_DRUG_DOSE_FORM DDF on D.DRUG_RXCUI = DDF.DRUG_RXCUI
	left join MED_DOSE_FORM DF on DDF.DOSE_FORM_RXCUI = DF.DOSE_FORM_RXCUI
	--Most of the drugs have multiple dose form groups...
	--left join MED_DRUG_DOSE_FORM_GROUP DDFG on D.DRUG_RXCUI = DDFG.DRUG_RXCUI
	--left join MED_DOSE_FORM_GROUP DFG on DDFG.DOSE_FORM_GROUP_RXCUI = DFG.DOSE_FORM_GROUP_RXCUI
  where D.DRUG_RXCUI = @RxNormCode // 197696
    and IT.INGREDIENT_TYPE = 'NonSurgicalOpioid'
*/

/*
  CQL, assuming translation to SQL:

  from
    [MED_DRUG: rxNormCode] D,
    [NON_SURGICAL_OPIOID_TO_INCLUDE] NSO,
    [MED_SCDC_FOR_DRUG] SCDCD,
    [MED_SCDC] SCDC,
    [MED_INGREDIENT_FOR_SCDC] SCDCI,
    [MED_INGREDIENT] I,
    [MED_INGREDIENT_TYPE] IT,
    [MED_DRUG_DOSE_FORM] DDF,
    [MED_DOSE_FORM] DF
  where D.DRUG_RXCUI = NSO.DRUG_RXCUI
    and D.DRUG_RXCUI = SCDCD.DRUG_RXCUI
    and SCDCD.SCDC_RXCUI = SCDC.SCDC_RXCUI
    and SCDC.SCDC_RXCUI = SCDCI.SCDC_RXCUI
    and SDCDI.INGREDIENT_RXCUI = I.INGREDIENT_RXCUI
    and I.INGREDIENT_RXCUI = IT.INGREDIENT_RXCUI
    and D.DRUG_RXCUI = DDF.DRUG_RXCUI
    and DDF.DOSE_FORM_RXCUI = DF.DOSE_FORM_RXCUI
    and IT.INGREDIENT_TYPE = 'NonSurgicalOpioid'
*/

/*
An engine with a naive implementation for multi-source queries would perform
pretty horribly here, so rewrite it using "syntactic optimization" :)
*/

/*
  Normalizes the input units to UCUM units
  
  Note guidance for UCUM presentation of medication units from SNOMED here:
  https://www.google.com/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0ahUKEwjU3vLpicPTAhWFMGMKHRpOBUAQFggiMAA&url=https%3A%2F%2Fconfluence.ihtsdotools.org%2Fdownload%2Fattachments%2F17859188%2FExpressing%2520Units%2520of%2520Measure%2520for%2520Medicinal%2520Products.doc%3Fapi%3Dv2&usg=AFQjCNE5sboicqvJDUyXJ2im8VzBpgHE8A
  
  The values listed here are the only ones currently present in the OMTK data
  
  Based on the HL7 UCUM subset here:
  http://download.hl7.de/documents/ucum/ucumdata.html
*/
define function ToUCUM(unit String):
  case unit
    when 'MG' then 'mg'
    when 'MG/ACTUAT' then 'mg/{actuat}'
    when 'MG/HR' then 'mg/h'
    when 'MG/ML' then 'mg/mL'
    else 'unknown{' + unit + '}' // Should probably be an error
  end

/*
  Calculates daily frequency given frequency within a period
*/
define function ToDaily(frequency Integer, period System.Quantity):
  case period.unit
    when 'h' then frequency * (24.0 / period.value)
    when 'min' then frequency * (24.0 / period.value) * 60
    when 's' then frequency * (24.0 / period.value) * 60 * 60
    when 'd' then frequency * (24.0 / period.value) / 24
    when 'wk' then frequency * (24.0 / period.value) / (24 * 7)
    when 'mo' then frequency * (24.0 / period.value) / (24 * 30) // assuming 30 days in month
    when 'a' then frequency * (24.0 / period.value) / (24 * 365) // assuming 365 days in year
    else null
  end

/*
  Returns true if the given dose form is a patch (transdermal system)
*/
define function IsPatch(doseFormCode Code):
  ToInteger(doseFormCode.code) = 316987

/*
  Returns the conversion factor for the given ingredient

Opioid (strength in mg except where noted)	MME Conversion Factor*
Buprenorphine, transdermal patch (MCG/HR)	12.6
Buprenorphine, tablet or film	30
Buprenorphine, film (MCG)	0.03
Butorphanol	7
Codeine	0.15
Dihydrocodeine	0.25
Fentanyl, buccal/SL tabet or lozenge/troche (MCG)	0.13
Fentanyl, film or oral spray (MCG)	0.18
Fentanyl, nasal spray (MCG)	0.16
Fentanyl, transdermal patch (MCG/HR)	7.2
Hydrocodone	1
Hydromorphone	4
Levomethadyl acetate	8
Levorphanol tartrate	11
Meperidine 	0.1
Methadone	3
  1-20 mg/d 4
  21-40 mg/d 8
  41-60 mg/d 10
  61-80 mg/d 12
Morphine	1
Opium	1 // NOTE: Not present as an ingredient in the RxNorm data
Oxycodone	1.5
Oxymorphone	3
Pentazocine	0.37
Tapentadol	0.4
Tramadol	0.1

*/
define function GetConversionFactor(ingredientCode Code, dailyDose Quantity, doseFormCode Code):
  case ToInteger(ingredientCode.code)
    when 161 then 0 //	Acetaminophen
    when 1191 then 0 //	Aspirin
    when 1223 then 0 //	Atropine
    when 1767 then 0 //	Brompheniramine
    when 1819 then ( //	Buprenorphine
      case 
        when ToInteger(doseFormCode.code) = 316987 then 12.6 // Transdermal system
        else 30 // Tablet or Film (or Film in MCG)
      end
    ) 
    when 1841 then 7 //	Butorphanol
    when 1886 then 0 //	Caffeine
    when 2101 then 0 //	Carisoprodol
    when 2354 then 0 //	chlorcyclizine
    when 2400 then 0 //	Chlorpheniramine
    when 2670 then 0.15 //	Codeine
    when 3423 then 4 //	Hydromorphone
    when 3498 then 0 //	Diphenhydramine
    when 4337 then ( //	Fentanyl
      case
        when ToInteger(doseFormCode.code) in { 970789, 317007, 316992 } then 0.13 // Buccal Tablet, Sublingual Tablet, Oral Lozenge
        when ToInteger(doseFormCode.code) = 346163 then 0.18 // Buccal Film
        when ToInteger(doseFormCode.code) in { 126542, 346163 } then 0.16 // Nasal Spray, Mucosal Spray
        when IsPatch(doseFormCode) then 7.2 // Transdermal system
        else 1000 // Really ought to be an error because it represents a previously unencountered dose form....
      end
    )
    when 5032 then 0 //	Guaifenesin
    when 5489 then 1 //	Hydrocodone
    when 5640 then 0 //	Ibuprofen
    when 6102 then 0 //	Kaolin
    when 6378 then 11 //	Levorphanol (NOTE: Given as Levorphanol tartrate in the CDC conversion table)
    when 6754 then 0.1 //	Meperidine
    when 6813 then ( //	Methadone
      case
        when dailyDose.value between 1 and 20 then 4
        when dailyDose.value between 21 and 40 then 8
        when dailyDose.value between 41 and 60 then 10
        when dailyDose.value >= 61 then 12
        else 1000 // Really ought to be an error because it represents an unexpected dose range...
      end
    )
    when 7052 then 1 //	Morphine
    when 7242 then 0 //	Naloxone
    when 7243 then 0 //	Naltrexone
    when 7804 then 1.5 //	Oxycodone
    when 7814 then 3 //	Oxymorphone
    when 8001 then 0.37 //	Pentazocine
    when 8163 then 0 //	Phenylephrine
    when 8175 then 0 //	Phenylpropanolamine
    when 8745 then 0 //	Promethazine
    when 8896 then 0 //	Pseudoephedrine
    when 9009 then 0 //	Pyrilamine
    when 10689 then 0.1 //	Tramadol
    when 10849 then 0 //	Triprolidine
    when 19759 then 0 //	bromodiphenhydramine
    when 19860 then 0 //	butalbital
    when 22696 then 0 //	dexbrompheniramine
    when 22697 then 0 //	dexchlorpheniramine
    when 23088 then 0.25 //	dihydrocodeine
    when 27084 then 0 //	homatropine
    when 35780 then 0 //	ropivacaine
    when 237005 then 8 //	Levomethadyl (NOTE: given as Levomethadyl acetate in the CDC conversion table)
    when 636827 then 0 //	guaiacolsulfonate
    when 787390 then 0.4 //	tapentadol
    else 0
  end

define function EnsureMicrogramQuantity(strength Quantity):
  if strength.value < 0.1 and (PositionOf('mg', strength.unit) = 0) then
    Quantity {
      value: strength.value * 1000,
      unit: 'mcg' + Substring(strength.unit, 2)
    }
  else
    strength

/*
  Returns the non-surgical opioid ingredients and their strengths that
  make up the drug identified by the given rxNormCode as a list of tuples:

  List<Tuple {
    rxNormCode Code,
    doseFormCode Code,
    doseFormName String,
    ingredientCode Code,
    ingredientName String,
    strength Quantity
  }>
*/
define function GetIngredients(rxNormCode Code):
  (
    [MED_SCDC_FOR_DRUG: DRUG_RXCUI in rxNormCode] SD
      where exists ([MED_DRUG_VALUE_SET: DRUG_RXCUI in SD.DRUG_RXCUI])
      return {
        rxNormCode: rxNormCode,
        component: SingletonFrom([MED_SCDC: SCDC_RXCUI in SD.SCDC_RXCUI]),
        ingredientCode: SingletonFrom([MED_SCDC: SCDC_RXCUI in SD.SCDC_RXCUI]).INGREDIENT_RXCUI,
        doseFormCode: SingletonFrom([MED_DRUG: DRUG_RXCUI in SD.DRUG_RXCUI]).DOSE_FORM_RXCUI // Could potentially look this up only once...
      }
  ) C
    let
      ingredient: SingletonFrom([MED_INGREDIENT: INGREDIENT_RXCUI in C.ingredientCode]),
      doseForm: SingletonFrom([MED_DOSE_FORM: DOSE_FORM_RXCUI in C.doseFormCode])
    where exists (
      [MED_INGREDIENT_TYPE: INGREDIENT_RXCUI in C.ingredientCode] IT
        where IT.INGREDIENT_TYPE = 'Opioid_NonSurgical'
    )
    return {
      rxNormCode: rxNormCode,
      doseFormCode: C.doseFormCode,
      doseFormName: doseForm.DOSE_FORM_NAME,
      ingredientCode: C.ingredientCode,
      ingredientName: ingredient.INGREDIENT_NAME,
      strength:
        EnsureMicrogramQuantity(
          Quantity {
            value: C.component.STRENGTH_VALUE,
            unit: ToUCUM(C.component.STRENGTH_UNIT)
          }
        )
    }

/*
  Calculates daily dose for a specific ingredient based on the ingredient strength, dose form, dose quantity, and daily frequency
*/
define function GetDailyDose(ingredientCode Code, strength Quantity, doseFormCode Code, doseQuantity Quantity, dosesPerDay Decimal):
  case
	  // if patch --> daily dose = dose value (e.g, number patches with doseQuantity unit = "patch") * per-hour strength
    when IsPatch(doseFormCode) then
      // buprenorphine or fentanyl patch
      if ToInteger(ingredientCode.code) in { 1819, 4337 } then
        Quantity { value: dosesPerDay * doseQuantity.value * strength.value, unit: strength.unit }
      else
        null

    // if dose unit in actual mass units (mg or mcg -- when it's a single med) --> daily dose = numTimesPerDay * dose
    when doseQuantity.unit in { 'mg', 'mcg' } then
      Quantity { value: dosesPerDay * doseQuantity.value, unit: doseQuantity.unit }

    // if doseQuantity is in actual volume units (mL) --> daily dose = numTimesPerDay * dose * strength
    when doseQuantity.unit = 'mL' and (PositionOf('/mL', strength.unit) = Length(strength.unit) - 3) then
      Quantity { value: dosesPerDay * doseQuantity.value * strength.value, unit: Substring(strength.unit, 0, PositionOf('/', strength.unit)) }

		// if doseQuantity is not in actual units (e.g., 1 tab, 1 spray -- when it's a combo med with a unit of tablet, or it's mg/actuat) --> daily dose = numTimesPerDay * dose value * strength value
    else
      Quantity { value: dosesPerDay * doseQuantity.value * strength.value, unit: Substring(strength.unit, 0, PositionOf('/', strength.unit)) }
  end
  
define function GetMedicationName(rxNormCode Code):
  SingletonFrom([MED_DRUG: DRUG_RXCUI in rxNormCode]).DRUG_NAME

/*
  Builds a description for the daily dose for an ingredient
*/
define function GetDailyDoseDescription(ingredientCode Code, ingredientName String, strength Quantity, doseFormCode Code, doseFormName String, doseQuantity Quantity, dosesPerDay Decimal, dailyDose Quantity):
  case
    // if patch
    when IsPatch(doseFormCode) then
      // buprenorphine or fentanyl patch
      if ToInteger(ingredientCode.code) in { 1819, 4337 } then
        ingredientName + ' patch: ' + ToString(doseQuantity.value) + ' * ' + ToString(strength) + ' = ' + ToString(dailyDose)
      else
        null

    // if dose unit in actual mass units (mg or mcg -- when it's a single med)
    when doseQuantity.unit in { 'mg', 'mcg' } then
      ingredientName + ' ' + doseFormName + ': ' + ToString(dosesPerDay) + '/d * ' + ToString(doseQuantity) + ' = ' + ToString(dailyDose)

    // if doseQuantity in actual volume units (mL) or not in actual units (e.g. 1 tab, 1 spray)
    else
      ingredientName + ' ' + doseFormName + ': ' + ToString(dosesPerDay) + '/d * ' + ToString(doseQuantity) + ' * ' + ToString(strength) + ' = ' + ToString(dailyDose)
  end

/*
  Calculates MMEs for the given input prescription information and returns it
  as a list of tuples:

  List<Tuple {
    rxNormCode Code,
    doseFormCode Code,
    doseQuantity Quantity,
    dosesPerDay Decimal,
    ingredientCode Code,
    ingredientName String,
    strength Quantity,
    dailyDose Quantity,
    dailyDoseDescription String,
    conversionFactor Decimal,
    mme Quantity
  }>
*/
define function CalculateMMEs(medications List<Tuple { rxNormCode Code, doseQuantity Quantity, dosesPerDay Decimal }>):
  Flatten(
    medications M
      let Ingredients: GetIngredients(M.rxNormCode)
      return
        Ingredients I
          let
            adjustedDoseQuantity: EnsureMicrogramQuantity(M.doseQuantity),
            dailyDose: GetDailyDose(I.ingredientCode, I.strength, I.doseFormCode, adjustedDoseQuantity, M.dosesPerDay),
            factor: GetConversionFactor(I.ingredientCode, dailyDose, I.doseFormCode)
          return {
            rxNormCode: M.rxNormCode,
            doseFormCode: I.doseFormCode,
            doseQuantity: adjustedDoseQuantity,
            dosesPerDay: M.dosesPerDay,
            ingredientCode: I.ingredientCode,
            ingredientName: I.ingredientName,
            strength: I.strength,
            dailyDose: dailyDose,
            dailyDoseDescription: GetDailyDoseDescription(I.ingredientCode, I.ingredientName, I.strength, I.doseFormCode, I.doseFormName, adjustedDoseQuantity, M.dosesPerDay, dailyDose),
            conversionFactor: factor,
            mme: Quantity {
              value: dailyDose.value * factor,
              unit: dailyDose.unit + '/d'
            }
          }
  )

define TestCalculateMMEs:
  CalculateMMEs({ { rxNormCode: Code '388508' from RxNorm, doseQuantity: Quantity { value: 1, unit: 'patch' }, dosesPerDay: 0.33 } })
