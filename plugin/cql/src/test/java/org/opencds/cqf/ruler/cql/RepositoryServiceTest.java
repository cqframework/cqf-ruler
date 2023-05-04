
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
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.ruler.cql.r4.ArtifactAssessment;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = {RepositoryServiceTest.class, CqlConfig.class},
	properties = {"hapi.fhir.fhir_version=r4", "hapi.fhir.security.basic_auth.enabled=false"})

class RepositoryServiceTest extends RestIntegrationTest {
	private final String specificationLibReference = "Library/SpecificationLibrary";
	private final String minimalLibReference = "Library/SpecificationLibraryDraftVersion-1-1-1";
	@Test
	void draftOperation_test() {
		loadTransaction("ersd-active-transaction-bundle-example.json");
		String version = "1.0.1";
		String draftedVersion = version + "-draft";
		Parameters params = parameters(part("version", version) );
		Bundle returnedBundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$draft")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();

		assertNotNull(returnedBundle);
		Optional<BundleEntryComponent> maybeLib = returnedBundle.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains("Library")).findAny();
		assertTrue(maybeLib.isPresent());
		Library lib = getClient().fetchResourceFromUrl(Library.class,maybeLib.get().getResponse().getLocation());
		assertNotNull(lib);
		assertTrue(lib.getStatus() == Enumerations.PublicationStatus.DRAFT);
		assertTrue(lib.getVersion().equals(draftedVersion));
		List<RelatedArtifact> relatedArtifacts = lib.getRelatedArtifact();
		assertTrue(!relatedArtifacts.isEmpty());
		assertTrue(Canonicals.getVersion(relatedArtifacts.get(0).getResource()).equals(draftedVersion));
		assertTrue(Canonicals.getVersion(relatedArtifacts.get(1).getResource()).equals(draftedVersion));
	}
	@Test
	void draftOperation_version_conflict_test() {
		loadTransaction("ersd-active-transaction-bundle-example.json");
		loadResource("minimal-draft-to-test-version-conflict.json");
		Parameters params = parameters(part("version", "1.1.1") );
		UnprocessableEntityException maybeException = null;
		try {
			getClient().operation()
			.onInstance(specificationLibReference)
			.named("$draft")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();
		} catch (UnprocessableEntityException e) {
			maybeException = e;
		}
		assertNotNull(maybeException);
		assertTrue(maybeException.getMessage().contains("already exists"));
	}
	
	@Test
	void draftOperation_draft_test() {
		loadResource("minimal-draft-to-test-version-conflict.json");
		Parameters params = parameters(part("version", "1.2.1") );
		UnprocessableEntityException maybeException = null;
		try {
			getClient().operation()
			.onInstance(minimalLibReference)
			.named("$draft")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();
		} catch (UnprocessableEntityException e) {
			maybeException = e;
		}
		assertNotNull(maybeException);
		assertTrue(maybeException.getMessage().contains("status of 'active'"));
	}
	@Test
	void draftOperation_wrong_id_test() {
		loadTransaction("ersd-draft-transaction-bundle-example.json");
		Parameters params = parameters(part("version", "1.3.1") );
		ResourceNotFoundException maybeException = null;
		try {
			getClient().operation()
			.onInstance("Library/there-is-no-such-id")
			.named("$draft")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();
		} catch (ResourceNotFoundException e) {
			maybeException = e;
		}
		assertNotNull(maybeException);
	}
	@Test
	void draftOperation_version_format_test() {
		loadResource("minimal-draft-to-test-version-conflict.json");
		List<String> testValues = Arrays.asList(new String[]{
			"11asd1",
			"1.1.3.1",
			"1.|1.1",
			"1/.1.1",
			"-1.-1.2",
			"1.-1.2",
			"1.1.-2",
			"1.2.3-draft",
			"",
			null
		});
		for(String version:testValues){
			UnprocessableEntityException maybeException = null;
			Parameters params = parameters(part("version", new StringType(version)) );
			try {
				getClient().operation()
				.onInstance(minimalLibReference)
				.named("$draft")
				.withParameters(params)
				.returnResourceType(Bundle.class)
				.execute();
			} catch (UnprocessableEntityException e) {
				maybeException = e;
			}
			assertNotNull(maybeException);
		}
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
			assertTrue(actualErrorMessage.contains("Support for 'latest-from-tx-server' is not yet implemented."));
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
			booleanPart("latest-from-tx-server", false)
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
		String artifactCommentTarget= "Library/This-Library-Does-Not-Exist|1.0.0";
		Parameters params = parameters( 
			part("artifactCommentTarget", new CanonicalType(artifactCommentTarget))
		);
		UnprocessableEntityException maybeException = null;
		try {
			getClient().operation()
			.onInstance(specificationLibReference)
			.named("$approve")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();
		} catch (UnprocessableEntityException e) {
			maybeException = e;
		}
		assertNotNull(maybeException);
		assertTrue(maybeException.getMessage().contains("URL"));
		maybeException = null;
		artifactCommentTarget= "http://hl7.org/fhir/us/ecr/Library/SpecificationLibrary|this-version-is-wrong";
		params = parameters( 
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
			maybeException = e;
		}
		assertNotNull(maybeException);
		assertTrue(maybeException.getMessage().contains("version"));
	}
	@Test
	void approveOperation_should_respect_artifactAssessment_information_type_binding() {
		loadResource("ersd-active-library-example.json");
		String artifactCommentType = "this-type-does-not-exist";
		Parameters params = parameters( 
			part("artifactCommentType", artifactCommentType)
		);
		UnprocessableEntityException maybeException = null;
		try {
			getClient().operation()
			.onInstance(specificationLibReference)
			.named("$approve")
			.withParameters(params)
			.returnResourceType(Library.class)
			.execute();
		} catch (UnprocessableEntityException e) {
			maybeException = e;
		}
		assertNotNull(maybeException);
	}

	@Test
	void approveOperation_test() {
		loadResource("ersd-active-library-example.json");
		loadResource("practitioner-example-for-refs.json");
		String approvalDateString = "2022-12-12";
		DateType approvalDate = new DateType(DatatypeConverter.parseDate(approvalDateString).getTime());
		String artifactCommentType = "comment";
		String artifactCommentText = "comment text";
		String artifactCommentTarget= "http://hl7.org/fhir/us/ecr/Library/SpecificationLibrary|1.0.0";
		String artifactCommentReference="reference-valid-no-spaces";
		String artifactCommentUser= "Practitioner/sample-practitioner";
		Parameters params = parameters( 
			part("approvalDate", approvalDate),
			part("artifactCommentType", artifactCommentType),
			part("artifactCommentText", artifactCommentText),
			part("artifactCommentTarget", new CanonicalType(artifactCommentTarget)),
			part("artifactCommentReference", new CanonicalType(artifactCommentReference)),
			part("artifactCommentUser", new Reference(artifactCommentUser))
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
	}

}

