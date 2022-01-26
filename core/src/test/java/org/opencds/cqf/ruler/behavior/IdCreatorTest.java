package org.opencds.cqf.ruler.behavior;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.Test;

import ca.uhn.fhir.context.FhirContext;

public class IdCreatorTest implements IdCreator {

	@Override
	public FhirContext getFhirContext() {
		return FhirContext.forR4Cached();
	}
	
	@Test
	public void createIdFromString() {
		IIdType id = newId("Measure/123");

		assertNotNull(id);
		assertThat(id, instanceOf(IdType.class));
		assertEquals("Measure", id.getResourceType());
		assertEquals("123", id.getIdPart());
	}

	@Test
	public void createIdFromParts() {
		IIdType id = newId("Measure", "123");

		assertNotNull(id);
		assertThat(id, instanceOf(IdType.class));
		assertEquals("Measure", id.getResourceType());
		assertEquals("123", id.getIdPart());
	}
}
