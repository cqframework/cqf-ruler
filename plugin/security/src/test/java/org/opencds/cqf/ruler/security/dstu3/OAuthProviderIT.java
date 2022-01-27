package org.opencds.cqf.ruler.security.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.security.SecurityConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { OAuthProviderIT.class,
	SecurityConfig.class }, properties = {
	"hapi.fhir.fhir_version=dstu3"
})
public class OAuthProviderIT extends RestIntegrationTest {
	@Test
	public void testOAuthConfig() {
		CapabilityStatement cs = getClient().capabilities().ofType(CapabilityStatement.class).execute();

		assertNotNull(cs);
		assertEquals(true, cs.getRestFirstRep().getSecurity().getCors());
		assertEquals("http://hl7.org/fhir/restful-security-service", cs.getRestFirstRep().getSecurity().getService().stream().findAny().get().getCodingFirstRep().getSystem());

	}
}
