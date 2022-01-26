package org.opencds.cqf.ruler.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.opencds.cqf.ruler.utility.IdCreator;
import org.opencds.cqf.ruler.utility.ResourceCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;

@EnableAutoConfiguration(exclude = QuartzAutoConfiguration.class)
@Import(DaoOnlyConfig.class)
@TestPropertySource(properties = {
		"scheduling_disabled=true",
		"spring.main.allow-bean-definition-overriding=true",
		"spring.batch.job.enabled=false",
		"hapi.fhir.allow_external_references=true",
		"hapi.fhir.enforce_referential_integrity_on_write=false",
		"spring.datasource.url=jdbc:h2:mem:db" ,
		"spring.main.lazy-initialization=true" })
@TestInstance(Lifecycle.PER_CLASS)
public class DaoIntegrationTest implements ResourceLoader, ResourceCreator, IdCreator {
	@Autowired
	TestDbService myDbService;

	@Autowired
	private FhirContext myCtx;

	@Autowired
	DaoRegistry myDaoRegistry;

	@Override
	public FhirContext getFhirContext() {
		return myCtx;
	}

	@Override
	public DaoRegistry getDaoRegistry() {
		return myDaoRegistry;
	}

	@AfterAll
	void baseAfterAll() {
		myDbService.resetDatabase();
	}
}
