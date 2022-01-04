package org.opencds.cqf.ruler.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.instance.model.api.IIdType;
import org.junit.jupiter.api.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public class IdUtilitiesTest implements IdUtilities {
    @Test
    public void testAllVersionsSupported() {
        for (FhirVersionEnum fhirVersionEnum : FhirVersionEnum.values()) {
            this.createId(fhirVersionEnum, "Patient/123");
        }
    }

    @Test
    public void testContextSupported() {
        IIdType id = this.createId(FhirContext.forDstu3Cached(), "Patient/123");
        assertTrue(id instanceof org.hl7.fhir.dstu3.model.IdType);
    }

    @Test
    public void testPartsSupported() {
        IIdType id = this.createId(FhirVersionEnum.DSTU3, "Patient","123");
        assertTrue(id instanceof org.hl7.fhir.dstu3.model.IdType);

        assertEquals("Patient", id.getResourceType());
        assertEquals("123", id.getIdPart());
    }

    @Test
    public void testClassSupported() {
        IIdType id = this.createId(org.hl7.fhir.dstu3.model.Library.class, "123");
        assertTrue(id instanceof org.hl7.fhir.dstu3.model.IdType);
        assertEquals("Library", id.getResourceType());
        assertEquals("123", id.getIdPart());
    }   
}
