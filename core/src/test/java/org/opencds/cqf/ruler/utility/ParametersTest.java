package org.opencds.cqf.ruler.utility;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.ruler.utility.Parameters.getPartsByName;
import static org.opencds.cqf.ruler.utility.Parameters.newParameters;
import static org.opencds.cqf.ruler.utility.Parameters.newPart;

class ParametersTest {

    @Test
    void testStu3Parameters() {
        FhirContext fhirContext = FhirContext.forDstu3Cached();
        IBaseParameters parameters = newParameters(fhirContext);
        assertTrue(parameters instanceof org.hl7.fhir.dstu3.model.Parameters);
        parameters = newParameters(fhirContext, "stu3ParameterIdString");
        assertEquals("stu3ParameterIdString", parameters.getIdElement().getIdPart());
        parameters = newParameters(fhirContext, new org.hl7.fhir.dstu3.model.IdType("stu3ParameterIdType"));
        assertEquals("stu3ParameterIdType", parameters.getIdElement().getIdPart());
    }

    @Test
    void testStu3ParametersPartName() {
        FhirContext fhirContext = FhirContext.forDstu3Cached();
        IBaseParameters parameters = newParameters(fhirContext, newPart(fhirContext, "stu3ParameterName"));
        assertTrue(parameters instanceof org.hl7.fhir.dstu3.model.Parameters);
        org.hl7.fhir.dstu3.model.Parameters stu3Parameters = (org.hl7.fhir.dstu3.model.Parameters) parameters;
        assertEquals("stu3ParameterName", stu3Parameters.getParameterFirstRep().getName());
    }

    @Test
    void testR4Parameters() {
        FhirContext fhirContext = FhirContext.forR4Cached();
        IBaseParameters parameters = newParameters(fhirContext);
        assertTrue(parameters instanceof org.hl7.fhir.r4.model.Parameters);
        parameters = newParameters(fhirContext, "r4ParameterIdString");
        assertEquals("r4ParameterIdString", parameters.getIdElement().getIdPart());
        parameters = newParameters(fhirContext, new org.hl7.fhir.r4.model.IdType("r4ParameterIdType"));
        assertEquals("r4ParameterIdType", parameters.getIdElement().getIdPart());
    }

    @Test
    void testR4ParametersPartName() {
        FhirContext fhirContext = FhirContext.forR4Cached();
        IBaseParameters parameters = newParameters(fhirContext, newPart(fhirContext, "r4ParameterName"));
        assertTrue(parameters instanceof org.hl7.fhir.r4.model.Parameters);
        org.hl7.fhir.r4.model.Parameters r4Parameters = (org.hl7.fhir.r4.model.Parameters) parameters;
        assertEquals("r4ParameterName", r4Parameters.getParameterFirstRep().getName());
    }

    @Test
    void testR5Parameters() {
        FhirContext fhirContext = FhirContext.forR5Cached();
        IBaseParameters parameters = newParameters(fhirContext);
        assertTrue(parameters instanceof org.hl7.fhir.r5.model.Parameters);
        parameters = newParameters(fhirContext, "r5ParameterIdString");
        assertEquals("r5ParameterIdString", parameters.getIdElement().getIdPart());
        parameters = newParameters(fhirContext, new org.hl7.fhir.r5.model.IdType("r5ParameterIdType"));
        assertEquals("r5ParameterIdType", parameters.getIdElement().getIdPart());
    }

    @Test
    void testR5ParametersPartName() {
        FhirContext fhirContext = FhirContext.forR5Cached();
        IBaseParameters parameters = newParameters(fhirContext, newPart(fhirContext, "r5ParameterName"));
        assertTrue(parameters instanceof org.hl7.fhir.r5.model.Parameters);
        org.hl7.fhir.r5.model.Parameters r5Parameters = (org.hl7.fhir.r5.model.Parameters) parameters;
        assertEquals("r5ParameterName", r5Parameters.getParameterFirstRep().getName());
    }

    @Test
    void getPartsByNameTest() {
        FhirContext fhirContext = FhirContext.forR5Cached();
        IBaseParameters parameters = newParameters(fhirContext,
                newPart(fhirContext, "r5ParameterName"),
                newPart(fhirContext, "r5ParameterName1"),
                newPart(fhirContext, "r5ParameterName1"));
        List<IBase> parts = getPartsByName(fhirContext, parameters, "r5ParameterName");
        assertEquals(1, parts.size());

        parts = getPartsByName(fhirContext, parameters, "r5ParameterName1");
        assertEquals(2, parts.size());
    }
}
