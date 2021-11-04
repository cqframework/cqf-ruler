package org.opencds.cqf.ruler;

public class Application {

  public static void main(String[] args) {
    ca.uhn.fhir.jpa.starter.Application.main(args);

    //Server is now accessible at eg. http://localhost:8080/fhir/metadata
    //UI is now accessible at http://localhost:8080/
  }
}