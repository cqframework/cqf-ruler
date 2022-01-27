package org.opencds.cqf.ruler.cr.dstu3.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.common.collect.Lists;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.test.DaoIntegrationTest;
import org.opencds.cqf.ruler.utility.Searches;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.jpa.partition.SystemRequestDetails;

@SpringBootTest(classes = { SubmitDataProviderIT.class }, properties = { "hapi.fhir.fhir_version=dstu3", })
public class SubmitDataProviderIT extends DaoIntegrationTest {

	@Autowired
	SubmitDataProvider mySubmitDataProvider;

	@Test
	public void testSubmitData() {
		// Create a MR and a resource
		MeasureReport mr = newResource(MeasureReport.class, "test-mr");
		Observation obs = newResource(Observation.class, "test-obs");

		// Submit it
		mySubmitDataProvider.submitData(new SystemRequestDetails(), new IdType("Measure", "test-m"), mr,
				Lists.newArrayList(obs));

		// Check if they made it to the db
		Observation savedObs = read(obs.getIdElement());
		assertNotNull(savedObs);

		MeasureReport savedMr = read(mr.getIdElement());
		assertNotNull(savedMr);
	}

	@Test
	public void testSubmitDataNoId() {
		// Create a MR and a resource
		MeasureReport mr = newResource(MeasureReport.class).setMeasure(new Reference("Measure/123"));
		Observation obs = newResource(Observation.class).setValue(new StringType("ABC"));

		// Submit it
		mySubmitDataProvider.submitData(new SystemRequestDetails(), new IdType("Measure", "123"), mr,
				Lists.newArrayList(obs));

		// Check if they made it to the db
		Observation savedObs = search(Observation.class, Searches.all()).single();
		assertNotNull(savedObs);
		assertEquals("ABC", savedObs.getValue().primitiveValue());

		MeasureReport savedMr = search(MeasureReport.class, Searches.all()).single();
		assertNotNull(savedMr);
		assertEquals("Measure/123", savedMr.getMeasure().getReference());
	}

}
