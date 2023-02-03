
package org.opencds.cqf.ruler.cql;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Resource;
//import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestMethodOrder;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.core.annotation.Order;

import java.util.List;

import static graphql.Assert.assertNotNull;
//import static graphql.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.booleanPart;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.part;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.stringPart;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = { RepositoryServiceTest.class, CqlConfig.class },
	properties = {"hapi.fhir.fhir_version=r4", "hapi.fhir.security.basic_auth.enabled=false"})
//@TestMethodOrder(MethodOrderer.MethodName.class)
class RepositoryServiceTest extends RestIntegrationTest {

	@Test
	void draftOperation_active_test() {
		loadTransaction("ersd-active-transaction-bundle-example.json");

		Resource returnResource = getClient().operation()
			.onInstance("Library/SpecificationLibrary")
			.named("$draft")
			.withNoParameters(Parameters.class)
			.returnResourceType(Library.class)
			.execute();

		Library returnedLibrary = (Library) returnResource;
		assertNotNull(returnedLibrary);
		assertTrue(((Library) returnedLibrary).getStatus() == Enumerations.PublicationStatus.DRAFT);
		List<RelatedArtifact> relatedArtifacts = returnedLibrary.getRelatedArtifact();
		assertTrue(!relatedArtifacts.isEmpty());
		assertTrue(Canonicals.getVersion(relatedArtifacts.get(0).getResource()) == null);
		assertTrue(Canonicals.getVersion(relatedArtifacts.get(1).getResource()) == null);
	}

	@Test
	void draftOperation_draft_test() {
		loadTransaction("ersd-draft-transaction-bundle-example.json");

		String actualMessage = "";
		try {
			Resource returnResource = getClient().operation()
				.onInstance("Library/DraftSpecificationLibrary")
				.named("$draft")
				.withNoParameters(Parameters.class)
				.returnResourceType(Library.class)
				.execute();
		} catch (Exception e) {
			actualMessage = e.getMessage();
		}

		assertTrue(actualMessage.contains("Drafts can only be created from artifacts with status of 'active'. Resource 'http://ersd.aimsplatform.org/fhir/Library/DraftSpecificationLibrary' has a status of: DRAFT"));
	}

//	@Test
//	void releaseResource_test() {
//		loadTransaction("ersd-draft-transaction-bundle-example.json");
//		String versionData = "1234";
//
//		Parameters params1 = parameters(
//			stringPart("version", "1234")//,
////			booleanPart("latestFromTxServer", true)
//		);
//
//		Library returnResource = getClient().operation()
//			.onInstance("Library/DraftSpecificationLibrary")
//			.named("$release")
//			.withParameters(params1)
//			.useHttpGet()
//			.returnResourceType(Library.class)
//			.execute();
//
//		assertNotNull(returnResource);
//	}

	@Test
	void reviseOperation_active_test() {
		Library library = (Library) loadResource("ersd-active-library-example.json");
		library.setName("NewSpecificationLibrary");
		String actualErrorMessage = "";
		Parameters params = parameters(part("resource", library));
		try {
			Library returnResource = getClient().operation()
				.onServer()
				.named("$revise")
				.withParameters(params)
				.returnResourceType(Library.class)
				.execute();
		} catch (Exception e) {
			actualErrorMessage = e.getMessage();
			assertTrue(actualErrorMessage.contains("Current resource status is 'ACTIVE'. Only resources with status of 'draft' can be revised."));
		}
	}

	@Test
	void reviseOperation_draft_test() {
		String newResourceName = "NewSpecificationLibrary";
		Library library = (Library) loadResource("ersd-draft-library-example.json");
		library.setName(newResourceName);
		String errorMessage = "";
		Parameters params = parameters(part("resource", library));
		Library returnResource = null;
		try {
			returnResource = getClient().operation()
				.onServer()
				.named("$revise")
				.withParameters(params)
				.returnResourceType(Library.class)
				.execute();
		} catch (Exception e) {
			errorMessage = e.getMessage();
		}

		assertTrue(errorMessage.isEmpty());
		assertTrue(returnResource != null);
		assertTrue(returnResource.getName().equals(newResourceName));
	}
}

