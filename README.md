# cqf-ruler
CQF Ruler

Base URL: http://measure.eval.kanvix.com/cqf-ruler/baseDstu3

Tester URL: http://measure.eval.kanvix.com/cqf-ruler/tester/

CDS Discovery URL: http://measure.eval.kanvix.com/cqf-ruler/cds-services

## Usage 
 - `$ mvn install`
 - `$ mvn -Djetty.http.port=XXXX jetty:run`

### Measure Processing
 - The process for processing Measures includes the following:
   - Load all the resources you'll need to run the measure into the JPA server
     - Use the GUI for resource loading
       - run `$ mvn exec:java -Dexec.mainClass="org.opencds.cqf.helpers.ResourceLoaderGUI"`
     - Use an HTTP Client
       - For example, if you wanted to load a condition with an ID of 1058 (JSON or XML formatted), use Postman (or other HTTP client) with the following request:
       - PUT http://localhost:XXXX/cqf-ruler/baseDstu3/Condition/1058
       - OR POST http://localhost:XXXX/cqf-ruler/baseDstu3/Condition if ID isn't specified (one will be provided at creation)
     - You may also load the Terminology information (CodeSystems and ValueSets) and use this as a Terminology Service (makes measure processing much more efficient). You may also use an external service by specifying it in the request (see below)
         - To use the XlsxToValueSet converter (excel spreadsheet to FHIR ValueSet resource):
                - run `$ mvn exec:java -Dexec.args="File/Path/To/Xlsx/Spreadsheet"`
                - The output will be in the src/main/resources/valuesets directory. The file name will be the oid of the ValueSet.
   - After everything is loaded into the database you may run the measure like so:
     - GET http://localhost:XXXX/cqf-ruler/baseDstu3/Measure/col/$evaluate?patient=Patient-12214&startPeriod=2014-01&endPeriod=2014-12
       - Format:
          [] - required () - optional
          Base/Measure/[MeasureID]/$evaluate?patient=[PatientID]&startperiod=[Start date of Measure]&endPeriod=[End date of Measure]&source=(URL of Data/Temrinology provider)&user=(username for provider auth)&pass=(password for provider auth)

### Clinical Reasoning Operations
 - CDC Opioid Guidelines using PlanDefinition $apply operation
  - ***NOTE*** In order to fully use this operation for the opioid guidelines, you must obtain a copy of the RxNorm local data. This data is represented as a SQLite database file.
    - To obtain a copy of the RxNorm local data, specify your request in the form provided [here](http://www.opencds.org/ContactUs.aspx)
  - This operation is accessed via a CDS Hooks request
   - Example:
    
   POST http://measure.eval.kanvix.com/cqf-ruler/cds-services/cdc-opioid-guidance
    
```
{
    "hookInstance" : "d1577c69-dfbe-44ad-ba6d-3e05e953b2ea",
    "fhirServer" : "http://fhirtest.uhn.ca/baseDstu2",
    "hook" : "medication-prescribe",
    "user" : "Practitioner/example",
    "context" : [
        {
            "resourceType": "MedicationOrder",
            "id": "medrx001",
            "dateWritten": "2017-05-05",
            "status": "draft",
            "patient": {
                "reference": "Patient/Patient-12214"
            },
            "medicationCodeableConcept": {
                "coding": [
                    {
                        "system": "http://www.nlm.nih.gov/research/umls/rxnorm",
                        "code": "197696"
                    }
                ]
            },
            "dosageInstruction": [
                {
                    "text": "Take 40mg three times daily",
                    "timing": {
                        "repeat": {
                            "frequency": 3,
                            "frequencyMax": 3,
                            "period": 1,
                            "unit": "d"
                        }
                    },
                    "asNeededBoolean": false,
                    "doseQuantity": {
                    "value": 40,
                    "unit": "mg",
                    "system": "http://unitsofmeasure.org",
                    "code": "mg"
                    }
                }
            ],
            "dispenseRequest": {
                "quantity": {
                  "value": 3000,
                  "unit": "mg",
                  "system": "http://unitsofmeasure.org",
                  "code": "mg"
                }
            }
        }
    ],
    "patient": "Patient/Patient-12214",
    "prefetch": {
        "medication": {
            "response": {
              "status": "200 OK"
            },
            "resource": {
                "resourceType": "MedicationOrder",
                "id": "medrx002",
                "dateWritten": "2017-04-25",
                "status": "active",
                "patient": {
                "reference": "Patient/Patient-12214"
                },
                "medicationCodeableConcept": {
                    "coding": [
                        {
                            "system": "http://www.nlm.nih.gov/research/umls/rxnorm",
                            "code": "199789"
                        }
                    ]
                },
                "dosageInstruction": [
                    {
                        "text": "Take 50mg twice daily",
                        "timing": {
                            "repeat": {
                                "frequency": 2,
                                "period": 1,
                                "periodUnits": "d"
                            }
                        },
                        "asNeededBoolean": false,
                        "doseQuantity": {
                            "value": 55,
                            "unit": "mg",
                            "system": "http://unitsofmeasure.org",
                            "code": "mg"
                        }
                    }
                ],
                "dispenseRequest": {
                    "quantity": {
                        "value": 3000,
                        "unit": "mg",
                        "system": "http://unitsofmeasure.org",
                        "code": "mg"
                    }
                }
            }
        }
    }
}
```
  - The response will be a CDS Hooks info card
  
### Execution Service
 - POST the following to [base]/cql/evaluate:
```
{
    "code": "Your CQL code",
    "fhirServiceUri": "Terminology Service Endpoint",
    "fhirUser": "Username for authentication",
    "fhirPass": "Password for authentication",
    "dataServiceUri": "Fhir Data Provider Endpoint",
    "dataUser": "Username for authentication",
    "dataPass": "Password for authentication",
    "patientId": "The patient you want to run the library against"
}
```
 - This Request will produce a JSON Response in the following format:
```
[
    {
        "translator-error": "Translation error message (is the only element returned)",
        "name": "CQL Expression name",
        "location": "[row:col]",
        "resultType": "CQL Type being returned",
        "error": "Runtime error output (this may cause the omission of resultType)"
    }
]
```
 - This service is used in the [cql-runner](https://github.com/DBCG/cql_runner)

### CQL Formatter
 - POST the following to [base]/format:
```
{
    "code": "Unformatted CQL code"
}
```
 - This Request will produce a JSON Response in the following format:
```
[
    {
        "formatted-cql": "The formatted CQL code"
    }
]
```

 - This service is used in the [cql-runner](https://github.com/DBCG/cql_runner)