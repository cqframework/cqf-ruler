package org.opencds.cqf.ruler.cpg.dstu3.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.cql.evaluator.fhir.util.dstu3.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.dstu3.Parameters.stringPart;

import org.hl7.fhir.dstu3.model.IntegerType;
import org.hl7.fhir.dstu3.model.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cpg.CpgConfig;
import org.opencds.cqf.ruler.security.SecurityConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CqlExecutionProviderIT.class,
		CpgConfig.class, SecurityConfig.class }, properties = { "hapi.fhir.fhir_version=dstu3",
				"hapi.fhir.security.basic_auth.enabled=true",
				"hapi.fhir.security.basic_auth.username=admin",
				"hapi.fhir.security.basic_auth.password=admin" })
class CqlExecutionAuthenticationIT extends RestIntegrationTest {
	private final String packagePrefix = "org/opencds/cqf/ruler/cpg/dstu3/provider/";

	@BeforeEach

	@Test
	void testSimpleArithmeticCqlExecutionProvider() {
		Parameters params = parameters(stringPart("expression", "5 * 5"));
		Parameters results = getClient().operation()
				.onServer()
				.named("$cql")
				.withParameters(params)
				.withAdditionalHeader("Authorization", "Basic YWRtaW46YWRtaW4=")
				.execute();

		assertTrue(results.getParameter().get(0).getValue() instanceof IntegerType);
		assertEquals("25", ((IntegerType) results.getParameter().get(0).getValue()).asStringValue());
	}

	@Test
	void testSimpleArithmeticCqlExecutionProviderWithoutAuth() {
		Parameters params = parameters(stringPart("expression", "5 * 5"));
		assertThrows(AuthenticationException.class, () -> {
			getClient().operation()
					.onServer()
					.named("$cql")
					.withParameters(params)
					.execute();
		});
	}
}
