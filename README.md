# cql_measure_processor
CQL Measure Processing Component

## Usage 
 - `$ mvn install`
 - `$ mvn -Djetty.http.port=XXXX jetty:run`
 - HAPI Fhir server endpoint: http://localhost:XXXX/cql-measure-processor/baseDstu3
 - Tester page: http://localhost:XXXX/cql-measure-processor/tester/
 - This server specializes in Measure processing. To access this functionality you will need to follow the following steps:
   - Load all the resources you'll need to run the measure into the JPA server
     - To use the XlsxToValueSet converter (excel spreadsheet to FHIR ValueSet resource):
       - run `$ mvn exec:java -Dexec.args="File/Path/To/Xlsx/Spreadsheet"`
       - The output will be in the src/main/resources/valuesets directory. The file name will be the oid of the ValueSet.
     - There is a GUI for resource loading
       - run `$ mvn exec:java -Dexec.mainClass="org.opencds.cqf.helpers.ResourceLoaderGUI"`
     - For example, if you wanted to load a condition with an ID of 1058 (JSON or XML formatted), use Postman (or other REST client) with the following request:
       - PUT http://localhost:XXXX/cql-measure-processor/baseDstu3/Condition/1058
       - OR POST http://localhost:XXXX/cql-measure-processor/baseDstu3/Condition if ID isn't specified (one will be provided at creation)
     - You may also load the Terminology information (CodeSystems and ValueSets) and use this as a Terminology Service (makes measure processing much more efficient). You may also use an external service by specifying it in the request (see below)
     - After everything is loaded into the database you may run the measure like so:
       - GET http://localhost:XXXX/cql-measure-processor/baseDstu3/Measure/col/$evaluate?patient=Patient-12214&startPeriod=2014-01&endPeriod=2014-12
         - Format:
            [] - required () - optional
            Base/Measure/[MeasureID]/$evaluate?patient=[PatientID]&startperiod=[Start date of Measure]&endPeriod=[End date of Measure]&source=(URL of Data/Temrinology provider)&user=(username for provider auth)&pass=(password for provider auth)
