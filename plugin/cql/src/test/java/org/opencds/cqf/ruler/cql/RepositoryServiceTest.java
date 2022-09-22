package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.utility.Ids;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RepositoryServiceTest {

	private static final String dateValid = "2022-01-01";
	private static final String dateValidAfter = "2022-06-01";
	private static final String dateInvalid = "bad-date";

	private static final RequestDetails requestDetails = new ServletRequestDetails();
	{
		requestDetails.addParameter("dateValid", new String[] { dateValid });
		requestDetails.addParameter("dateAfter", new String[] { dateValidAfter });
		requestDetails.addParameter("dateMultiple", new String[] { dateValid, dateValidAfter });
		requestDetails.addParameter("dateInvalid", new String[] { dateInvalid });
		requestDetails.addParameter("dateNull", new String[] { null });
		requestDetails.addParameter("dateEmpty", new String[] { "" });
	}

	@Test
	public void releaseResource_test() {

		IdType id = Ids.newId(FhirContext.forDstu3Cached(), "ersdv2bundle1-2");
		assertTrue(id instanceof IdType);
		RepositoryService repositoryService = new RepositoryService();
		Resource resource = repositoryService.releaseOperation(requestDetails, id);
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

		IdType id = Ids.newId(FhirContext.forDstu3Cached(), "ersdv2bundle1-2");
		assertTrue(id instanceof IdType);
		RepositoryService repositoryService = new RepositoryService();
		Resource resource = repositoryService.publishVersion(requestDetails, id);

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
