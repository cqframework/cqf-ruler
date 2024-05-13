package org.opencds.cqf.ruler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.cr.config.RepositoryConfig;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = {
		Application.class,
		JpaStarterWebsocketDispatcherConfig.class
	}, properties =
	{
		"spring.profiles.include=storageSettingsTest",
		"spring.datasource.url=jdbc:h2:mem:dbr3",
		"hapi.fhir.fhir_version=dstu3",
		"hapi.fhir.cr_enabled=true",
		"hapi.fhir.mdm_enabled=false",
		"spring.batch.enabled=false",
		"hapi.fhir.subscription.websocket_enabled=true",
		"hapi.fhir.allow_external_references=true",
		"hapi.fhir.allow_placeholder_references=true",
		"hapi.fhir.enable_repository_validating_interceptor=true",
		"spring.main.allow-bean-definition-overriding=true",
		"spring.jpa.properties.hibernate.search.backend.directory.root=target/lucenefiles-dstu3",
	})


class ExampleServerDstu3IT implements IServerSupport {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(ExampleServerDstu3IT.class);
	private IGenericClient ourClient;
	private FhirContext ourCtx;

	@Autowired
	DaoRegistry myDaoRegistry;

	@LocalServerPort
	private int port;

	@BeforeEach
	void beforeEach() {
		ourCtx = FhirContext.forDstu3();
		ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
		String ourServerBase = "http://localhost:" + port + "/fhir/";
		ourClient = ourCtx.newRestfulGenericClient(ourServerBase);
		ourClient.registerInterceptor(new LoggingInterceptor(true));
	}

	@Test
	@DirtiesContext
	void testCreateAndRead() {

		String methodName = "testCreateResourceConditional";

		Patient pt = new Patient();
		pt.addName().setFamily(methodName);
		IIdType id = ourClient.create().resource(pt).execute().getId();

		Patient pt2 = ourClient.read().resource(Patient.class).withId(id).execute();
		assertEquals(methodName, pt2.getName().get(0).getFamily());
	}
}
