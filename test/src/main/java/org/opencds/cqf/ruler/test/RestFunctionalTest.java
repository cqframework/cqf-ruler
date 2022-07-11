package org.opencds.cqf.ruler.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.opencds.cqf.ruler.Application;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@Import(Application.class)
@TestPropertySource(properties = {
	"debug=true",
	"loader.debug=true",
	"scheduling_disabled=true",
	"spring.main.allow-bean-definition-overriding=true",
	"spring.batch.job.enabled=false",
	"hapi.fhir.allow_external_references=true",
	"hapi.fhir.enforce_referential_integrity_on_write=false",
	"hapi.fhir.auto_create_placeholder_reference_targets=true",
	"hapi.fhir.client_id_strategy=ANY",
	"spring.main.lazy-initialization=true",
	"spring.flyway.enabled=false" })
@TestInstance(Lifecycle.PER_METHOD)
public class RestFunctionalTest extends TestInitService {

	@BeforeEach
	void beforeEach() {
		baseBeforeEach();
	}

	@AfterEach
	void afterEach() {
		baseAfterAll();
	}

}
