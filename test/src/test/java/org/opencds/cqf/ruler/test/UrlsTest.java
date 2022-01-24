package org.opencds.cqf.ruler.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.validator.routines.UrlValidator;
import org.junit.jupiter.api.Test;

public class UrlsTest  {
    @Test
    public void testClientUrlWithTemplate() {

        String template = "http://localhost:%d/fhir";
        Integer port = 8084;
        String url = Urls.getUrl(template, port);

        String[] schemes = {"http","https"};
        UrlValidator urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_LOCAL_URLS);
        
        assertTrue(urlValidator.isValid(url));

        assertEquals("http://localhost:8084/fhir", url);
    }

    @Test
    public void testClientUrlWithoutTemplate() {

        Integer port = 8084;
        String url = Urls.getUrl(port);

        String[] schemes = {"http","https"};
        UrlValidator urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_LOCAL_URLS);
        
        assertTrue(urlValidator.isValid(url));
    }
}
