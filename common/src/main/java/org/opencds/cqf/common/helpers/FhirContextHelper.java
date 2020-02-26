package org.opencds.cqf.common.helpers;

import ca.uhn.fhir.context.FhirContext;

public class FhirContextHelper {
    static FhirContext fhirContextDstu3 = FhirContext.forDstu3();
    static FhirContext fhirContextR4 = FhirContext.forR4();

    public static FhirContext getFhirContextDstu3(){
        return fhirContextDstu3;
    }

    public static FhirContext getFhirContextR4(){
        return fhirContextR4;
    }
}
