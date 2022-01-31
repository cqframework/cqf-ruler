package org.opencds.cqf.ruler.cr.r4.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cr.CqlBuilder;
import org.opencds.cqf.ruler.cr.r4.Libraries;
import org.opencds.cqf.ruler.cr.r4.MeasureBuilder;
import org.opencds.cqf.ruler.cr.r4.Patients;
import org.opencds.cqf.ruler.test.DaoIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.jpa.partition.SystemRequestDetails;

@SpringBootTest(classes = { CollectDataProviderIT.class }, properties = { "hapi.fhir.fhir_version=r4", })
public class CollectDataProviderIT extends DaoIntegrationTest {

	@Autowired
	CollectDataProvider collectDataProvider;

	@Test
	public void testCollectData() {
		// Create test Measure
		String cql = CqlBuilder.newCql("4.0.1")
				.addExpression(
						"Initial Population",
						"exists([Observation])")
				.build();

		Library lib = Libraries.library(cql);

		Measure m = MeasureBuilder
				.newCohortMeasure()
				.setLibrary(lib)
				.build();

		this.create(lib);
		this.create(m);

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
				"2019-12-31", "Patient" + "/" + john.getIdElement().getIdPart(), null, null);

		List<ParametersParameterComponent> resources = results.getParameter().stream()
				.filter(x -> x.getName().equals("resource")).collect(Collectors.toList());
		assertEquals(1, resources.size());
		assertEquals("Observation", resources.get(0).getResource().fhirType());
	}
}
