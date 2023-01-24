
package org.opencds.cqf.ruler.cql;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.Order;

import java.util.List;

import static graphql.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.part;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = { RepositoryServiceTest.class, CqlConfig.class },
	properties = {"hapi.fhir.fhir_version=r4", "hapi.fhir.security.basic_auth.enabled=false"})
@TestMethodOrder(MethodOrderer.MethodName.class)
class RepositoryServiceTest extends RestIntegrationTest {

	@Test
	@Order(1)
	void draftOperation_active_test() {
		loadTransaction("ersd-active-transaction-bundle-example.json");

		Resource returnResource = getClient().operation()
			.onInstance("Library/SpecificationLibrary")
			.named("$draft")
			.withNoParameters(Parameters.class)
			.returnResourceType(Library.class)
			.execute();

		Library returnedLibrary = (Library)returnResource;
		assertNotNull(returnedLibrary);
		assertTrue(((Library)returnedLibrary).getStatus() == Enumerations.PublicationStatus.DRAFT);
		List<RelatedArtifact> relatedArtifacts = returnedLibrary.getRelatedArtifact();
		assertTrue(!relatedArtifacts.isEmpty());
		assertTrue(Canonicals.getVersion(relatedArtifacts.get(0).getResource()) == null);
		assertTrue(Canonicals.getVersion(relatedArtifacts.get(1).getResource()) == null);
	}

	@Test
	@Order(2)
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
		} catch ( Exception e) {
			actualMessage = e.getMessage();
		}

		assertTrue(actualMessage.contains("Drafts can only be created from artifacts with status of 'active'. Resource 'http://ersd.aimsplatform.org/fhir/Library/DraftSpecificationLibrary' has a status of: DRAFT"));
	}

	@Test
	@Order(3)
	void releaseResource_test() {
		loadTransaction("ersd-draft-transaction-bundle-example.json");
		Library returnResource = getClient().operation()
			.onInstance("Library/SpecificationLibrary")
			.named("$release")
			.withNoParameters(Parameters.class)
			.useHttpGet()
			.returnResourceType(Library.class)
			.execute();

		assertNotNull(returnResource);
	}

	@Test
	@Order(4)
	void publishResource_test() {
		Library specLibrary = (Library) readResource("ersd-active-library-example.json");
		specLibrary.setName("NewSpecificationLibrary");
		specLibrary.setId((String) null);

		Parameters params = parameters(part("resource", (MetadataResource)specLibrary) );

		Library returnResource = getClient().operation()
			.onServer()
			.named("$publish")
			.withParameters(params)
			.returnResourceType(Library.class)
			.execute();

		assertNotNull(returnResource);
		assertTrue(returnResource.getName().equals("NewSpecificationLibrary"));
	}

	@Test
	@Order(5)
	void reviseOperation_draft_test() {
		String newResourceName = "NewSpecificationLibrary";
		Library library = (Library)loadResource("ersd-draft-library-example.json");
		library.setName(newResourceName);
		String errorMessage = "";
		Parameters params = parameters(part("resource", library) );
		Library returnResource = null;
		try {
			returnResource = getClient().operation()
				.onServer()
				.named("$revise")
				.withParameters(params)
				.returnResourceType(Library.class)
				.execute();
		} catch ( Exception e) {
			errorMessage = e.getMessage();
		}

		assertTrue(errorMessage.isEmpty());
		assertTrue(returnResource != null);
		assertTrue(returnResource.getName().equals(newResourceName));
	}

	@Test
	@Order(6)
	void reviseOperation_active_test() {
		loadResource("ersd-active-library-example.json");
		Library specLibrary = (Library) readResource("ersd-active-library-example.json");
		specLibrary.setName("NewSpecificationLibrary");
		String actualMessage = "";
		Parameters params = parameters( part("resource", specLibrary) );
		try {
			Library returnResource = getClient().operation()
				.onServer()
				.named("$revise")
				.withParameters(params)
				.returnResourceType(Library.class)
				.execute();
		} catch ( Exception e) {
			actualMessage = e.getMessage();
		}

		assertTrue(actualMessage.contains("Only resources with status of 'draft' can be revised."));
	}

<<<<<<< Updated upstream
	@Test
	void packageOperation_active_test() {
		loadTransaction("ersd-active-transaction-bundle-example.json");

		Resource returnBundle = getClient().operation()
			.onInstance("Library/SpecificationLibrary")
			.named("$package")
			.withNoParameters(Parameters.class)
			.returnResourceType(Bundle.class)
			.execute();

		assertNotNull(returnBundle);
	}

=======
>>>>>>> Stashed changes
	@Test
	@Order(7)
	void packageOperation_draft_test() {
		loadTransaction("ersd-draft-transaction-bundle-example.json");

		String actualMessage = "";
		try {
			Resource returnBundle = getClient().operation()
				.onInstance("Library/DraftSpecificationLibrary")
				.named("$package")
				.withNoParameters(Parameters.class)
				.returnResourceType(Bundle.class)
				.execute();
		} catch ( Exception e) {
			actualMessage = e.getMessage();
		}

		assertTrue(actualMessage.contains("Only resources with status of 'active' can be packaged."));
	}

	@Test
	@Order(8)
	void packageOperation_active_id_test() {
		loadResource("ersd-active-library-example.json");
		Library specLibrary = (Library) readResource("ersd-active-library-example.json");
		specLibrary.setName("NewSpecificationLibrary");
		specLibrary.setStatus(Enumerations.PublicationStatus.ACTIVE);
		specLibrary.setId("");
		String actualMessage = "";
		Parameters params = parameters( part("resource", specLibrary) );
		try {
			Library returnResource = getClient().operation()
				.onInstance("Library/SpecificationLibrary")
				.named("$package")
				.withParameters(params)
				.returnResourceType(Library.class)
				.execute();
		} catch ( Exception e) {
			actualMessage = e.getMessage();
			assertTrue(actualMessage.contains("The resource must have a valid id to be packaged."));
		}
	}
}