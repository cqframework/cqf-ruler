package org.opencds.cqf.ruler.cr.r4.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import ca.uhn.fhir.cr.config.CrR4Config;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.evaluator.fhir.util.Ids;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.cr.CqlBuilder;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.cr.r4.Libraries;
import org.opencds.cqf.ruler.cr.r4.MeasureBuilder;
import org.opencds.cqf.ruler.cr.r4.Patients;
import org.opencds.cqf.ruler.test.DaoIntegrationTest;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.rest.api.server.SystemRequestDetails;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = { CrConfig.class}, properties = {
		"hapi.fhir.fhir_version=r4" })
class CollectDataProviderIT extends RestIntegrationTest {

	@Autowired
	CollectDataProvider collectDataProvider;

	@Test
	void testCollectData() {
		// Create test Measure
		String cql = CqlBuilder.newCqlLibrary("4.0.1")
				.addExpression(
						"Initial Population",
						"exists([Observation])")
				.build();

		Library lib = Libraries.library(cql);

		Measure m = MeasureBuilder
				.newCohortMeasure(lib)
				.build();

		this.update(lib);
		this.update(m);

		// Create test data
		Patient john = Patients.john_doe();
		this.create(john);

		Observation obs = newResource(Observation.class)
				.setSubject(new Reference(john));
		this.create(obs);

		Encounter enc = newResource(Encounter.class)
				.setSubject(new Reference(john));
		this.create(enc);

		// Submit it
		Parameters results = collectDataProvider.collectData(new SystemRequestDetails(), m.getIdElement(), "2019-01-01",
				"2019-12-31", Ids.simple(john), null, null);

		List<ParametersParameterComponent> resources = org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters
				.getPartsByName(results,
						"resource");
		assertEquals(1, resources.size());
		assertEquals("Observation", resources.get(0).getResource().fhirType());

		List<ParametersParameterComponent> reports = org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters
				.getPartsByName(results,
						"measureReport");
		assertEquals(1, reports.size());
		assertEquals("MeasureReport", reports.get(0).getResource().fhirType());
	}
}
