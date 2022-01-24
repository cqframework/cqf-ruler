package org.opencds.cqf.ruler.cr.r4.provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.common.collect.Lists;

import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Observation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.common.utility.IdCreator;
import org.opencds.cqf.ruler.common.utility.ResourceCreator;
import org.opencds.cqf.ruler.test.DaoIntegrationTest;
import org.opencds.cqf.ruler.test.DaoOnlyConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.jpa.partition.SystemRequestDetails;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { DaoOnlyConfig.class }, properties = {
				"scheduling_disabled=true",
            "spring.main.allow-bean-definition-overriding=true",
            "spring.batch.job.enabled=false",
            "hapi.fhir.fhir_version=r4",
				"hapi.fhir.allow_external_references=true",
				"hapi.fhir.enforce_referential_integrity_on_write=false",
})
@EnableAutoConfiguration(exclude=QuartzAutoConfiguration.class)
public class SubmitDataProviderIT extends DaoIntegrationTest implements IdCreator, ResourceCreator {
	
	@Autowired
	SubmitDataProvider mySubmitDataProvider;

	@Test
	public void testSubmitData() {
		// Create a MR and a few resources
		MeasureReport mr = newResource(newId("MeasureReport/test-mr"));
		Observation obs = newResource(newId("Observation/test-obs"));

		// Submit it
		mySubmitDataProvider.submitData(new SystemRequestDetails(), newId("Measure/test-m"), mr, Lists.newArrayList(obs));

		// Check if they made it to the db
		Observation savedObs = read(obs.getIdElement());
		assertNotNull(savedObs);

		MeasureReport savedMr = read(mr.getIdElement());
		assertNotNull(savedMr);
	}

}
