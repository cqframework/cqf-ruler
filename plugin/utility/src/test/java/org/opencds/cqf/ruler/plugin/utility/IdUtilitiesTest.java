package org.opencds.cqf.ruler.plugin.utility;

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
        IIdType type = this.createId(FhirContext.forDstu3Cached(), "Patient/123");
        assertTrue(type instanceof org.hl7.fhir.dstu3.model.IdType);
    }

    @Test
    public void testPartsSupported() {
        IIdType type = this.createId(FhirVersionEnum.DSTU3, "Patient","123");
        assertTrue(type instanceof org.hl7.fhir.dstu3.model.IdType);

        assertEquals("Patient", type.getResourceType());
        assertEquals("123", type.getIdPart());
    }
}
