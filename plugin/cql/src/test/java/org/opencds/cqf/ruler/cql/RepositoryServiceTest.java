package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.opencds.cqf.ruler.utility.Ids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static graphql.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.ruler.utility.Parameters.newParameters;
import static org.opencds.cqf.ruler.utility.Parameters.newPart;
import static org.opencds.cqf.ruler.utility.Parameters.newStringPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.parameters;
import static org.opencds.cqf.ruler.utility.r4.Parameters.part;
import static org.opencds.cqf.ruler.utility.r4.Parameters.stringPart;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
	classes = { RepositoryServiceTest.class, CqlConfig.class },
	properties = { "hapi.fhir.fhir_version=r4", "hapi.fhir.security.enabled=true" })
public class RepositoryServiceTest extends RestIntegrationTest {

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
	void draftOperation_test() {
		Library library = new Library();
		library.setDescription("Test Library");
		library.setUrl("url");
		library.setVersion("draft-1");
		library.setStatus(Enumerations.PublicationStatus.ACTIVE);

		Parameters params = parameters(
			new IdType("Library/ersdv2bundle1-2"));

		Resource returnResource = getClient().operation()
			.onServer()
			.named("$draft")
			.withParameters(params)
			.returnResourceType(Resource.class)
			.execute();

		assertNotNull(returnResource);
	}

	@Test
	void releaseResource_test() {

		Parameters params = parameters(
			new IdType("Library/ersdv2bundle1-2"));

		Resource returnResource = getClient().operation()
			.onServer()
			.named("$release")
			.withParameters(params)
			.returnResourceType(Resource.class)
			.execute();

		assertNotNull(returnResource);
	}

	@Test
	void publishResource_test() {

		Parameters params = parameters(
			new IdType("Library/ersdv2bundle1-2"));

		Resource returnResource = getClient().operation()
			.onServer()
			.named("$publish")
			.withParameters(params)
			.returnResourceType(Resource.class)
			.execute();

		assertNotNull(returnResource);
	}
}
