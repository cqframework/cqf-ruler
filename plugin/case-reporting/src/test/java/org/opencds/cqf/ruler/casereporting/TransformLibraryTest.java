package org.opencds.cqf.ruler.casereporting;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opencds.cqf.ruler.casereporting.r4.FhirResourceExists;
import org.opencds.cqf.ruler.casereporting.r4.ImportBundleProducer;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {
	TransformConfig.class }, properties = { "hapi.fhir.fhir_version=r4" })
public class TransformLibraryTest extends RestIntegrationTest {

	@Mock
	private TransformProperties transformProperties; // Your DAO to mock

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this); // Initializes mocks
	}

	/**
	 * @throws FhirResourceExists
	 */
	@Test
	public void testRootLibraryImport() throws FhirResourceExists {
		Bundle v2Bundle = (Bundle) loadResource("ersd-bundle-example.json");
		String targetedValueSetUrl = "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1506";
		String targetedPinnedValueSetVersion = "1.0.0";

		// Extract Root Library
		Library rootLibrary = extractRootLibrary(v2Bundle.getEntry());

		// Assert state before pre-import conformance
		assertFalse(rootLibrary.getRelatedArtifact()
			.stream()
			.anyMatch(i -> i.getResource().equals(targetedValueSetUrl + "|" + targetedPinnedValueSetVersion))
		);

		// Extract targeted ValueSet to check for import conformance
		Optional<Bundle.BundleEntryComponent> preImportBundleEntry = v2Bundle.getEntry().stream()
			.filter(i -> i.getFullUrl().equals(targetedValueSetUrl))
			.findFirst();

		ValueSet preImportValueSet = (ValueSet) preImportBundleEntry.get().getResource();

		assertFalse(preImportValueSet.getMeta().getProfile().containsAll(Arrays.asList(TransformProperties.leafValueSetVsmHostedProfile, TransformProperties.leafValueSetConditionProfile)));
		assertTrue(preImportValueSet.getUseContext().stream().anyMatch(i -> i.getCode().getCode().equals("focus") || i.getCode().getCode().equals("priority")));
		assertNotNull(preImportValueSet);

		// ensures that resources not found when doing checks
		when(transformProperties.search(any(), any())).thenThrow(new ResourceNotFoundException("Not Found"));

		List<Bundle.BundleEntryComponent> transactionBundleEntry = ImportBundleProducer.transformImportBundle(v2Bundle, transformProperties);

		Library updatedRootLibrary = extractRootLibrary(transactionBundleEntry);

		List<RelatedArtifact> ra = updatedRootLibrary
			.getRelatedArtifact()
			.stream()
			.filter(i -> i.getResource().equals(targetedValueSetUrl + "|" + targetedPinnedValueSetVersion))
			.collect(Collectors.toList());
		assertTrue(!ra.isEmpty());

		CodeableConcept conditionCodeableConcept = (CodeableConcept) ra.get(0).getExtension().get(0).getValue();
		assertEquals(conditionCodeableConcept.getText(), "Infection caused by Acanthamoeba (disorder)");

		CodeableConcept priorityCodeableConcept = (CodeableConcept) ra.get(0).getExtension().get(1).getValue();
		assertEquals(priorityCodeableConcept.getCoding().get(0).getCode(), "routine");

		// Extract targeted ValueSet to check for post-import conformance
		Optional<Bundle.BundleEntryComponent> postImportBundleEntry = transactionBundleEntry.stream()
			.filter(i -> i.getFullUrl().equals(targetedValueSetUrl))
			.findFirst();

		ValueSet postImportVs = (ValueSet) postImportBundleEntry.get().getResource();

		List<String> profileStrings = postImportVs.getMeta().getProfile().stream().map(PrimitiveType::getValueAsString).collect(Collectors.toList());
		assertTrue(profileStrings.containsAll(Arrays.asList(TransformProperties.leafValueSetVsmHostedProfile, TransformProperties.leafValueSetConditionProfile)));
		assertFalse(postImportVs.getUseContext().stream().anyMatch(i -> i.getCode().getCode().equals("focus") || i.getCode().getCode().equals("priority")));
		assertNotNull(postImportVs);
	}

	private Library extractRootLibrary(List<Bundle.BundleEntryComponent> bundleEntry) {
		Optional<IBaseResource> rootLibraryEntry = bundleEntry.stream()
			.filter(entry -> entry.hasResource() && ImportBundleProducer.isRootSpecificationLibrary(entry.getResource()))
			.findFirst()
			.map(Bundle.BundleEntryComponent::getResource);
		assertTrue(rootLibraryEntry.isPresent());
		return (Library) rootLibraryEntry.get();
	}
}
