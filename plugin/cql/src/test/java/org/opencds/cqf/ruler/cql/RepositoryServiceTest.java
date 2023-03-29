
package org.opencds.cqf.ruler.cql;

import static graphql.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.booleanPart;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.codePart;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.part;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.stringPart;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.DatatypeConverter;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.ContactDetail;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.ruler.cql.r4.ArtifactAssessment;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = {RepositoryServiceTest.class, CqlConfig.class},
	properties = {"hapi.fhir.fhir_version=r4", "hapi.fhir.security.basic_auth.enabled=false"})

class RepositoryServiceTest extends RestIntegrationTest {
	private final String specificationLibReference = "Library/SpecificationLibrary";
	@Test
	void draftOperation_active_test() {
		loadTransaction("ersd-active-transaction-bundle-example.json");

		Parameters params = parameters(part("version", "1.1.1") );
		Resource returnResource = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$draft")
			.withParameters(params)
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

	//@Test
	//void releaseResource_test() {
	//	loadTransaction("ersd-draft-transaction-bundle-example.json");
	//	Library returnResource = getClient().operation()
	//		.onInstance("Library/DraftSpecificationLibrary")
	//		.named("$release")
	//		.withNoParameters(Parameters.class)
	//		.useHttpGet()
	//		.returnResourceType(Library.class)
	//		.execute();

	@Test
	void releaseResource_test() {
		loadTransaction("ersd-release-bundle.json");
		String versionData = "1234";

		Parameters params1 = parameters(
			stringPart("version", "1234"),
			codePart("version-behavior", "default")
		);

		Library returnResource = getClient().operation()
			.onInstance("Library/ReleaseSpecificationLibrary")
			.named("$release")
			.withParameters(params1)
			.useHttpGet()
			.returnResourceType(Library.class)
			.execute();

		assertNotNull(returnResource);
	}

	@Test
	void releaseResource_latestFromTx_NotSupported_test() {
		loadTransaction("ersd-release-bundle.json");
		String actualErrorMessage = "";

		Parameters params1 = parameters(
			stringPart("version", "1234"),
			codePart("version-behavior", "default"),
			booleanPart("latest-from-tx-server", true)
		);

		try {
			Library returnResource = getClient().operation()
				.onInstance("Library/ReleaseSpecificationLibrary")
				.named("$release")
				.withParameters(params1)
				.useHttpGet()
				.returnResourceType(Library.class)
				.execute();
		} catch (Exception e) {
			actualErrorMessage = e.getMessage();
			assertTrue(actualErrorMessage.contains("Support for 'latestFromTxServer' is not yet implemented."));
		}
	}

	@Test
	void release_missing_approvalDate_validation_test() {
		loadTransaction("ersd-release-missing-approvalDate-validation-bundle.json");
		String versionData = "1234";
		String actualErrorMessage = "";

		Parameters params1 = parameters(
			stringPart("version", "1234"),
			codePart("version-behavior", "default"),
			booleanPart("latest-from-tx-server", true)
		);

		try {
			Library returnResource = getClient().operation()
				.onInstance("Library/ReleaseSpecificationLibrary")
				.named("$release")
				.withParameters(params1)
				.useHttpGet()
				.returnResourceType(Library.class)
				.execute();
		} catch (Exception e) {
			actualErrorMessage = e.getMessage();
			assertTrue(actualErrorMessage.contains("The artifact must be approved (indicated by approvalDate) before it is eligible for release."));
		}
	}

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

	@Test
	void approveOperation_endpoint_id_should_match_target_parameter() {
		loadResource("ersd-active-library-example.json");
		String artifactCommentTarget= "Library/This-Library-Does-Not-Exist";
		Parameters params = parameters( 
			part("artifactCommentTarget", new CanonicalType(artifactCommentTarget))
		);	
		try {
			getClient().operation()
			.onInstance(specificationLibReference)
			.named("$approve")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();
		} catch (UnprocessableEntityException e) {
			assertNotNull(e);
		}
	}
	@Test
	void approveOperation_should_respect_artifactAssessment_information_type_binding() {
		loadResource("ersd-active-library-example.json");
		String artifactCommentType = "this-type-does-not-exist";
		Parameters params = parameters( 
			part("artifactCommentType", artifactCommentType)
		);	
		try {
			getClient().operation()
			.onInstance(specificationLibReference)
			.named("$approve")
			.withParameters(params)
			.returnResourceType(Library.class)
			.execute();
		} catch (UnprocessableEntityException e) {
			assertNotNull(e);
		}
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
		//once
		getClient().operation()
			.onInstance(specificationLibReference)
			.named("$approve")
			.withParameters(params1)
			.returnResourceType(Bundle.class)
			.execute();
			// twice
			getClient().operation()
			.onInstance(specificationLibReference)
			.named("$approve")
			.withParameters(params2)
			.returnResourceType(Bundle.class)
			.execute();

		// Endorser was appended	
		Library lib = getClient().fetchResourceFromUrl(Library.class, specificationLibReference);
		assertTrue(lib.getEndorser().size() == 2);
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
		//once
		getClient().operation()
			.onInstance(specificationLibReference)
			.named("$approve")
			.withParameters(params1)
			.returnResourceType(Bundle.class)
			.execute();
			endorser.setTelecom((List<ContactPoint>) Arrays.asList(newContact));
			Parameters params2 = parameters( 
			part("approvalDate", approvalDate),
			part("endorser", endorser)
		);	
			// twice
			getClient().operation()
			.onInstance(specificationLibReference)
			.named("$approve")
			.withParameters(params2)
			.returnResourceType(Bundle.class)
			.execute();

		// Endorser was updated	
		Library lib = getClient().fetchResourceFromUrl(Library.class, specificationLibReference);
		assertTrue(lib.getEndorser().size() == 1);
		assertTrue(lib.getEndorser().get(0).getTelecom().get(0).getValue().equals(testContactValue));
	}

	@Test
	void approveOperation_test() {
		loadResource("ersd-active-library-example.json");
		loadResource("practitioner-example-for-refs.json");
		String approvalDateString = "2022-12-12";
		DateType approvalDate = new DateType(DatatypeConverter.parseDate(approvalDateString).getTime());
		String artifactCommentType = "comment";
		String artifactCommentText = "comment text";
		String artifactCommentTarget= "http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary" + "|1.0.0";
		String artifactCommentReference="reference-valid-no-spaces";
		String artifactCommentUser= "Practitioner/sample-practitioner";
		String endorserName = "EndorserName";
		ContactDetail endorser = new ContactDetail();
		endorser.setName(endorserName);
		Parameters params = parameters( 
			part("approvalDate", approvalDate),
			part("artifactCommentType", artifactCommentType),
			part("artifactCommentText", artifactCommentText),
			part("artifactCommentTarget", new CanonicalType(artifactCommentTarget)),
			part("artifactCommentReference", new CanonicalType(artifactCommentReference)),
			part("artifactCommentUser", new Reference(artifactCommentUser)),
			part("endorser", endorser)
		);	
		Bundle returnedResource = null;
		returnedResource = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$approve")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();

		assertNotNull(returnedResource);
		Library lib = getClient().fetchResourceFromUrl(Library.class, specificationLibReference);
		assertNotNull(lib);
		// Approval date is correct
		assertTrue(lib.getApprovalDateElement().asStringValue().equals(approvalDateString));
		// match Libray.date to Library.meta.lastUpdated precision before comparing
		lib.getMeta().getLastUpdatedElement().setPrecision(TemporalPrecisionEnum.SECOND);
		// date is correct
		assertTrue(lib.getDateElement().asStringValue().equals(lib.getMeta().getLastUpdatedElement().asStringValue()));
		// ArtifactAssessment is saved as type Basic, update when we change to OperationOutcome
		// Get the reference from BundleEntry.response.location
		Optional<BundleEntryComponent> maybeArtifactAssessment = returnedResource.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains("Basic")).findAny();
		assertTrue(maybeArtifactAssessment.isPresent());
		ArtifactAssessment artifactAssessment = getClient().fetchResourceFromUrl(ArtifactAssessment.class,maybeArtifactAssessment.get().getResponse().getLocation());
		assertNotNull(artifactAssessment);
		assertTrue(artifactAssessment.isValidArtifactComment());
		assertTrue(artifactAssessment.checkArtifactCommentParams(
			artifactCommentType,
			artifactCommentText,
      specificationLibReference,
			artifactCommentReference,
			artifactCommentTarget,
			artifactCommentUser
		));
		// Endorser is correct
		assertTrue(lib.getEndorser().get(0).getName().equals(endorserName));
		
	}

}

