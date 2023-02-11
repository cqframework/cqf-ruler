
package org.opencds.cqf.ruler.cql;

import static graphql.Assert.assertNotNull;
//import static graphql.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.part;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.DatatypeConverter;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ContactDetail;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.UriType;
//import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestMethodOrder;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.ruler.cql.r4.ArtifactCommentExtension;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.core.annotation.Order;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = {RepositoryServiceTest.class, CqlConfig.class},
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

		Library returnedLibrary = (Library)returnResource;
		assertNotNull(returnedLibrary);
		assertTrue(((Library)returnedLibrary).getStatus() == Enumerations.PublicationStatus.DRAFT);
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
		} catch ( Exception e) {
			actualMessage = e.getMessage();
		}

		assertTrue(actualMessage.contains("Drafts can only be created from artifacts with status of 'active'. Resource 'http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary' has a status of: DRAFT"));
	}

	@Test
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

	@Test
	void packageOperation_no_id_test() {
		loadTransaction("ersd-active-transaction-bundle-example.json");
		Bundle specLibrary = (Bundle) readResource("ersd-active-transaction-bundle-example.json");
		specLibrary.setId("");
		String actualMessage = "";
		IBaseBundle returnBundle = null;
		try {
			returnBundle = getClient().operation()
				.onInstance("Library/SpecificationLibrary")
				.named("$package")
				.withNoParameters(Parameters.class)
				.returnResourceType(Bundle.class)
				.execute();
		} catch ( Exception e) {
			actualMessage = e.getMessage();
			assertTrue(actualMessage.contains("The resource must have a valid id to be packaged."));
		}

		assertNotNull(returnBundle);
	}

	@Test
	void packageOperation_id_test() {
		loadTransaction("ersd-active-transaction-bundle-example.json");
		Bundle specLibrary = (Bundle) readResource("ersd-active-transaction-bundle-example.json");
		specLibrary.setId("NewSpecificationLibrary");
		String actualMessage = "";
		IBaseBundle returnBundle = null;
		try {
			returnBundle = getClient().operation()
				.onInstance("Library/SpecificationLibrary")
				.named("$package")
				.withNoParameters(Parameters.class)
				.returnResourceType(Bundle.class)
				.execute();
		} catch ( Exception e) {
			actualMessage = e.getMessage();
			assertTrue(actualMessage.contains("The resource must have a valid id to be packaged."));
		}

		assertNotNull(returnBundle);
	}

	@Test
	void approveOperation_twice_appends_artifactComment_test() {
		loadResource("ersd-active-library-example.json");
		DateType approvalDate = new DateType(DatatypeConverter.parseDate("2022-12-12").getTime());
		Parameters params = parameters( 
			part("approvalDate", approvalDate),
			part("artifactCommentType", "documentation")
		);	
		Library returnedResource = null;
		//once
		getClient().operation()
			.onInstance("Library/SpecificationLibrary")
			.named("$approve")
			.withParameters(params)
			.returnResourceType(Library.class)
			.execute();
			// twice
			returnedResource = getClient().operation()
			.onInstance("Library/SpecificationLibrary")
			.named("$approve")
			.withParameters(params)
			.returnResourceType(Library.class)
			.execute();
		assertNotNull(returnedResource);

		// Artifact Comment Extension was appended	
		assertTrue(returnedResource.getExtension().size() == 2);
	}

	@Test
	void approveOperation_twice_appends_endorser_test() {
		loadResource("ersd-active-library-example.json");
		DateType approvalDate = new DateType(DatatypeConverter.parseDate("2022-12-12").getTime());
		String endorserName1 = "EndorserName";
		String endorserName2 = "EndorserName2";
		ContactDetail endorser1 = new ContactDetail();
		ContactDetail endorser2 = new ContactDetail();
		endorser1.setName(endorserName1);
		endorser2.setName(endorserName2);
		Parameters params1 = parameters( 
			part("approvalDate", approvalDate),
			part("endorser", endorser1)
		);	
		Parameters params2 = parameters( 
			part("approvalDate", approvalDate),
			part("endorser", endorser2)
		);	
		Library returnedResource = null;
		//once
		getClient().operation()
			.onInstance("Library/SpecificationLibrary")
			.named("$approve")
			.withParameters(params1)
			.returnResourceType(Library.class)
			.execute();
			// twice
			returnedResource = getClient().operation()
			.onInstance("Library/SpecificationLibrary")
			.named("$approve")
			.withParameters(params2)
			.returnResourceType(Library.class)
			.execute();
		assertNotNull(returnedResource);

		// Endorser was appended	
		assertTrue(returnedResource.getEndorser().size() == 2);
	}

	@Test
	void approveOperation_twice_updates_endorser_test() {
		loadResource("ersd-active-library-example.json");
		DateType approvalDate = new DateType(DatatypeConverter.parseDate("2022-12-12").getTime());
		String endorserName = "EndorserName";
		ContactDetail endorser = new ContactDetail();
		endorser.setName(endorserName);
		ContactPoint newContact = new ContactPoint();
		String testContactValue = "test";
		newContact.setValue(testContactValue);
		Parameters params1 = parameters( 
			part("approvalDate", approvalDate),
			part("endorser", endorser)
		);	
		Library returnedResource = null;
		//once
		getClient().operation()
			.onInstance("Library/SpecificationLibrary")
			.named("$approve")
			.withParameters(params1)
			.returnResourceType(Library.class)
			.execute();
			endorser.setTelecom((List<ContactPoint>) Arrays.asList(newContact));
			Parameters params2 = parameters( 
			part("approvalDate", approvalDate),
			part("endorser", endorser)
		);	
			// twice
			returnedResource = getClient().operation()
			.onInstance("Library/SpecificationLibrary")
			.named("$approve")
			.withParameters(params2)
			.returnResourceType(Library.class)
			.execute();
		assertNotNull(returnedResource);

		// Endorser was appended	
		assertTrue(returnedResource.getEndorser().size() == 1);
		assertTrue(returnedResource.getEndorser().get(0).getTelecom().get(0).getValue().equals(testContactValue));
	}

	@Test
	void approveOperation_test() {
		loadResource("ersd-active-library-example.json");
		String approvalDateString = "2022-12-12";
		DateType approvalDate = new DateType(DatatypeConverter.parseDate(approvalDateString).getTime());
		String artifactCommentType = "documentation";
		String artifactCommentText = "comment text";
		String artifactCommentTarget= "target";
		String artifactCommentReference="reference-valid-no-spaces";
		String artifactCommentUser="user";
		String endorserName = "EndorserName";
		ContactDetail endorser = new ContactDetail();
		endorser.setName(endorserName);
		Parameters params = parameters( 
			part("approvalDate", approvalDate),
			part("artifactCommentType", artifactCommentType),
			part("artifactCommentText", artifactCommentText),
			part("artifactCommentTarget", artifactCommentTarget),
			part("artifactCommentReference", artifactCommentReference),
			part("artifactCommentUser", artifactCommentUser),
			part("endorser", endorser)
		);	
		Library returnedResource = null;
		returnedResource = getClient().operation()
			.onInstance("Library/SpecificationLibrary")
			.named("$approve")
			.withParameters(params)
			.returnResourceType(Library.class)
			.execute();

		assertNotNull(returnedResource);
				
		// Approval date is correct
		assertTrue(returnedResource.getApprovalDateElement().asStringValue().equals(approvalDateString));
		// match Libray.date to Library.meta.lastUpdated precision before comparing
		returnedResource.getMeta().getLastUpdatedElement().setPrecision(TemporalPrecisionEnum.SECOND);
		// date is correct
		assertTrue(returnedResource.getDateElement().asStringValue().equals(returnedResource.getMeta().getLastUpdatedElement().asStringValue()));
		
		// Artifact Comment Extension is correct	
		Optional<Extension> artifactCommentExtension = returnedResource.getExtension().stream().filter(e -> e.getUrl().equals(ArtifactCommentExtension.ARTIFACT_COMMENT_EXTENSION_URL)).findFirst();
		assertTrue(artifactCommentExtension.isPresent());
		assertTrue(artifactCommentExtension.get().getExtensionByUrl(ArtifactCommentExtension.TYPE).getValue().toString().equals(artifactCommentType));
		assertTrue(artifactCommentExtension.get().getExtensionByUrl(ArtifactCommentExtension.TEXT).getValue().toString().equals(artifactCommentText));
		assertTrue(artifactCommentExtension.get().getExtensionByUrl(ArtifactCommentExtension.TARGET).getValue().toString().equals( artifactCommentTarget));
		assertTrue(((UriType) artifactCommentExtension.get().getExtensionByUrl(ArtifactCommentExtension.REFERENCE).getValue()).asStringValue().equals(artifactCommentReference));
		assertTrue(artifactCommentExtension.get().getExtensionByUrl(ArtifactCommentExtension.USER).getValue().toString().equals(artifactCommentUser));
		
		// Endorser is correct
		assertTrue(returnedResource.getEndorser().get(0).getName().equals(endorserName));
		
	}

}

