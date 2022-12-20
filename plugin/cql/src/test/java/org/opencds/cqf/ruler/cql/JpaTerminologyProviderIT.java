package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import org.apache.commons.collections4.IterableUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = { JpaTerminologyProviderIT.class },
	properties = { "hapi.fhir.fhir_version=r4" })
class JpaTerminologyProviderIT extends RestIntegrationTest {

	@Autowired
	DaoConfig daoConfig;
	@Autowired
	private JpaTerminologyProviderFactory jpaTerminologyProviderFactory;
	private JpaTerminologyProvider terminologyProvider;

	@BeforeAll
	void setup() {
		SystemRequestDetails requestDetails = new SystemRequestDetails();
		requestDetails.setFhirContext(getFhirContext());
		requestDetails.setFhirServerBase(getServerBase());
		terminologyProvider = jpaTerminologyProviderFactory.create(requestDetails);
	}

	@Test
	void testExpandFhirCodeSystem() {
		Iterable<org.opencds.cqf.cql.engine.runtime.Code> expandResult = getExpansion(
			"valueset-filter-comparator.json",
			"http://hl7.org/fhir/us/cqfmeasures/ValueSet/value-filter-comparator",
			"3.0.0");
		assertNotNull(expandResult);
		assertEquals(7, IterableUtils.size(expandResult));
	}

	/*
		Possible issue with the following codes having the same code, but different display values:
		/[HPF]
		/[LPF]
		[beth'U]
		[pptr]
		[todd'U]
		[iU]
		{Ehrlich'U}/100.g
		The ValueSet composition includes 1364 codes, but the expansion returns 1357 codes
			- display values are not present in the expansion
	*/
	@Test
	void testExpandUnitsOfMeasureCodeSystemMoreThan1000() {
		daoConfig.setMaximumExpansionSize(Integer.MAX_VALUE);
		Iterable<org.opencds.cqf.cql.engine.runtime.Code> expandResult = getExpansion(
			"valueset-ucum-common.json", "http://hl7.org/fhir/ValueSet/ucum-common", "1.0.0");
		assertEquals(1357, IterableUtils.size(expandResult));
	}

	@Test
	void testPreExpandedRxNormCodeSystemMoreThan1000() {
		daoConfig.setMaximumExpansionSize(Integer.MAX_VALUE);
		Iterable<org.opencds.cqf.cql.engine.runtime.Code> expandResult = getExpansion(
			"valueset-opioid-analgesics-with-ambulatory-misuse-potential.json",
			"http://fhir.org/guides/cdc/opioid-cds/ValueSet/opioid-analgesics-with-ambulatory-misuse-potential",
			"0.1.1");
		assertNotNull(expandResult);
		assertEquals(1180, IterableUtils.size(expandResult));
	}

	@Test
	void testPreExpandedSnomedCodeSystem() {
		Iterable<org.opencds.cqf.cql.engine.runtime.Code> expandResult = getExpansion(
			"valueset-hospice-procedure.json",
			"http://fhir.org/guides/cdc/opioid-cds/ValueSet/hospice-procedure",
			"1.0.0");
		assertNotNull(expandResult);
		assertEquals(6, IterableUtils.size(expandResult));
	}

	@Test
	void testExpandIgDefinedCodeSystem() {
		Iterable<org.opencds.cqf.cql.engine.runtime.Code> expandResult = getExpansion(
			"valueset-opioidcds-indicator.json",
			"http://fhir.org/guides/cdc/opioid-cds/ValueSet/opioidcds-indicator",
			"0.1.1");
		assertNotNull(expandResult);
		assertEquals(3, IterableUtils.size(expandResult));
	}

	@Test
	void testExpandFilterWithoutExpansion() {
		Iterable<org.opencds.cqf.cql.engine.runtime.Code> expandResult = getExpansion(
			"valueset-hospice-finding.json",
			"http://fhir.org/guides/cdc/opioid-cds/ValueSet/hospice-finding",
			"0.1.1");
		assertNotNull(expandResult);
		assertEquals(0, IterableUtils.size(expandResult));
	}

	@Test
	void testMultipleVersions() {
		Iterable<org.opencds.cqf.cql.engine.runtime.Code> expandResult = getExpansion(
			"valueset-event-status-4.3.0.json",
			"http://example.org/fhir/ValueSet/event-status",
			"4.3.0");
		assertNotNull(expandResult);
		assertEquals(8, IterableUtils.size(expandResult));
		expandResult = getExpansion(
			"valueset-event-status-3.0.2.json",
			"http://example.org/fhir/ValueSet/event-status",
			"3.0.2");
		assertNotNull(expandResult);
		assertEquals(7, IterableUtils.size(expandResult));
		/*
			The last ValueSet added to the cache will be used when no version is supplied.
			In theory, this is the desired behavior as the "latest" ValueSet is picked for expansion by default.
			However, as in this case, the 4.3.0 version of the ValueSet was added to the cache before the 3.0.2 version.
			Therefore, the latest version of the ValueSet was not picked for expansion.
			This is not exactly ideal behavior and the FHIR spec states:
				"Note that if a References to a canonical URL does not have a version, and the server finds
				multiple versions for the value set, the system using the reference should pick the latest
				version of the target resource and use that." (http://www.hl7.org/fhir/references.html#canonical)
			Based on that, this is not a bug in the HAPI code. It is not the preferred approach though.
		*/
		ValueSetInfo vsInfo = new ValueSetInfo().withId("http://example.org/fhir/ValueSet/event-status");
		expandResult = terminologyProvider.expand(vsInfo);
		assertEquals(7, IterableUtils.size(expandResult));
	}

	private Iterable<org.opencds.cqf.cql.engine.runtime.Code> getExpansion(
		String vsFileName, String url, String version) {
		loadResource(vsFileName);
		ValueSetInfo vsInfo = new ValueSetInfo().withId(url).withVersion(version);
		return terminologyProvider.expand(vsInfo);
	}
}
