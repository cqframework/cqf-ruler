# cqf-ruler

The CQF Ruler is an implementation of FHIR's [Clinical Reasoning Module](
http://hl7.org/fhir/clinicalreasoning-module.html) and serves as a
knowledge artifact repository and clinical decision support service.

## Usage 

 - `mvn jetty:run -am --projects cqf-ruler-dstu3`
   - Starts embedded Jetty server accessible at base URL `http://localhost:8080/cqf-ruler-dstu3/`

For R4 use `mvn jetty:run -am --projects cqf-ruler-r4`

 - `mvn package -DskipTests=false`
   - Builds the project war file (cqf-ruler-dstu3.war and cqf-ruler-r4.war in the projects' target directory) 
   - Runs the test suite (tests are skipped by default)
 
Visit the [wiki](https://github.com/DBCG/cqf-ruler/wiki) for more documentation.

## Dependencies

Before the instructions in the above "Usage" section will work, you need to
install several primary dependencies.

### Java

Go to [http://www.oracle.com/technetwork/java/javase/downloads/](
http://www.oracle.com/technetwork/java/javase/downloads/) and download the
latest (version 11 or higher) JDK for your platform, and install it.

### Apache Maven 3.5.3

Go to [https://maven.apache.org](https://maven.apache.org), visit the main
"Download" page, and under "Files" download the 3.5.3 binary.  Then unpack that archive file and follow the installation
instructions in its README.txt.  The end result of this should be that the
binary "mvn" is now in your path.
