# cql_measure_processor
CQL Measure Processing Component

## Usage 
  - $ mvn tomcat:run
  - NOTE: to change the default port (8080): mvn -Dmaven.tomcat.port=XXXX tomcat:run
  - Make requests:
    - http://localhost:8080/Measure/measureId/$evaluate?patient=patientId&startPeriod=startDate&endPeriod=endDate
    - Example:
      - http://localhost:8080/Measure/col/$evaluate?patient=Patient-12214&startPeriod=2014-01&endPeriod=2014-12
