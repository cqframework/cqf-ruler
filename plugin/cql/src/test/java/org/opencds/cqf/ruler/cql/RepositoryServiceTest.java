package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.utility.Ids;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RepositoryServiceTest {

	@Test
	public void releaseResource_test() {

		IIdType id = Ids.newId(FhirContext.forDstu3Cached(), "ersdv2bundle1-2");
		assertTrue(id instanceof org.hl7.fhir.dstu3.model.IdType);
		RepositoryService repositoryService = new RepositoryService();
		Resource resource = repositoryService.releaseResource(id);
		assertTrue(!resource.getId().contains("draft-"));

		Bundle bundle = new Bundle();
		bundle.getEntry().forEach(
			entry -> {
				if(entry.hasResource() && entry.getResource() instanceof Library) {
					assertTrue(((Library) entry.getResource()).hasStatus());
					assertTrue(((Library) entry.getResource()).getStatus().equals("Active"));
				}
			}
		);

	}

	@Test
	public void publishResource_test() {

		IIdType id = Ids.newId(FhirContext.forDstu3Cached(), "ersdv2bundle1-2");
		assertTrue(id instanceof org.hl7.fhir.dstu3.model.IdType);
		RepositoryService repositoryService = new RepositoryService();
		Resource resource = repositoryService.publishResource(id);

		assertTrue(!resource.getId().contains("draft-"));

		Bundle bundle = new Bundle();
		bundle.getEntry().forEach(
			entry -> {
				if(entry.hasResource() && entry.getResource() instanceof Library) {
					assertTrue(((Library) entry.getResource()).hasStatus());
					assertTrue(((Library) entry.getResource()).getStatus().equals("Active"));
				}
			}
		);
	}
}
