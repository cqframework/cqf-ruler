package org.opencds.cqf.ruler.cr.dstu3.provider;

import static org.opencds.cqf.ruler.utility.dstu3.Parameters.getPartsByName;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cr.CqlBuilder;
import org.opencds.cqf.ruler.cr.dstu3.Libraries;
import org.opencds.cqf.ruler.cr.dstu3.MeasureBuilder;
import org.opencds.cqf.ruler.cr.dstu3.Patients;
import org.opencds.cqf.ruler.test.DaoIntegrationTest;
import org.opencds.cqf.ruler.utility.Ids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.jpa.partition.SystemRequestDetails;

@SpringBootTest(classes = { CollectDataProviderIT.class }, properties = { "hapi.fhir.fhir_version=dstu3", })
class CollectDataProviderIT extends DaoIntegrationTest {

	@Autowired
	CollectDataProvider collectDataProvider;

	@Test
	void testCollectData() {
		// Create test Measure
		String cql = CqlBuilder.newCqlLibrary("3.0.0")
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

		List<ParametersParameterComponent> resources = getPartsByName(results, "resource");
		assertEquals(1, resources.size());
		assertEquals("Observation", resources.get(0).getResource().fhirType());

		List<ParametersParameterComponent> reports = getPartsByName(results, "measureReport");
		assertEquals(1, reports.size());
		assertEquals("MeasureReport", reports.get(0).getResource().fhirType());
	}
}
