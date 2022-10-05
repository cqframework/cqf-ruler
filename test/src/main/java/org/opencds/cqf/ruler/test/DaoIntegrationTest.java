package org.opencds.cqf.ruler.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.opencds.cqf.ruler.behavior.IdCreator;
import org.opencds.cqf.ruler.behavior.ResourceCreator;
import org.opencds.cqf.ruler.test.behavior.ResourceLoader;
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
		"spring.datasource.url=jdbc:h2:mem:test",
		"spring.main.allow-circular-references=true",
		"debug=true",
		"loader.debug=true",
		"scheduling_disabled=true",
		"spring.main.allow-bean-definition-overriding=true",
		"spring.batch.job.enabled=false",
		// "spring.jpa.properties.hibernate.show_sql=true",
		// "spring.jpa.properties.hibernate.format_sql=true",
		// "spring.jpa.properties.hibernate.use_sql_comments=true",
		"hapi.fhir.allow_external_references=true",
		"hapi.fhir.enforce_referential_integrity_on_write=false",
		"hapi.fhir.auto_create_placeholder_reference_targets=true",
		"hapi.fhir.client_id_strategy=ANY",
		"spring.main.lazy-initialization=true",
		"spring.flyway.enabled=false" })
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
