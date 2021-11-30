package org.opencds.cqf.ruler.plugin.testutility;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ClientUtilitiesTest implements ClientUtilities {
    @Test
    public void testClientUrlWithTemplate() {

        String template = "http://localhost:%d/fhir";
        Integer port = 8084;
        String url = getClientUrl(template, port);

        assertEquals("http://localhost:8084/fhir", url);
    }

    @Test
    public void testClientUrlWithoutTemplate() {

        Integer port = 8084;
        String url = getClientUrl(port);

        assertEquals("http://localhost:8084/fhir", url);
    }
}
