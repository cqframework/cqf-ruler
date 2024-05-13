package org.opencds.cqf.ruler.casereporting.r4;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.MarkdownType;
import org.hl7.fhir.r4.model.UsageContext;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment;
import org.opencds.cqf.ruler.casereporting.CaseReportingConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = {CaseReportingConfig.class},
	properties = {"hapi.fhir.fhir_version=r4", "hapi.fhir.security.basic_auth.enabled=false", "hapi.fhir.cr.enabled=true"})
class CaseReportingOperationProviderTest extends RestIntegrationTest {
	private final String specificationLibReference = "Library/SpecificationLibrary";
	private final String minimalLibReference = "Library/SpecificationLibraryDraftVersion-1-0-0-23";
	private final List<String> badVersionList = Arrays.asList(
		"11asd1",
		"1.1.3.1.1",
		"1.|1.1.1",
		"1/.1.1.1",
		"-1.-1.2.1",
		"1.-1.2.1",
		"1.1.-2.1",
		"7.1..21",
		"1.2.1.3-draft",
		"1.2.3-draft",
		"3.2",
		"1.",
		"3.ad.2.",
		"",
		null
	);

	@Test
	void draftOperation_test() {
		loadTransaction("ersd-active-transaction-bundle-example.json");
		Library baseLib = getClient()
			.read()
			.resource(Library.class)
			.withId(specificationLibReference.split("/")[1])
			.execute();
		// Root Artifact must have approval date, releaseLabel and releaseDescription for this test
		assertTrue(baseLib.hasApprovalDate());
		assertTrue(baseLib.hasExtension(KnowledgeArtifactProcessor.releaseDescriptionUrl));
		assertTrue(baseLib.hasExtension(KnowledgeArtifactProcessor.releaseLabelUrl));
		assertTrue(baseLib.hasApprovalDate());
		String version = "1.0.1.23";
		String draftedVersion = version + "-draft";
		Parameters params = new Parameters();
		params.addParameter("version", version);
		Bundle returnedBundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$draft")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();

		assertNotNull(returnedBundle);
		Optional<Bundle.BundleEntryComponent> maybeLib = returnedBundle.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains("Library")).findAny();
		assertTrue(maybeLib.isPresent());
		Library lib = getClient().fetchResourceFromUrl(Library.class,maybeLib.get().getResponse().getLocation());
		assertNotNull(lib);
		assertTrue(lib.getStatus() == Enumerations.PublicationStatus.DRAFT);
		assertTrue(lib.getVersion().equals(draftedVersion));
		assertFalse(lib.hasApprovalDate());
		assertFalse(lib.hasExtension(KnowledgeArtifactProcessor.releaseDescriptionUrl));
		assertFalse(lib.hasExtension(KnowledgeArtifactProcessor.releaseLabelUrl));
		List<RelatedArtifact> relatedArtifacts = lib.getRelatedArtifact();
		assertTrue(!relatedArtifacts.isEmpty());
		forEachMetadataResource(returnedBundle.getEntry(), resource -> {
			List<RelatedArtifact> relatedArtifacts2 = AdapterFactory.forFhirVersion(FhirVersionEnum.R4).createKnowledgeArtifactAdapter(resource).getRelatedArtifact();
			if (relatedArtifacts2 != null && relatedArtifacts2.size() > 0) {
				for (RelatedArtifact relatedArtifact : relatedArtifacts2) {
					if (KnowledgeArtifactAdapter.checkIfRelatedArtifactIsOwned(relatedArtifact)) {
						assertTrue(Canonicals.getVersion(relatedArtifact.getResource()).equals(draftedVersion));
					}
				}
			}
		});
	}

	@Test
	void draftOperation_no_effectivePeriod_test() {
		loadTransaction("ersd-active-transaction-bundle-example.json");
		Library baseLib = getClient()
			.read()
			.resource(Library.class)
			.withId(specificationLibReference.split("/")[1])
			.execute();
		assertTrue(baseLib.hasEffectivePeriod());
		PlanDefinition planDef = getClient()
			.read()
			.resource(PlanDefinition.class)
			.withId("plandefinition-ersd-instance-example")
			.execute();
		assertTrue(planDef.hasEffectivePeriod());
		String version = "1.01.21.273";
		Parameters params = new Parameters();
		params.addParameter("version", version);
		Bundle returnedBundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$draft")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();

		forEachMetadataResource(returnedBundle.getEntry(), resource -> {
			var adapter = AdapterFactory.forFhirVersion(FhirVersionEnum.R4).createKnowledgeArtifactAdapter(resource);
			assertFalse(((Period)adapter.getEffectivePeriod()).hasStart() || ((Period)adapter.getEffectivePeriod()).hasEnd());
		});
	}

	@Test
	void draftOperation_version_conflict_test() {
		loadTransaction("ersd-active-transaction-bundle-example.json");
		loadResource("minimal-draft-to-test-version-conflict.json");
		Parameters params = new Parameters();
		params.addParameter("version", "1.0.0.23");
		String maybeException = null;
		try {
			getClient().operation()
				.onInstance(specificationLibReference)
				.named("$draft")
				.withParameters(params)
				.returnResourceType(Bundle.class)
				.execute();
		} catch (Exception e) {
			maybeException = e.getMessage();
		}
		assertNotNull(maybeException);
		assertTrue(maybeException.contains("already exists"));
	}

	@Test
	void draftOperation_cannot_create_draft_of_draft_test() {
		loadResource("minimal-draft-to-test-version-conflict.json");
		Parameters params = new Parameters();
		params.addParameter("version", "1.2.1.23");
		String maybeException = null;
		try {
			getClient().operation()
				.onInstance(minimalLibReference)
				.named("$draft")
				.withParameters(params)
				.returnResourceType(Bundle.class)
				.execute();
		} catch (Exception e) {
			maybeException = e.getMessage();
		}
		assertNotNull(maybeException);
		assertTrue(maybeException.contains("status of 'active'"));
	}

	@Test
	void draftOperation_wrong_id_test() {
		loadTransaction("ersd-draft-transaction-bundle-example.json");
		Parameters params = new Parameters();
		params.addParameter("version", "1.3.1.23");
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
		for(String version:badVersionList){
			UnprocessableEntityException maybeException = null;
			Parameters params = new Parameters();
			params.addParameter("version", new StringType(version));
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

	@Test
	void releaseResource_test() {
		loadTransaction("ersd-release-bundle.json");
		loadResource("artifactAssessment-search-parameter.json");
		String existingVersion = "1.2.3";
		String versionData = "1.2.7.23";
		Parameters params1 = new Parameters();
		params1.addParameter("version", new StringType(versionData));
		params1.addParameter("versionBehavior", new CodeType("default"));
		Bundle returnResource = getClient().operation()
			.onInstance("Library/ReleaseSpecificationLibrary")
			.named("$release")
			.withParameters(params1)
			.useHttpGet()
			.returnResourceType(Bundle.class)
			.execute();

		assertNotNull(returnResource);
		Optional<Bundle.BundleEntryComponent> maybeLib = returnResource.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains("Library")).findFirst();
		assertTrue(maybeLib.isPresent());
		var releasedLibrary = getClient().fetchResourceFromUrl(Library.class,maybeLib.get().getResponse().getLocation());
		// versionBehaviour == 'default' so version should be
		// existingVersion and not the new version provided in
		// the parameters
		assertTrue(releasedLibrary.getVersion().equals(existingVersion));
		var ersdTestArtifactDependencies = Arrays.asList(
			"http://ersd.aimsplatform.org/fhir/PlanDefinition/release-us-ecr-specification|" + existingVersion,
			"http://ersd.aimsplatform.org/fhir/Library/release-rctc|" + existingVersion,
			"http://ersd.aimsplatform.org/fhir/ValueSet/release-dxtc|" + existingVersion,
			"http://ersd.aimsplatform.org/fhir/ValueSet/release-ostc|" + existingVersion,
			"http://ersd.aimsplatform.org/fhir/ValueSet/release-lotc|" + existingVersion,
			"http://ersd.aimsplatform.org/fhir/ValueSet/release-lrtc|" + existingVersion,
			"http://ersd.aimsplatform.org/fhir/ValueSet/release-mrtc|" + existingVersion,
			"http://ersd.aimsplatform.org/fhir/ValueSet/release-sdtc|" + existingVersion,
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1063|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.360|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.120|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.362|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.528|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.408|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.409|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1469|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1866|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1906|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.480|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.481|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.761|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1223|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1182|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1181|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1184|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1601|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1600|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1603|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1602|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1082|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1439|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1436|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1435|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1446|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1438|2022-10-19",
			"http://notOwnedTest.com/Library/notOwnedRoot|0.1.1",
			"http://notOwnedTest.com/Library/notOwnedLeaf|0.1.1",
			"http://notOwnedTest.com/Library/notOwnedLeaf1|0.1.1",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-condition",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-encounter",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-medicationrequest",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-immunization",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-lab",
			"http://hl7.org/fhir/us/core/StructureDefinition/us-core-diagnosticreport-lab",
			"http://hl7.org/fhir/us/ecr/StructureDefinition/eicr-document-bundle",
			"http://hl7.org/fhir/StructureDefinition/ServiceRequest"
		);
		var ersdTestArtifactComponents = Arrays.asList(
			"http://ersd.aimsplatform.org/fhir/PlanDefinition/release-us-ecr-specification|" + existingVersion,
			"http://ersd.aimsplatform.org/fhir/Library/release-rctc|" + existingVersion,
			"http://notOwnedTest.com/Library/notOwnedRoot|0.1.1"
		);
		var dependenciesOnReleasedArtifact = releasedLibrary.getRelatedArtifact()
			.stream()
			.filter(ra -> ra.getType().equals(RelatedArtifact.RelatedArtifactType.DEPENDSON))
			.map(ra -> ra.getResource())
			.collect(Collectors.toList());
		var componentsOnReleasedArtifact = releasedLibrary.getRelatedArtifact()
			.stream()
			.filter(ra -> ra.getType().equals(RelatedArtifact.RelatedArtifactType.COMPOSEDOF))
			.map(ra -> ra.getResource())
			.collect(Collectors.toList());
		// check that the released artifact has all the required dependencies
		for(var dependency: ersdTestArtifactDependencies){
			assertTrue(dependenciesOnReleasedArtifact.contains(dependency));
		}
		// and components
		for(var component: ersdTestArtifactComponents){
			assertTrue(componentsOnReleasedArtifact.contains(component));
		}
		// has extra groupers and rctc dependencies
		assertEquals(ersdTestArtifactDependencies.size(), dependenciesOnReleasedArtifact.size());
		assertEquals(ersdTestArtifactComponents.size(), componentsOnReleasedArtifact.size());
	}

	@Test
	void releaseResource_force_version() {
		loadTransaction("ersd-small-approved-draft-bundle.json");
		loadResource("artifactAssessment-search-parameter.json");
		// Existing version should be "1.2.3";
		String newVersionToForce = "1.2.7.23";

		Parameters params = new Parameters();
		params.addParameter("version", new StringType(newVersionToForce));
		params.addParameter("versionBehavior", new CodeType("force"));


		Bundle returnResource = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$release")
			.withParameters(params)
			.useHttpGet()
			.returnResourceType(Bundle.class)
			.execute();

		assertNotNull(returnResource);
		Optional<Bundle.BundleEntryComponent> maybeLib = returnResource.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains(specificationLibReference)).findFirst();
		assertTrue(maybeLib.isPresent());
		Library releasedLibrary = getClient().fetchResourceFromUrl(Library.class,maybeLib.get().getResponse().getLocation());
		assertTrue(releasedLibrary.getVersion().equals(newVersionToForce));
	}

	@Test
	void releaseResource_require_non_experimental_error() {
		loadResource("artifactAssessment-search-parameter.json");
		// SpecificationLibrary - root is experimentalbut HAS experimental children
		loadTransaction("ersd-small-approved-draft-experimental-bundle.json");
		// SpecificationLibrary2 - root is NOT experimental but HAS experimental children
		loadTransaction("ersd-small-approved-draft-non-experimental-with-experimental-comp-bundle.json");

		Parameters params = new Parameters();
		params.addParameter("version", new StringType("1.2.3"));
		params.addParameter("versionBehavior", new CodeType("default"));
		params.addParameter("requireNonExperimental", new CodeType("error"));
		Exception notExpectingAnyException = null;
		// no Exception if root is experimental
		try {
			getClient().operation()
				.onInstance(specificationLibReference)
				.named("$release")
				.withParameters(params)
				.useHttpGet()
				.returnResourceType(Bundle.class)
				.execute();
		} catch (Exception e) {
			notExpectingAnyException = e;
		}
		assertEquals( null, notExpectingAnyException);

		UnprocessableEntityException nonExperimentalChildException = null;
		try {
			getClient().operation()
				.onInstance(specificationLibReference+"2")
				.named("$release")
				.withParameters(params)
				.useHttpGet()
				.returnResourceType(Bundle.class)
				.execute();
		} catch (UnprocessableEntityException e) {
			nonExperimentalChildException = e;
		}
		assertNotNull(nonExperimentalChildException);
		assertTrue(nonExperimentalChildException.getMessage().contains("not Experimental"));
	}

	@Test
	void releaseResource_require_non_experimental_warn() {
		loadResource("artifactAssessment-search-parameter.json");
		// SpecificationLibrary - root is experimentalbut HAS experimental children
		loadTransaction("ersd-small-approved-draft-experimental-bundle.json");
		// SpecificationLibrary2 - root is NOT experimental but HAS experimental children
		loadTransaction("ersd-small-approved-draft-non-experimental-with-experimental-comp-bundle.json");
		Appender<ILoggingEvent> myMockAppender = mock(Appender.class);
		List<String> warningMessages = new ArrayList<>();
		doAnswer(t -> {
			ILoggingEvent evt = (ILoggingEvent) t.getArguments()[0];
			// we only care about warning messages here
			if (evt.getLevel().equals(Level.WARN)) {
				// instead of appending to logs, we just add it to a list
				warningMessages.add(evt.getFormattedMessage());
			}
			return null;
		}).when(myMockAppender).doAppend(any());
		org.slf4j.Logger logger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		ch.qos.logback.classic.Logger myLoggerRoot = (ch.qos.logback.classic.Logger) logger;
		// add the mocked appender, make sure it is detached at the end
		myLoggerRoot.addAppender(myMockAppender);

		Parameters params = new Parameters();
		params.addParameter("version", new StringType("1.2.3"));
		params.addParameter("versionBehavior", new CodeType("default"));
		params.addParameter("requireNonExperimental", new CodeType("warn"));
		getClient().operation()
			.onInstance(specificationLibReference)
			.named("$release")
			.withParameters(params)
			.useHttpGet()
			.returnResourceType(Bundle.class)
			.execute();
		// no warning if the root is Experimental
		assertEquals(0, warningMessages.size());

		getClient().operation()
			.onInstance(specificationLibReference+"2")
			.named("$release")
			.withParameters(params)
			.useHttpGet()
			.returnResourceType(Bundle.class)
			.execute();
		// SHOULD warn if the root is not experimental
		assertTrue(warningMessages.stream().anyMatch(message -> message.contains("http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.7")));
		assertTrue(warningMessages.stream().anyMatch(message -> message.contains("http://ersd.aimsplatform.org/fhir/Library/rctc2")));
		// cleanup
		myLoggerRoot.detachAppender(myMockAppender);
	}

	@Test
	void releaseResource_propagate_effective_period() {
		loadTransaction("ersd-small-approved-draft-no-child-effective-period.json");
		loadResource("artifactAssessment-search-parameter.json");
		var effectivePeriodToPropagate = "2020-12-11";

		var params = new Parameters();
		params.addParameter("version", new StringType("1.2.7"));
		params.addParameter("versionBehavior", new CodeType("default"));


		var returnResource = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$release")
			.withParameters(params)
			.useHttpGet()
			.returnResourceType(Bundle.class)
			.execute();

		assertNotNull(returnResource);
		forEachMetadataResource(returnResource.getEntry(), resource -> {
			assertNotNull(resource);
			if(!resource.getClass().getSimpleName().equals("ValueSet")){
				var adapter = AdapterFactory.forFhirVersion(FhirVersionEnum.R4).createKnowledgeArtifactAdapter(resource);
				assertTrue(((Period)adapter.getEffectivePeriod()).hasStart());
				var start = ((Period)adapter.getEffectivePeriod()).getStart();
				var calendar = new GregorianCalendar();
				calendar.setTime(start);
				int year = calendar.get(Calendar.YEAR);
				int month = calendar.get(Calendar.MONTH) + 1;
				int day = calendar.get(Calendar.DAY_OF_MONTH);
				var startString = year + "-" + month + "-" + day;
				assertTrue(startString.equals(effectivePeriodToPropagate));
			}
		});
	}

	private void forEachMetadataResource(List<Bundle.BundleEntryComponent> entries, Consumer<MetadataResource> callback) {
		entries.stream()
			.map(entry -> entry.getResponse().getLocation())
			.map(location -> {
				switch (location.split("/")[0]) {
					case "ActivityDefinition":
						return getClient().fetchResourceFromUrl(ActivityDefinition.class, location);
					case "Library":
						return getClient().fetchResourceFromUrl(Library.class, location);
					case "Measure":
						return getClient().fetchResourceFromUrl(Measure.class, location);
					case "PlanDefinition":
						return getClient().fetchResourceFromUrl(PlanDefinition.class, location);
					case "ValueSet":
						return getClient().fetchResourceFromUrl(ValueSet.class, location);
					default:
						return null;
				}
			})
			.forEach(callback);
	}

	@Test
	void releaseResource_latestFromTx_NotSupported_test() {
		loadTransaction("ersd-small-approved-draft-bundle.json");
		String actualErrorMessage = "";

		Parameters params = new Parameters();
		params.addParameter("version", "1.2.3");
		params.addParameter("versionBehavior", new CodeType("default"));
		params.addParameter("latestFromTxServer", new BooleanType(true));

		try {
			getClient().operation()
				.onInstance(specificationLibReference)
				.named("$release")
				.withParameters(params)
				.useHttpGet()
				.returnResourceType(Bundle.class)
				.execute();
		} catch (Exception e) {
			actualErrorMessage = e.getMessage();
		}
		assertTrue(actualErrorMessage.contains("not yet implemented"));
	}

	@Test
	void release_missing_approvalDate_validation_test() {
		loadTransaction("ersd-release-missing-approvalDate-validation-bundle.json");
		String versionData = "1.2.3.23";
		String actualErrorMessage = "";

		Parameters params1 = new Parameters();
		params1.addParameter("version", versionData);
		params1.addParameter("versionBehavior", new CodeType("default"));

		try {
			getClient().operation()
				.onInstance("Library/ReleaseSpecificationLibrary")
				.named("$release")
				.withParameters(params1)
				.useHttpGet()
				.returnResourceType(Bundle.class)
				.execute();
		} catch (Exception e) {
			actualErrorMessage = e.getMessage();
		}
		assertTrue(actualErrorMessage.contains("approvalDate"));
	}

	@Test
	void release_version_format_test() {
		loadTransaction("ersd-small-approved-draft-bundle.json");
		loadResource("artifactAssessment-search-parameter.json");
		for(String version:badVersionList){
			UnprocessableEntityException maybeException = null;
			Parameters params = new Parameters();
			params.addParameter("version", new StringType(version));
			params.addParameter("versionBehavior", new CodeType("force"));

			try {
				getClient().operation()
					.onInstance(specificationLibReference)
					.named("$release")
					.withParameters(params)
					.returnResourceType(Bundle.class)
					.execute();
			} catch (UnprocessableEntityException e) {
				maybeException = e;
			}
			assertNotNull(maybeException);
		}
	}

	@Test
	void release_releaseLabel_test() {
		loadTransaction("ersd-small-approved-draft-bundle.json");
		loadResource("artifactAssessment-search-parameter.json");
		String releaseLabel = "release label test";
		Parameters params = new Parameters();
		params.addParameter("releaseLabel", new StringType(releaseLabel));
		params.addParameter("version", "1.2.3.23");
		params.addParameter("versionBehavior", new StringType("default"));

		Bundle returnResource = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$release")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();
		assertNotNull(returnResource);
		Optional<Bundle.BundleEntryComponent> maybeLib = returnResource.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains(specificationLibReference)).findFirst();
		assertTrue(maybeLib.isPresent());
		Library releasedLibrary = getClient().fetchResourceFromUrl(Library.class,maybeLib.get().getResponse().getLocation());
		Optional<Extension> maybeReleaseLabel = releasedLibrary.getExtension().stream().filter(ext -> ext.getUrl().equals(KnowledgeArtifactProcessor.releaseLabelUrl)).findFirst();
		assertTrue(maybeReleaseLabel.isPresent());
		assertTrue(((StringType) maybeReleaseLabel.get().getValue()).getValue().equals(releaseLabel));
	}

	@Test
	void release_version_active_test() {
		loadTransaction("ersd-small-active-bundle.json");
		loadResource("artifactAssessment-search-parameter.json");
		UnprocessableEntityException maybeException = null;
		Parameters params = new Parameters();
		params.addParameter("version", new StringType("1.2.3"));
		params.addParameter("versionBehavior", new CodeType("force"));

		try {
			getClient().operation()
				.onInstance(specificationLibReference)
				.named("$release")
				.withParameters(params)
				.returnResourceType(Bundle.class)
				.execute();
		} catch (UnprocessableEntityException e) {
			maybeException = e;
		}
		assertNotNull(maybeException);
	}

	@Test
	void release_resource_not_found_test() {
		ResourceNotFoundException maybeException = null;
		Parameters params = new Parameters();
		params.addParameter("version", new StringType("1.2.3"));
		params.addParameter("versionBehavior", new CodeType("force"));

		try {
			getClient().operation()
				.onInstance("Library/this-resource-does-not-exist")
				.named("$release")
				.withParameters(params)
				.returnResourceType(Bundle.class)
				.execute();
		} catch (ResourceNotFoundException e) {
			maybeException = e;
		}
		assertNotNull(maybeException);
	}

	@Test
	void release_versionBehaviour_format_test() {
		loadTransaction("ersd-small-approved-draft-bundle.json");
		loadResource("artifactAssessment-search-parameter.json");
		List<String> badVersionBehaviors = Arrays.asList(
			"not-a-valid-option",
			null
		);
		for(String versionBehaviour:badVersionBehaviors){
			UnprocessableEntityException maybeException = null;
			Parameters params = new Parameters();
			params.addParameter("version", new StringType("1.2.3"));
			params.addParameter("versionBehavior", new CodeType(versionBehaviour));

			try {
				getClient().operation()
					.onInstance(specificationLibReference)
					.named("$release")
					.withParameters(params)
					.returnResourceType(Bundle.class)
					.execute();
			} catch (UnprocessableEntityException e) {
				maybeException = e;
			}
			assertNotNull(maybeException);
		}
	}

	@Test
	void release_preserve_extensions() {
		loadTransaction("ersd-small-approved-draft-bundle.json");
		loadResource("artifactAssessment-search-parameter.json");
		Parameters params = new Parameters();
		params.addParameter("version", new StringType("1.2.3.23"));
		params.addParameter("versionBehavior", new StringType("default"));

		Bundle returnResource =	getClient().operation()
			.onInstance(specificationLibReference)
			.named("$release")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();
		assertNotNull(returnResource);
		Optional<Bundle.BundleEntryComponent> maybeLib = returnResource.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains(specificationLibReference)).findFirst();
		assertTrue(maybeLib.isPresent());
		Library releasedLibrary = getClient().fetchResourceFromUrl(Library.class,maybeLib.get().getResponse().getLocation());
		Optional<RelatedArtifact> maybeRelatedArtifactWithPriorityExtension = releasedLibrary.getRelatedArtifact().stream().filter(ra -> ra.getExtensionByUrl(KnowledgeArtifactProcessor.valueSetPriorityUrl) != null).findAny();
		Optional<RelatedArtifact> maybeRelatedArtifactWithUseContextExtension = releasedLibrary.getRelatedArtifact().stream().filter(ra -> ra.getExtensionByUrl(KnowledgeArtifactProcessor.valueSetConditionUrl) != null).findAny();
		assertTrue(maybeRelatedArtifactWithUseContextExtension.isPresent());
		assertTrue(maybeRelatedArtifactWithUseContextExtension.get().getResource().equals("http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6|20210526"));
		assertTrue(maybeRelatedArtifactWithPriorityExtension.isPresent());
		assertTrue(maybeRelatedArtifactWithPriorityExtension.get().getResource().equals("http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6|20210526"));
		Extension priority = maybeRelatedArtifactWithUseContextExtension.get().getExtensionByUrl(KnowledgeArtifactProcessor.valueSetPriorityUrl);
		assertTrue(((CodeableConcept) priority.getValue()).getCoding().get(0).getCode().equals("emergent"));
		Extension condition = maybeRelatedArtifactWithUseContextExtension.get().getExtensionByUrl(KnowledgeArtifactProcessor.valueSetConditionUrl);
		assertTrue(((CodeableConcept) condition.getValue()).getCoding().get(0).getCode().equals("49649001"));
	}

	@Test
	void release_test_condition_missing() {
		loadTransaction("ersd-small-approved-draft-missing-condition.json");
		loadResource("artifactAssessment-search-parameter.json");
		Parameters params = new Parameters();
		params.addParameter("version", new StringType("1.2.3.23"));
		params.addParameter("versionBehavior", new StringType("default"));

		UnprocessableEntityException noConditionExtension = null;
		try {
			getClient().operation()
				.onInstance(specificationLibReference)
				.named("$release")
				.withParameters(params)
				.returnResourceType(Bundle.class)
				.execute();
		} catch (UnprocessableEntityException e) {
			// TODO: handle exception
			noConditionExtension = e;
		}
		assertNotNull(noConditionExtension);
		assertTrue(noConditionExtension.getMessage().contains("Missing condition"));
	}

	@Test
	void release_test_artifactComment_updated() {
		loadTransaction("ersd-release-missing-approvalDate-validation-bundle.json");
		loadResource("artifactAssessment-search-parameter.json");
		String versionData = "1.2.3";
		Parameters approveParams = new Parameters();
		approveParams.addParameter("approvalDate", new DateType(new Date(), TemporalPrecisionEnum.DAY));

		Bundle approvedBundle = getClient().operation()
			.onInstance("Library/ReleaseSpecificationLibrary")
			.named("$approve")
			.withParameters(approveParams)
			.useHttpGet()
			.returnResourceType(Bundle.class)
			.execute();
		Optional<Bundle.BundleEntryComponent> maybeArtifactAssessment = approvedBundle.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains("Basic")).findAny();
		assertTrue(maybeArtifactAssessment.isPresent());
		var artifactAssessment = getClient().fetchResourceFromUrl(ArtifactAssessment.class,maybeArtifactAssessment.get().getResponse().getLocation());
		assertTrue(artifactAssessment.getDerivedFromContentRelatedArtifact().get().getResourceElement().getValue().equals("http://ersd.aimsplatform.org/fhir/Library/ReleaseSpecificationLibrary|1.2.3-draft"));
		Parameters releaseParams = new Parameters();
		releaseParams.addParameter("version", versionData);
		releaseParams.addParameter("versionBehavior", new CodeType("default"));

		Bundle releasedBundle = getClient().operation()
			.onInstance("Library/ReleaseSpecificationLibrary")
			.named("$release")
			.withParameters(releaseParams)
			.useHttpGet()
			.returnResourceType(Bundle.class)
			.execute();
		Optional<Bundle.BundleEntryComponent> maybeReleasedArtifactAssessment = releasedBundle.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains("Basic")).findAny();
		assertTrue(maybeReleasedArtifactAssessment.isPresent());
		ArtifactAssessment releasedArtifactAssessment = getClient().fetchResourceFromUrl(ArtifactAssessment.class,maybeReleasedArtifactAssessment.get().getResponse().getLocation());
		assertTrue(releasedArtifactAssessment.getDerivedFromContentRelatedArtifact().get().getResourceElement().getValue().equals("http://ersd.aimsplatform.org/fhir/Library/ReleaseSpecificationLibrary|1.2.3"));
	}

	@Test
	void reviseOperation_active_test() {
		Library library = (Library) loadResource("ersd-active-library-example.json");
		library.setName("NewSpecificationLibrary");
		String actualErrorMessage = "";
		Parameters params = new Parameters();
		params.addParameter().setName("resource").setResource(library);
		try {
			getClient().operation()
				.onServer()
				.named("$revise")
				.withParameters(params)
				.returnResourceType(Library.class)
				.execute();
		} catch (Exception e) {
			actualErrorMessage = e.getMessage();
		}
		assertTrue(actualErrorMessage.contains("Current resource status is 'ACTIVE'. Only resources with status of 'draft' can be revised."));
	}

	@Test
	void reviseOperation_draft_test() {
		String newResourceName = "NewSpecificationLibrary";
		Library library = (Library) loadResource("ersd-draft-library-example.json");
		library.setName(newResourceName);
		String errorMessage = "";
		Parameters params = new Parameters();
		params.addParameter().setName("resource").setResource(library);
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
		String artifactAssessmentTarget= "Library/This-Library-Does-Not-Exist|1.0.0";
		Parameters params = new Parameters();
		params.addParameter("artifactAssessmentTarget", new CanonicalType(artifactAssessmentTarget));

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
		artifactAssessmentTarget= "http://hl7.org/fhir/us/ecr/Library/SpecificationLibrary|this-version-is-wrong";
		params = new Parameters();
		params.addParameter("artifactAssessmentTarget", new CanonicalType(artifactAssessmentTarget));

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
		String artifactAssessmentType = "this-type-does-not-exist";
		Parameters params = new Parameters();
		params.addParameter("artifactAssessmentType", artifactAssessmentType);

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
	}

	@Test
	void approveOperation_test() {
		loadResource("ersd-active-library-example.json");
		loadResource("practitioner-example-for-refs.json");
		var today = new Date();
		// get today's date in the form "2023-05-11"
		var approvalDate = new DateType(today, TemporalPrecisionEnum.DAY);
		var artifactAssessmentType = "comment";
		var artifactAssessmentSummary = "comment text";
		var artifactAssessmentTarget= "http://hl7.org/fhir/us/ecr/Library/SpecificationLibrary|1.0.0";
		var artifactAssessmentRelatedArtifact="reference-valid-no-spaces";
		var artifactAssessmentAuthor= "Practitioner/sample-practitioner";
		var params = new Parameters();
		params.addParameter("approvalDate", approvalDate);
		params.addParameter("artifactAssessmentType", new CodeType(artifactAssessmentType));
		params.addParameter("artifactAssessmentSummary", new MarkdownType(artifactAssessmentSummary));
		params.addParameter("artifactAssessmentTarget", new CanonicalType(artifactAssessmentTarget));
		params.addParameter("artifactAssessmentRelatedArtifact", new CanonicalType(artifactAssessmentRelatedArtifact));
		params.addParameter("artifactAssessmentAuthor", new Reference(artifactAssessmentAuthor));

		Bundle returnedResource = null;
		returnedResource = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$approve")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();

		assertNotNull(returnedResource);
		var lib = getClient().fetchResourceFromUrl(Library.class, specificationLibReference);
		assertNotNull(lib);
		// Ensure Approval Date matches input parameter
		assertTrue(lib.getApprovalDateElement().asStringValue().equals(approvalDate.asStringValue()));
		// match Libray.date to Library.meta.lastUpdated precision before comparing
		lib.getMeta().getLastUpdatedElement().setPrecision(TemporalPrecisionEnum.DAY);
		// Library.date matches the meta.lastUpdated value
		assertTrue(lib.getDateElement().asStringValue().equals(lib.getMeta().getLastUpdatedElement().asStringValue()));
		// Ensure that approval date is NOT before Library.date (see $release)
		assertFalse(lib.getApprovalDate().before(lib.getDate()));
		// ArtifactAssessment is saved as type Basic, update when we change to OperationOutcome
		// Get the reference from BundleEntry.response.location
		Optional<Bundle.BundleEntryComponent> maybeArtifactAssessment = returnedResource.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains("Basic")).findAny();
		assertTrue(maybeArtifactAssessment.isPresent());
		var artifactAssessment = getClient().fetchResourceFromUrl(ArtifactAssessment.class,maybeArtifactAssessment.get().getResponse().getLocation());
		assertNotNull(artifactAssessment);
		assertTrue(artifactAssessment.isValidArtifactComment());
		assertTrue(artifactAssessment.checkArtifactCommentParams(
			artifactAssessmentType,
			artifactAssessmentSummary,
			specificationLibReference,
			artifactAssessmentRelatedArtifact,
			artifactAssessmentTarget,
			artifactAssessmentAuthor
		));
	}

	@Test
	void packageOperation_should_fail_non_matching_capability() {
		loadTransaction("ersd-active-transaction-capabilities-bundle.json");
		List<String> capabilities = Arrays.asList(
			"computable",
			"publishable",
			"executable"
		);
		// the library contains all three capabilities
		// so we should get an error when trying with
		// any one capability
		for (String capability : capabilities) {
			Parameters params = new Parameters();
			params.addParameter("capability", capability);

			PreconditionFailedException maybeException = null;
			try {
				getClient().operation()
					.onInstance(specificationLibReference)
					.named("$package")
					.withParameters(params)
					.returnResourceType(Bundle.class)
					.execute();
			} catch (PreconditionFailedException e) {
				maybeException = e;
			}
			assertNotNull(maybeException);
		}
		Parameters allParams = new Parameters();
		allParams.addParameter("capability", "computable");
		allParams.addParameter("capability", "publishable");
		allParams.addParameter("capability", "executable");

		Bundle packaged = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$package")
			.withParameters(allParams)
			.returnResourceType(Bundle.class)
			.execute();
		// no error when running the operation with all
		// three capabilities
		assertNotNull(packaged);
	}

	@Test
	void packageOperation_should_apply_check_force_canonicalVersions() {
		loadTransaction("ersd-active-transaction-no-versions.json");
		String versionToUpdateTo = "1.3.1.23";
		Parameters params = new Parameters();
		params.addParameter("artifactVersion", new CanonicalType("http://to-add-missing-version/PlanDefinition/us-ecr-specification|" + versionToUpdateTo));
		params.addParameter("artifactVersion", new CanonicalType("http://to-add-missing-version/ValueSet/dxtc|" + versionToUpdateTo));

		Bundle updatedCanonicalVersionPackage = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$package")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();
		List<MetadataResource> updatedResources = updatedCanonicalVersionPackage.getEntry().stream()
			.map(entry -> (MetadataResource) entry.getResource())
			.filter(resource -> resource.getUrl().contains("to-add-missing-version"))
			.collect(Collectors.toList());
		assertTrue(updatedResources.size() == 2);
		for (MetadataResource updatedResource: updatedResources) {
			assertTrue(updatedResource.getVersion().equals(versionToUpdateTo));
		}
		params = new Parameters();
		params.addParameter("checkArtifactVersion", new CanonicalType("http://to-check-version/Library/SpecificationLibrary|1.3.1"));

		String correctCheckVersion = "2022-10-19";
		PreconditionFailedException checkCanonicalThrewError = null;
		try {
			getClient().operation()
				.onInstance(specificationLibReference)
				.named("$package")
				.withParameters(params)
				.returnResourceType(Bundle.class)
				.execute();
		} catch (PreconditionFailedException e) {
			checkCanonicalThrewError = e;
		}
		assertNotNull(checkCanonicalThrewError);
		params = new Parameters();
		params.addParameter("checkArtifactVersion", new CanonicalType("http://to-check-version/Library/SpecificationLibrary|" + correctCheckVersion));

		Bundle noErrorCheckCanonicalPackage = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$package")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();
		Optional<MetadataResource> checkedVersionResource = noErrorCheckCanonicalPackage.getEntry().stream()
			.map(entry -> (MetadataResource) entry.getResource())
			.filter(resource -> resource.getUrl().contains("to-check-version"))
			.findFirst();
		assertTrue(checkedVersionResource.isPresent());
		assertTrue(checkedVersionResource.get().getVersion().equals(correctCheckVersion));
		String versionToForceTo = "1.1.9.23";
		params = new Parameters();
		params.addParameter("forceArtifactVersion", new CanonicalType("http://to-force-version/Library/rctc|" + versionToForceTo));

		Bundle forcedVersionPackage = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$package")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();
		Optional<MetadataResource> forcedVersionResource = forcedVersionPackage.getEntry().stream()
			.map(entry -> (MetadataResource) entry.getResource())
			.filter(resource -> resource.getUrl().contains("to-force-version"))
			.findFirst();
		assertTrue(forcedVersionResource.isPresent());
		assertTrue(forcedVersionResource.get().getVersion().equals(versionToForceTo));

	}

	@Test
	void packageOperation_should_respect_count_offset() {
		loadTransaction("ersd-small-active-bundle.json");
		Parameters countZeroParams = new Parameters();
		countZeroParams.addParameter("count", new IntegerType(0));
		Bundle countZeroBundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$package")
			.withParameters(countZeroParams)
			.returnResourceType(Bundle.class)
			.execute();
		// when count = 0 only show the total
		assertTrue(countZeroBundle.getEntry().size() == 0);
		assertTrue(countZeroBundle.getTotal() == 6);
		Parameters count2Params = new Parameters();
		count2Params.addParameter("count", new IntegerType(2));

		Bundle count2Bundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$package")
			.withParameters(count2Params)
			.returnResourceType(Bundle.class)
			.execute();
		assertTrue(count2Bundle.getEntry().size() == 2);
		Parameters count2Offset2Params = new Parameters();
		count2Offset2Params.addParameter("count", new IntegerType(2));
		count2Offset2Params.addParameter("offset", new IntegerType(2));

		Bundle count2Offset2Bundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$package")
			.withParameters(count2Offset2Params)
			.returnResourceType(Bundle.class)
			.execute();
		assertTrue(count2Offset2Bundle.getEntry().size() == 2);
		Parameters offset4Params = new Parameters();
		offset4Params.addParameter("offset", new IntegerType(4));

		Bundle offset4Bundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$package")
			.withParameters(offset4Params)
			.returnResourceType(Bundle.class)
			.execute();
		assertTrue(offset4Bundle.getEntry().size() == (countZeroBundle.getTotal() - 4));
		assertTrue(offset4Bundle.getType() == Bundle.BundleType.COLLECTION);
		assertTrue(offset4Bundle.hasTotal() == false);
		Parameters offsetMaxParams = new Parameters();
		offsetMaxParams.addParameter("offset", new IntegerType(countZeroBundle.getTotal()));

		Bundle offsetMaxBundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$package")
			.withParameters(offsetMaxParams)
			.returnResourceType(Bundle.class)
			.execute();
		assertTrue(offsetMaxBundle.getEntry().size() == 0);
		Parameters offsetMaxRandomCountParams = new Parameters();
		offsetMaxRandomCountParams.addParameter("offset", new IntegerType(countZeroBundle.getTotal()));
		offsetMaxRandomCountParams.addParameter("count", new IntegerType(ThreadLocalRandom.current().nextInt(3, 20)));

		Bundle offsetMaxRandomCountBundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$package")
			.withParameters(offsetMaxRandomCountParams)
			.returnResourceType(Bundle.class)
			.execute();
		assertTrue(offsetMaxRandomCountBundle.getEntry().size() == 0);
	}

	@Test
	void packageOperation_different_bundle_types() {
		loadTransaction("ersd-small-active-bundle.json");
		Parameters countZeroParams = new Parameters();
		countZeroParams.addParameter("count", new IntegerType(0));

		Bundle countZeroBundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$package")
			.withParameters(countZeroParams)
			.returnResourceType(Bundle.class)
			.execute();
		assertTrue(countZeroBundle.getType() == Bundle.BundleType.SEARCHSET);
		Parameters countSevenParams = new Parameters();
		countSevenParams.addParameter("count", new IntegerType(7));

		Bundle countSevenBundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$package")
			.withParameters(countSevenParams)
			.returnResourceType(Bundle.class)
			.execute();
		assertTrue(countSevenBundle.getType() == Bundle.BundleType.TRANSACTION);
		Parameters countFourParams = new Parameters();
		countFourParams.addParameter("count", new IntegerType(4));

		Bundle countFourBundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$package")
			.withParameters(countFourParams)
			.returnResourceType(Bundle.class)
			.execute();
		assertTrue(countFourBundle.getType() == Bundle.BundleType.COLLECTION);
		// these assertions test for Bundle base profile conformance when type = collection
		assertFalse(countFourBundle.getEntry().stream().anyMatch(entry -> entry.hasRequest()));
		assertFalse(countFourBundle.hasTotal());
		Parameters offsetOneParams = new Parameters();
		offsetOneParams.addParameter("offset", new IntegerType(1));

		Bundle offsetOneBundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$package")
			.withParameters(offsetOneParams)
			.returnResourceType(Bundle.class)
			.execute();
		assertTrue(offsetOneBundle.getType() == Bundle.BundleType.COLLECTION);
		// these assertions test for Bundle base profile conformance when type = collection
		assertFalse(offsetOneBundle.getEntry().stream().anyMatch(entry -> entry.hasRequest()));
		assertFalse(offsetOneBundle.hasTotal());

		Parameters countOneOffsetOneParams = new Parameters();
		countOneOffsetOneParams.addParameter("count", new IntegerType(1));
		countOneOffsetOneParams.addParameter("offset", new IntegerType(1));

		Bundle countOneOffsetOneBundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$package")
			.withParameters(countOneOffsetOneParams)
			.returnResourceType(Bundle.class)
			.execute();
		assertTrue(countOneOffsetOneBundle.getType() == Bundle.BundleType.COLLECTION);
		// these assertions test for Bundle base profile conformance when type = collection
		assertFalse(countOneOffsetOneBundle.getEntry().stream().anyMatch(entry -> entry.hasRequest()));
		assertFalse(countOneOffsetOneBundle.hasTotal());
	}

	@Test
	void packageOperation_should_conditionally_create() {
		loadTransaction("ersd-small-active-bundle.json");
		Parameters emptyParams = new Parameters();
		Bundle packagedBundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$package")
			.withParameters(emptyParams)
			.returnResourceType(Bundle.class)
			.execute();
		for (Bundle.BundleEntryComponent component : packagedBundle.getEntry()) {
			String ifNoneExist = component.getRequest().getIfNoneExist();
			String url = ((MetadataResource) component.getResource()).getUrl();
			String version = ((MetadataResource) component.getResource()).getVersion();
			assertTrue(ifNoneExist.equals("url="+url+"&version="+version));
		}
	}

	@Test
	void packageOperation_should_be_aware_of_valueset_priority_extension() {
		loadTransaction("ersd-small-active-bundle.json");
		Parameters emptyParams = new Parameters();
		Bundle packagedBundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$package")
			.withParameters(emptyParams)
			.returnResourceType(Bundle.class)
			.execute();
		Optional<ValueSet> shouldBeUpdatedToEmergent = packagedBundle.getEntry().stream()
			.filter(entry -> entry.getResource().getResourceType().equals(ResourceType.ValueSet))
			.map(entry -> (ValueSet) entry.getResource())
			.filter(vs -> vs.getUrl().equals("http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6") && vs.getVersion().equals("20210526"))
			.findFirst();
		assertTrue(shouldBeUpdatedToEmergent.isPresent());
		Optional<UsageContext> priority = shouldBeUpdatedToEmergent.get().getUseContext().stream()
			.filter(useContext -> useContext.getCode().getSystem().equals(KnowledgeArtifactProcessor.usPhContextTypeUrl) && useContext.getCode().getCode().equals(KnowledgeArtifactProcessor.valueSetPriorityCode))
			.findFirst();
		assertTrue(priority.isPresent());
		assertTrue(((CodeableConcept) priority.get().getValue()).getCoding().get(0).getCode().equals("emergent"));
		assertTrue(((CodeableConcept) priority.get().getValue()).getCoding().get(0).getSystem().equals(KnowledgeArtifactProcessor.contextUrl));

		Optional<ValueSet> shouldBeUpdatedToRoutine = packagedBundle.getEntry().stream()
			.filter(entry -> entry.getResource().getResourceType().equals(ResourceType.ValueSet))
			.map(entry -> (ValueSet) entry.getResource())
			.filter(vs -> vs.getUrl().equals("http://cts.nlm.nih.gov/fhir/ValueSet/123-this-will-be-routine") && vs.getVersion().equals("20210526"))
			.findFirst();
		assertTrue(shouldBeUpdatedToRoutine.isPresent());
		Optional<UsageContext> priority2 = shouldBeUpdatedToRoutine.get().getUseContext().stream()
			.filter(useContext -> useContext.getCode().getSystem().equals(KnowledgeArtifactProcessor.usPhContextTypeUrl) && useContext.getCode().getCode().equals(KnowledgeArtifactProcessor.valueSetPriorityCode))
			.findFirst();
		assertTrue(priority2.isPresent());
		assertTrue(((CodeableConcept) priority2.get().getValue()).getCoding().get(0).getCode().equals("routine"));
		assertTrue(((CodeableConcept) priority2.get().getValue()).getCoding().get(0).getSystem().equals(KnowledgeArtifactProcessor.contextUrl));
	}

	@Test
	void packageOperation_should_be_aware_of_useContext_extension() {
		loadTransaction("ersd-small-active-bundle.json");
		Parameters emptyParams = new Parameters();
		Bundle packagedBundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$package")
			.withParameters(emptyParams)
			.returnResourceType(Bundle.class)
			.execute();
		Optional<ValueSet> shouldHaveFocusSetToNewValue = packagedBundle.getEntry().stream()
			.filter(entry -> entry.getResource().getResourceType().equals(ResourceType.ValueSet))
			.map(entry -> (ValueSet) entry.getResource())
			.filter(vs -> vs.getUrl().equals("http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6") && vs.getVersion().equals("20210526"))
			.findFirst();
		assertTrue(shouldHaveFocusSetToNewValue.isPresent());
		Optional<UsageContext> focus = shouldHaveFocusSetToNewValue.get().getUseContext().stream()
			.filter(useContext -> useContext.getCode().getSystem().equals(KnowledgeArtifactProcessor.contextTypeUrl) && useContext.getCode().getCode().equals(KnowledgeArtifactProcessor.valueSetConditionCode))
			.findFirst();
		assertTrue(focus.isPresent());
		assertTrue(((CodeableConcept) focus.get().getValue()).getCoding().get(0).getCode().equals("49649001"));
		assertTrue(((CodeableConcept) focus.get().getValue()).getCoding().get(0).getSystem().equals("http://snomed.info/sct"));
	}

	@Test
	void packageOperation_should_respect_include() {
		loadTransaction("ersd-small-active-bundle.json");
		Map<String, List<String>> includeOptions = new HashMap<String,List<String>>();
		includeOptions.put("artifact",Arrays.asList("http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary"));
		includeOptions.put("canonical",Arrays.asList(
			"http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary",
			"http://ersd.aimsplatform.org/fhir/PlanDefinition/us-ecr-specification",
			"http://ersd.aimsplatform.org/fhir/Library/rctc",
			"http://ersd.aimsplatform.org/fhir/ValueSet/dxtc",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6",
			"http://cts.nlm.nih.gov/fhir/ValueSet/123-this-will-be-routine"
		));
		includeOptions.put("knowledge",Arrays.asList(
			"http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary",
			"http://ersd.aimsplatform.org/fhir/PlanDefinition/us-ecr-specification",
			"http://ersd.aimsplatform.org/fhir/Library/rctc"
		));
		includeOptions.put("terminology",Arrays.asList(
			"http://ersd.aimsplatform.org/fhir/ValueSet/dxtc",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6",
			"http://cts.nlm.nih.gov/fhir/ValueSet/123-this-will-be-routine"
		));
		includeOptions.put("conformance",Arrays.asList());
		includeOptions.put("extensions",Arrays.asList());
		includeOptions.put("profiles",Arrays.asList());
		includeOptions.put("tests",Arrays.asList());
		includeOptions.put("examples",Arrays.asList());
		for (Map.Entry<String, List<String>> includedTypeURLs : includeOptions.entrySet()) {
			Parameters params = new Parameters();
			params.addParameter("include", includedTypeURLs.getKey());

			Bundle packaged = getClient().operation()
				.onInstance(specificationLibReference)
				.named("$package")
				.withParameters(params)
				.returnResourceType(Bundle.class)
				.execute();
			List<MetadataResource> resources = packaged.getEntry().stream()
				.map(entry -> (MetadataResource) entry.getResource())
				.collect(Collectors.toList());
			for (MetadataResource resource: resources) {
				Boolean noExtraResourcesReturned = includedTypeURLs.getValue().stream()
					.anyMatch(url -> url.equals(resource.getUrl()));
				assertTrue(noExtraResourcesReturned);
			}
			for (String url: includedTypeURLs.getValue()) {
				Boolean expectedResourceReturned = resources.stream()
					.anyMatch(resource -> resource.getUrl().equals(url));
				assertTrue(expectedResourceReturned);
			}
		}
	}

	@Test
	void packageOperation_big_bundle() {
		Bundle loadedBundle = (Bundle) loadTransaction("ersd-active-transaction-bundle-example.json");
		Bundle packagedBundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$package")
			.withParameters(new Parameters())
			.returnResourceType(Bundle.class)
			.execute();
		assertTrue(packagedBundle.getEntry().size() == loadedBundle.getEntry().size());
	}

	@Disabled
	@Test
		// We need to disable this as it requires VSAC credentials to expand Value Sets
	void packageOperation_expansion() {
		loadTransaction("small-expansion-bundle.json");
		Parameters emptyParams = new Parameters();
		Bundle packagedBundle = getClient().operation()
			.onInstance("Library/SmallSpecificationLibrary")
			.named("$package")
			.withParameters(emptyParams)
			.returnResourceType(Bundle.class)
			.execute();

		List<ValueSet> leafValueSets = packagedBundle.getEntry().stream()
			.filter(entry -> entry.getResource().getResourceType() == ResourceType.ValueSet)
			.map(entry -> ((ValueSet) entry.getResource()))
			.filter(valueSet -> !valueSet.hasCompose() || (valueSet.hasCompose() && valueSet.getCompose().getIncludeFirstRep().getValueSet().size() == 0))
			.collect(Collectors.toList());

		// Ensure expansion is populated and each code has correct version for all leaf value sets
		leafValueSets.forEach(valueSet -> assertNotNull(valueSet.getExpansion()));
		leafValueSets.stream().allMatch(vs -> vs.getExpansion().getContains().stream().allMatch(c -> c.getVersion().equals("http://snomed.info/sct/731000124108/version/20230901")));
	}

	@Test
	void packageOperation_naive_expansion() {
		loadTransaction("small-naive-expansion-bundle.json");
		Parameters emptyParams = new Parameters();
		Bundle packagedBundle = getClient().operation()
			.onInstance("Library/SmallSpecificationLibrary")
			.named("$package")
			.withParameters(emptyParams)
			.returnResourceType(Bundle.class)
			.execute();

		List<ValueSet> leafValueSets = packagedBundle.getEntry().stream()
			.filter(entry -> entry.getResource().getResourceType() == ResourceType.ValueSet)
			.map(entry -> ((ValueSet) entry.getResource()))
			.filter(valueSet -> !valueSet.hasCompose() || (valueSet.hasCompose() && valueSet.getCompose().getIncludeFirstRep().getValueSet().size() == 0))
			.collect(Collectors.toList());

		// Ensure expansion is populated for all leaf value sets
		leafValueSets.forEach(valueSet -> assertNotNull(valueSet.getExpansion()));
	}

	@Test
	void package_test_condition_missing() {
		loadTransaction("ersd-small-approved-draft-missing-condition.json");
		loadResource("artifactAssessment-search-parameter.json");
		UnprocessableEntityException noConditionExtension = null;
		try {
			getClient().operation()
				.onInstance(specificationLibReference)
				.named("$package")
				.withNoParameters(Parameters.class)
				.returnResourceType(Bundle.class)
				.execute();
		} catch (UnprocessableEntityException e) {
			// TODO: handle exception
			noConditionExtension = e;
		}
		assertNotNull(noConditionExtension);
		assertTrue(noConditionExtension.getMessage().contains("Missing condition"));
	}

	@Test
	void validateOperation() {
		var ersdExampleSpecBundle = (Bundle) loadResource("ersd-bundle-example.json");
		var specBundleParams = new Parameters();
		specBundleParams.addParameter().setName("resource").setResource(ersdExampleSpecBundle);
		var specBundleOutcome = getClient().operation()
			.onServer()
			.named("$validate")
			.withParameters(specBundleParams)
			.returnResourceType(OperationOutcome.class)
			.execute();
		var specBundleValidationErrors = specBundleOutcome.getIssue().stream().filter((issue) -> issue.getSeverity() == OperationOutcome.IssueSeverity.ERROR || issue.getSeverity() == OperationOutcome.IssueSeverity.FATAL).collect(Collectors.toList());
		assertEquals(2, specBundleValidationErrors.size());
		// expect errors for Variable extension which bubble up and invalidate the PlanDefinition slice
		assertTrue(specBundleValidationErrors.get(0).getDiagnostics().contains("slicePlanDefinition"));
		assertTrue(specBundleValidationErrors.get(1).getDiagnostics().contains("variable"));
		var ersdExampleSupplementalBundle = (Bundle) loadResource("ersd-supplemental-bundle-example.json");
		var supplementalBundleParams = new Parameters();
		supplementalBundleParams.addParameter().setName("resource").setResource(ersdExampleSupplementalBundle);

		var supplementalBundleOutcome = getClient().operation()
			.onServer()
			.named("$validate")
			.withParameters(supplementalBundleParams)
			.returnResourceType(OperationOutcome.class)
			.execute();
		var supplementalBundleErrors = supplementalBundleOutcome.getIssue().stream().filter((issue) -> issue.getSeverity() == OperationOutcome.IssueSeverity.ERROR || issue.getSeverity() == OperationOutcome.IssueSeverity.FATAL).collect(Collectors.toList());
		assertTrue(supplementalBundleErrors.size() == 0);

		var validationErrorLibrary = (Library) loadResource("ersd-active-library-us-ph-validation-failure-example.json");
		var validationFailedParams = new Parameters();
		validationFailedParams.addParameter().setName("resource").setResource(validationErrorLibrary);
		var failedValidationOutcome = getClient().operation()
			.onServer()
			.named("$validate")
			.withParameters(validationFailedParams)
			.returnResourceType(OperationOutcome.class)
			.execute();
		var invalidLibraryErrors = failedValidationOutcome.getIssue().stream().filter((issue) -> issue.getSeverity() == OperationOutcome.IssueSeverity.ERROR || issue.getSeverity() == OperationOutcome.IssueSeverity.FATAL).collect(Collectors.toList());
		assertTrue(invalidLibraryErrors.size() == 5);

		var noResourceParams = new Parameters();
		UnprocessableEntityException noResourceException = null;
		try {
			getClient().operation()
				.onServer()
				.named("$validate")
				.withParameters(noResourceParams)
				.returnResourceType(OperationOutcome.class)
				.execute();
		} catch (UnprocessableEntityException e) {
			noResourceException = e;
		}
		assertNotNull(noResourceException);
		assertTrue(noResourceException.getMessage().contains("resource must be provided"));
	}

	@Test
	void validateOperationUnqualifiedRelatedArtifact() {
		Bundle ersdExampleSpecBundleUnqualifiedPlanDefinition = (Bundle) loadResource("ersd-library-validation-failure-unqualified-plandefinition-bundle.json");
		Parameters validationFailedUnqualifiedPlanDefinitionParams = new Parameters();
		validationFailedUnqualifiedPlanDefinitionParams.addParameter().setName("resource").setResource( ersdExampleSpecBundleUnqualifiedPlanDefinition);
		OperationOutcome failedValidationUnqualifiedPlanDefinitionOutcome = getClient().operation()
			.onServer()
			.named("$validate")
			.withParameters(validationFailedUnqualifiedPlanDefinitionParams)
			.returnResourceType(OperationOutcome.class)
			.execute();
		boolean missingPlanDefinitionSliceErrorExists = failedValidationUnqualifiedPlanDefinitionOutcome.getIssue().stream()
			.anyMatch((issue) -> issue.getDiagnostics().contains("Library.relatedArtifact:slicePlanDefinition: minimum required = 1, but only found 0 (from http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-specification-library|2.1.0)"));
		assertTrue(missingPlanDefinitionSliceErrorExists);
	}

	@Test
	void validatePackageOutput() {
		loadTransaction("ersd-active-transaction-bundle-example.json");
		Bundle packagedBundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$package")
			.withParameters(new Parameters())
			.returnResourceType(Bundle.class)
			.execute();
		assertTrue(packagedBundle.getEntry().size() == 37);
		Parameters packagedBundleParams = new Parameters();
		packagedBundleParams.addParameter().setName("resource").setResource( packagedBundle);

		OperationOutcome packagedBundleOutcome = getClient().operation()
			.onServer()
			.named("$validate")
			.withParameters(packagedBundleParams)
			.returnResourceType(OperationOutcome.class)
			.execute();
		List<OperationOutcome.OperationOutcomeIssueComponent> errors = packagedBundleOutcome.getIssue().stream().filter((issue) -> issue.getSeverity() == OperationOutcome.IssueSeverity.ERROR || issue.getSeverity() == OperationOutcome.IssueSeverity.FATAL).collect(Collectors.toList());
		assertTrue(errors.size() == 4);
		// expect errors for Variable extension which bubble up and invalidate the PlanDefinition slice
		assertTrue(errors.get(0).getDiagnostics().contains("slicePlanDefinition"));
		assertTrue(errors.get(1).getDiagnostics().contains("'depends-on' but must be 'composed-of'"));
		assertTrue(errors.get(2).getDiagnostics().contains("variable"));
		assertTrue(errors.get(3).getDiagnostics().contains("variable"));
	}

	@Test
	void update_rckms_valueset_updates_Libraries() {
		// setup
		ValueSet conditions = (ValueSet) loadResource("rckms-condition-codes.json");
		IdType id = new IdType(conditions.getId());
		// just remove the history part or it causes problems later
		conditions.setId(id.getResourceType()+"/"+id.getIdPart());
		loadResource("manifest-code-search-parameter.json");
		loadTransaction("libraries-with-conditions-bundle.json");

		// if we update 2 synonyms
		updateSynonym(conditions, "767146004", "testUpdate1");
		updateSynonym(conditions, "49649001", "testUpdate2");
		getClient().update().resource(conditions).execute();
		String valueSetUrl = "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1506|1.0.0";
		Library lib1 = (Library)getClient().read().resource("Library").withId("lib1").execute();
		Library lib2 = (Library)getClient().read().resource("Library").withId("lib2").execute();
		List<Extension> condition1 = getConditionExtensionForValueSet(valueSetUrl,lib1);
		assertTrue(condition1.size() > 0);
		// see a change in both libraries
		assertTrue(((CodeableConcept)condition1.get(0).getValue()).getText().equals("testUpdate1"));
		List<Extension> condition2 = getConditionExtensionForValueSet(valueSetUrl,lib2);
		assertTrue(condition2.size() > 0);
		// see a change in both libraries
		assertTrue(((CodeableConcept)condition2.get(0).getValue()).getText().equals("testUpdate2"));

	}

	private void updateSynonym(ValueSet vs, String code, String newText) {
		vs.getCompose()
			.getInclude().get(0)
			.getConcept()
			.stream().filter(c -> c.getCode().equals(code)).findFirst().get()
			.getDesignation().get(0)
			.setValue(newText);
	}

	private List<Extension> getConditionExtensionForValueSet(String valueSetUrl, Library library) {
		return library.getRelatedArtifact().stream()
			.filter(ra -> ra.hasResource() && ra.getResource().equals(valueSetUrl))
			.map(ra -> ra.getExtensionsByUrl(KnowledgeArtifactProcessor.valueSetConditionUrl))
			.flatMap(exts -> exts.stream())
			.collect(Collectors.toList());
	}

	void basic_artifact_diff() {
		loadTransaction("ersd-small-active-bundle.json");
		Bundle bundle = (Bundle) loadTransaction("small-drafted-ersd-bundle.json");
		Optional<Bundle.BundleEntryComponent> maybeLib = bundle.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains("Library")).findFirst();
		loadResource("artifactAssessment-search-parameter.json");
		Parameters diffParams = new Parameters();
		diffParams.addParameter("source", specificationLibReference);
		diffParams.addParameter("target", maybeLib.get().getResponse().getLocation());

		Parameters returnedParams = getClient().operation()
			.onServer()
			.named("artifact-diff")
			.withParameters(diffParams)
			.returnResourceType(Parameters.class)
			.execute();
		List<Parameters.ParametersParameterComponent> parameters = returnedParams.getParameter();
		List<Parameters.ParametersParameterComponent> libraryReplaceOperations = getOperationsByType(parameters, "replace");
		List<Parameters.ParametersParameterComponent> libraryInsertOperations = getOperationsByType(parameters, "insert");
		List<Parameters.ParametersParameterComponent> libraryDeleteOperations = getOperationsByType(parameters, "delete");
		List<String> libraryReplacedPaths = List.of(
			"Library.id",
			"Library.version",
			"Library.status",
			"Library.relatedArtifact[0].resource", // planDefinition version update
			"Library.relatedArtifact[1].resource", // RCTC lib version update
			"Library.relatedArtifact[4].resource"  // DXTC Grouper version update
		);
		List<String> libraryDeletedPaths = List.of(
			"Library.relatedArtifact[5]"  // deleted DXTC leaf VS
		);
		List<String> libraryInsertedPaths = List.of(
			"Library.relatedArtifact"  // new DXTC leaf VS
		);
		for (Parameters.ParametersParameterComponent param: libraryReplaceOperations) {
			String path = param.getPart().stream()
				.filter(part -> part.getName().equals("path"))
				.map(part -> ((StringType)part.getValue()).getValue())
				.findFirst().get();
			assertTrue(libraryReplacedPaths.contains(path));
		}
		for (Parameters.ParametersParameterComponent param: libraryInsertOperations) {
			String path = param.getPart().stream()
				.filter(part -> part.getName().equals("path"))
				.map(part -> ((StringType)part.getValue()).getValue())
				.findFirst().get();
			assertTrue(libraryInsertedPaths.contains(path));
		}
		for (Parameters.ParametersParameterComponent param: libraryDeleteOperations) {
			String path = param.getPart().stream()
				.filter(part -> part.getName().equals("path"))
				.map(part -> ((StringType)part.getValue()).getValue())
				.findFirst().get();
			assertTrue(libraryDeletedPaths.contains(path));
		}
		List<String> libraryNestedChanges = List.of(
			"http://ersd.aimsplatform.org/fhir/PlanDefinition/us-ecr-specification",
			"http://ersd.aimsplatform.org/fhir/Library/rctc",
			"http://notOwnedTest.com/Library/notOwnedRoot", // will be empty / unable to retrieve
			"http://cts.nlm.nih.gov/fhir/ValueSet/123-this-will-be-routine",
			"http://ersd.aimsplatform.org/fhir/ValueSet/dxtc",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.163", // the new VS added to the DXTC
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6" // the VS deleted from the DXTC
		);
		libraryNestedChanges.stream()
			.forEach(nestedChangeUrl -> {
				Parameters.ParametersParameterComponent nestedChange = returnedParams.getParameter(nestedChangeUrl);
				assertNotNull(nestedChange);
				if (nestedChange.hasResource()) {
					assertTrue(nestedChange.getResource() instanceof Parameters);
				}
			});
	}

	@Test
	void artifact_diff_compare_computable() {
		loadTransaction("ersd-small-active-bundle.json");
		Bundle bundle = (Bundle) loadTransaction("small-drafted-ersd-bundle.json");
		Optional<Bundle.BundleEntryComponent> maybeLib = bundle.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains("Library")).findFirst();
		loadResource("artifactAssessment-search-parameter.json");
		Parameters diffParams = new Parameters();
		diffParams.addParameter("source", specificationLibReference);
		diffParams.addParameter("target", maybeLib.get().getResponse().getLocation());
		diffParams.addParameter("compareComputable", new BooleanType(true));

		Parameters returnedParams = getClient().operation()
			.onServer()
			.named("$artifact-diff")
			.withParameters(diffParams)
			.returnResourceType(Parameters.class)
			.execute();
		List<Parameters> nestedChanges = returnedParams.getParameter().stream()
			.filter(p -> !p.getName().equals("operation"))
			.map(p -> (Parameters)p.getResource())
			.filter(p -> p != null)
			.collect(Collectors.toList());
		assertTrue(nestedChanges.size() == 3);
		Parameters grouperChanges = returnedParams.getParameter().stream().filter(p -> p.getName().contains("/dxtc")).map(p-> (Parameters)p.getResource()).findFirst().get();
		List<Parameters.ParametersParameterComponent> deleteOperations = getOperationsByType(grouperChanges.getParameter(), "delete");
		List<Parameters.ParametersParameterComponent> insertOperations = getOperationsByType(grouperChanges.getParameter(), "insert");
		// delete the old leaf
		assertTrue(deleteOperations.size() == 1);
		// there aren't actually 2 operations here
		assertTrue(insertOperations.size() == 2);
		String path1 = insertOperations.get(0).getPart().stream().filter(p -> p.getName().equals("path")).map(p -> ((StringType)p.getValue()).getValue()).findFirst().get();
		String path2 = insertOperations.get(1).getPart().stream().filter(p -> p.getName().equals("path")).map(p -> ((StringType)p.getValue()).getValue()).findFirst().get();
		// insert the new leaf; adding a node takes multiple operations if
		// the thing being added isn't a defined complex FHIR type
		assertTrue(path1.equals("ValueSet.compose.include"));
		assertTrue(path2.equals("ValueSet.compose.include[1].valueSet"));
	}

	@Test
	void artifact_diff_compare_executable() {
		loadTransaction("ersd-small-active-bundle.json");
		Bundle bundle = (Bundle) loadTransaction("small-drafted-ersd-bundle.json");
		Optional<Bundle.BundleEntryComponent> maybeLib = bundle.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains("Library")).findFirst();
		loadResource("artifactAssessment-search-parameter.json");
		Parameters diffParams = new Parameters();
		diffParams.addParameter("source", specificationLibReference);
		diffParams.addParameter("target", maybeLib.get().getResponse().getLocation());
		diffParams.addParameter("compareExecutable", new BooleanType(true));

		Parameters returnedParams = getClient().operation()
			.onServer()
			.named("$artifact-diff")
			.withParameters(diffParams)
			.returnResourceType(Parameters.class)
			.execute();
		List<Parameters> nestedChanges = returnedParams.getParameter().stream()
			.filter(p -> !p.getName().equals("operation"))
			.map(p -> (Parameters)p.getResource())
			.filter(p -> p != null)
			.collect(Collectors.toList());
		assertTrue(nestedChanges.size() == 3);
		Parameters grouperChanges = returnedParams.getParameter().stream().filter(p -> p.getName().contains("/dxtc")).map(p-> (Parameters)p.getResource()).findFirst().get();
		List<Parameters.ParametersParameterComponent> deleteOperations = getOperationsByType(grouperChanges.getParameter(), "delete");
		List<Parameters.ParametersParameterComponent> insertOperations = getOperationsByType(grouperChanges.getParameter(), "insert");
		// old codes removed
		assertTrue(deleteOperations.size() == 23);
		// new codes added
		assertTrue(insertOperations.size() == 32);
	}

	private Parameters createChangelogSetup() {
		loadTransaction("small-diff-bundle.json");
		var bundle = (Bundle) loadTransaction("small-dxtc-modified-diff-bundle.json");
		var maybeLib = bundle.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains("Library")).findFirst();
		Parameters diffParams = new Parameters();
		diffParams.addParameter("source", specificationLibReference);
		diffParams.addParameter("target", maybeLib.get().getResponse().getLocation());
		return diffParams;
	}

	@Test
	void create_changelog_pages() {
		// check that the correct pages are created
		var returnedBinary = getClient().operation()
			.onServer()
			.named("$create-changelog")
			.withParameters(createChangelogSetup())
			.returnResourceType(Binary.class)
			.execute();
		assertNotNull(returnedBinary);
		byte[] decodedBytes = Base64.getDecoder().decode(returnedBinary.getContentAsBase64());
		String decodedString = new String(decodedBytes);
		ObjectMapper mapper = new ObjectMapper();
		var pageURLS = List.of(
			"http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary",
			"http://ersd.aimsplatform.org/fhir/PlanDefinition/us-ecr-specification",
			"http://ersd.aimsplatform.org/fhir/Library/rctc",
			"http://ersd.aimsplatform.org/fhir/ValueSet/dxtc"
		);
		Exception expectNoException = null;
		try {
			var node = mapper.readTree(decodedString);
			assertTrue(node.get("pages").isArray());
			var pages = node.get("pages");
			assertEquals(pages.size(), pageURLS.size());
			for (final var url : pageURLS) {
				var pageExists = StreamSupport.stream(pages.spliterator(), false)
					.anyMatch(page -> page.get("url").asText().equals(url));
				assertTrue(pageExists);
			}
		} catch (Exception e) {
			// TODO: handle exception
			expectNoException = e;
		}
		assertNull(expectNoException);
	}

	@Test
	void create_changelog_codes() {
		// check that the correct leaf VS codes are generated and have
		// the correct memberOID values
		var returnedBinary = getClient().operation()
			.onServer()
			.named("$create-changelog")
			.withParameters(createChangelogSetup())
			.returnResourceType(Binary.class)
			.execute();
		assertNotNull(returnedBinary);
		byte[] decodedBytes = Base64.getDecoder().decode(returnedBinary.getContentAsBase64());
		String decodedString = new String(decodedBytes);
		ObjectMapper mapper = new ObjectMapper();
		Exception expectNoException = null;
		Map<String, codeAndOperation> oldCodes = new HashMap<String, codeAndOperation>();
		oldCodes.put("772155008", new codeAndOperation("123-this-will-be-routine",null));
		oldCodes.put("1086051000119107", new codeAndOperation("2.16.840.1.113762.1.4.1146.6","delete"));
		oldCodes.put("1086061000119109", new codeAndOperation("2.16.840.1.113762.1.4.1146.6","delete"));
		oldCodes.put("1086071000119103", new codeAndOperation("2.16.840.1.113762.1.4.1146.6","delete"));
		oldCodes.put("1090211000119102", new codeAndOperation("2.16.840.1.113762.1.4.1146.6","delete"));
		oldCodes.put("129667001", new codeAndOperation("2.16.840.1.113762.1.4.1146.6","delete"));
		oldCodes.put("13596001", new codeAndOperation("2.16.840.1.113762.1.4.1146.6","delete"));
		oldCodes.put("15682004", new codeAndOperation("2.16.840.1.113762.1.4.1146.6","delete"));
		oldCodes.put("186347006", new codeAndOperation("2.16.840.1.113762.1.4.1146.6","delete"));
		oldCodes.put("18901009", new codeAndOperation("2.16.840.1.113762.1.4.1146.6","delete"));
		oldCodes.put("194945009", new codeAndOperation("2.16.840.1.113762.1.4.1146.6","delete"));
		oldCodes.put("230596007", new codeAndOperation("2.16.840.1.113762.1.4.1146.6","delete"));
		oldCodes.put("240422004", new codeAndOperation("2.16.840.1.113762.1.4.1146.6","delete"));
		oldCodes.put("26117009", new codeAndOperation("2.16.840.1.113762.1.4.1146.6","delete"));
		oldCodes.put("276197005", new codeAndOperation("2.16.840.1.113762.1.4.1146.6","delete"));
		oldCodes.put("276197005", new codeAndOperation("2.16.840.1.113762.1.4.1146.6","delete"));
		oldCodes.put("3419005", new codeAndOperation("2.16.840.1.113762.1.4.1146.6","delete"));
		oldCodes.put("397428000", new codeAndOperation("2.16.840.1.113762.1.4.1146.6","delete"));
		oldCodes.put("397430003", new codeAndOperation("2.16.840.1.113762.1.4.1146.6","delete"));
		oldCodes.put("48278001", new codeAndOperation("2.16.840.1.113762.1.4.1146.6","delete"));
		oldCodes.put("50215002", new codeAndOperation("2.16.840.1.113762.1.4.1146.6","delete"));
		oldCodes.put("715659006", new codeAndOperation("2.16.840.1.113762.1.4.1146.6","delete"));
		oldCodes.put("75589004", new codeAndOperation("2.16.840.1.113762.1.4.1146.6","delete"));
		oldCodes.put("7773002", new codeAndOperation("2.16.840.1.113762.1.4.1146.6","delete"));
		oldCodes.put("789005009", new codeAndOperation("2.16.840.1.113762.1.4.1146.6","delete"));
		var newCodes = Map.of(
			"772155008", new codeAndOperation("123-this-will-be-routine",null),
			"1193749009", new codeAndOperation("2.16.840.1.113762.1.4.1146.163","insert"),
			"1193750009", new codeAndOperation("2.16.840.1.113762.1.4.1146.163","insert"),
			"240349003", new codeAndOperation("2.16.840.1.113762.1.4.1146.163","insert"),
			"240350003", new codeAndOperation("2.16.840.1.113762.1.4.1146.163","insert"),
			"240351004", new codeAndOperation("2.16.840.1.113762.1.4.1146.163","insert"),
			"447282003", new codeAndOperation("2.16.840.1.113762.1.4.1146.163","insert"),
			"63650001", new codeAndOperation("2.16.840.1.113762.1.4.1146.163","insert"),
			"81020007", new codeAndOperation("2.16.840.1.113762.1.4.1146.163","insert")
		);
		try {
			var node = mapper.readTree(decodedString);
			assertTrue(node.get("pages").isArray());
			var pages = node.get("pages");
			for (final var page : pages) {
				if (Canonicals.getResourceType(page.get("url").asText()).equals("ValueSet")) {
					assertTrue(page.get("oldData").get("codes").isArray());
					for (final var code: page.get("oldData").get("codes")) {
						codeAndOperation expectedOldCode = oldCodes.get(code.get("code").asText());
						assertNotNull(expectedOldCode);
						if (expectedOldCode.operation != null) {
							assertEquals(expectedOldCode.operation, code.get("operation").get("type").asText());
							assertEquals(expectedOldCode.code, code.get("memberOid").asText());
						}
					}
					assertTrue(page.get("newData").get("codes").isArray());
					for (final var code: page.get("newData").get("codes")) {
						codeAndOperation expectedNewCode = newCodes.get(code.get("code").asText());
						assertNotNull(expectedNewCode);
						if (expectedNewCode.operation != null) {
							assertEquals(expectedNewCode.operation, code.get("operation").get("type").asText());
							assertEquals(expectedNewCode.code, code.get("memberOid").asText());
						}
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			expectNoException = e;
		}
		assertNull(expectNoException);
	}

	@Test
	void create_changelog_conditions_and_priorities() {
		// check that the conditions and priorities are correctly
		// extracted and have the correct operations
		var returnedBinary = getClient().operation()
			.onServer()
			.named("$create-changelog")
			.withParameters(createChangelogSetup())
			.returnResourceType(Binary.class)
			.execute();
		assertNotNull(returnedBinary);
		Map<String,Map<String,List<codeAndOperation>>> oldLeafsAndConditions = Map.of(
			"123-this-will-be-routine", Map.of(
				"conditions", List.of(
					new codeAndOperation("49649001", null),
					new codeAndOperation("000000000", "delete")
				),
				"priority", new ArrayList<>()
			),
			"2.16.840.1.113762.1.4.1146.6", Map.of(
				"conditions", List.of(
					new codeAndOperation("49649001", null),
					new codeAndOperation("767146004", null)
				),
				"priority", List.of(
					new codeAndOperation("emergent", null)
				)
			)
		);
		Map<String,Map<String,List<codeAndOperation>>> newLeafsAndConditions = Map.of(
			"123-this-will-be-routine", Map.of(
				"conditions", List.of(
					new codeAndOperation("767146004", "insert"),
					new codeAndOperation("49649001", null)
				),
				"priority", new ArrayList<>()
			),
			"2.16.840.1.113762.1.4.1146.163", Map.of(
				"conditions", List.of(
					new codeAndOperation("123123123", null)
				),
				"priority", List.of(
					new codeAndOperation("emergent", null)
				)
			)
		);
		ObjectMapper mapper = new ObjectMapper();
		Exception expectNoException = null;
		try {
			var node = mapper.readTree(new String(Base64.getDecoder().decode(returnedBinary.getContentAsBase64())));
			assertTrue(node.get("pages").isArray());
			var pages = node.get("pages");
			for (final var page : pages) {
				if (Canonicals.getResourceType(page.get("url").asText()).equals("ValueSet")) {
					assertTrue(page.get("oldData").get("leafValuesets").isArray());
					for (final var leaf: page.get("oldData").get("leafValuesets")) {
						assertTrue(leaf.get("conditions").isArray());
						List<codeAndOperation> expectedConditions = oldLeafsAndConditions.get(leaf.get("memberOid").asText()).get("conditions");
						assertTrue(expectedConditions.size() > 0);
						for (final var condition: leaf.get("conditions")) {
							Optional<codeAndOperation> conditionInList = expectedConditions.stream().filter(c -> c.code != null && c.code.equals(condition.get("code").asText())).findAny();
							assertTrue(conditionInList.isPresent());
							if (conditionInList.get().operation != null) {
								assertEquals(conditionInList.get().operation, condition.get("operation").get("type").asText());
							}
						}
					}
					assertTrue(page.get("newData").get("leafValuesets").isArray());
					for (final var leaf: page.get("newData").get("leafValuesets")) {
						assertTrue(leaf.get("conditions").isArray());
						List<codeAndOperation> expectedConditions = newLeafsAndConditions.get(leaf.get("memberOid").asText()).get("conditions");
						assertTrue(expectedConditions.size() > 0);
						for (final var condition: leaf.get("conditions")) {
							Optional<codeAndOperation> conditionInList = expectedConditions.stream().filter(c -> c.code != null && c.code.equals(condition.get("code").asText())).findAny();
							assertTrue(conditionInList.isPresent());
							if (conditionInList.get().operation != null) {
								assertEquals(conditionInList.get().operation, condition.get("operation").get("type").asText());
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			expectNoException = e;
		}
		assertNull(expectNoException);
	}

	@Test
	void create_changelog_grouped_leaf() {
		// check that all the grouped leaf valuesets exist
		var returnedBinary = getClient().operation()
			.onServer()
			.named("$create-changelog")
			.withParameters(createChangelogSetup())
			.returnResourceType(Binary.class)
			.execute();
		assertNotNull(returnedBinary);
		ObjectMapper mapper = new ObjectMapper();
		Exception expectNoException = null;
		var oldLeafs = Map.of(
			"123-this-will-be-routine", "",
			"2.16.840.1.113762.1.4.1146.6", "delete"
		);
		var newLeafs = Map.of(
			"123-this-will-be-routine", "",
			"2.16.840.1.113762.1.4.1146.163", "insert"
		);
		try {
			var node = mapper.readTree(new String(Base64.getDecoder().decode(returnedBinary.getContentAsBase64())));
			assertTrue(node.get("pages").isArray());
			var pages = node.get("pages");
			for (final var page : pages) {
				if (Canonicals.getResourceType(page.get("url").asText()).equals("ValueSet")) {
					assertTrue(page.get("oldData").get("leafValuesets").isArray());
					for (final var leaf: page.get("oldData").get("leafValuesets")) {
						var expectedLeaf = oldLeafs.get(leaf.get("memberOid").asText());
						assertNotNull(expectedLeaf);
						if (!expectedLeaf.isBlank()) {
							assertEquals(expectedLeaf, leaf.get("operation").get("type").asText());
						}
					}
					assertTrue(page.get("newData").get("leafValuesets").isArray());
					for (final var leaf: page.get("newData").get("leafValuesets")) {
						var expectedLeaf = newLeafs.get(leaf.get("memberOid").asText());
						assertNotNull(expectedLeaf);
						if (!expectedLeaf.isBlank()) {
							assertEquals(expectedLeaf, leaf.get("operation").get("type").asText());
						}
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			expectNoException = e;
		}
		assertNull(expectNoException);
	}

	private List<Parameters.ParametersParameterComponent> getOperationsByType(List<Parameters.ParametersParameterComponent> parameters, String type) {
		return parameters.stream().filter(
			p -> p.getName().equals("operation")
				&& p.getPart().stream().anyMatch(part -> part.getName().equals("type") && ((CodeType)part.getValue()).getCode().equals(type))
		).collect(Collectors.toList());
	}

	private static class codeAndOperation {
		public String code;
		public String operation;
		codeAndOperation(String code, String operation) {
			this.code = code;
			this.operation = operation;
		}
	}
}
