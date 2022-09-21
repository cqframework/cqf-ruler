package org.opencds.cqf.ruler.utility.r4;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.ruler.utility.r4.Parameters.getPartsByName;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newBase64BinaryPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newBooleanPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newCanonicalPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newCodePart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newDatePart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newDateTimePart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newDecimalPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newIdPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newInstantPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newIntegerPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newMarkdownPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newOidPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newParameters;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newPositiveIntPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newStringPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newTimePart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newUnsignedIntPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newUriPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newUrlPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newUuidPart;

class ParametersTest {
    @Test
    void testParametersPartTypes() {
        org.hl7.fhir.r4.model.Parameters parameters = newParameters(
                newBase64BinaryPart("r4Base64BinaryPart", "SGVsbG8gV29ybGQh"),
                newBooleanPart("r4BooleanPart", true),
                newCanonicalPart("r4CanonicalPart", "https://example.com/Library/example-library"),
                newCodePart("r4CodePart", "active"),
                newDatePart("r4DatePart", "2012-12-31"),
                newDateTimePart("r4DateTimePart", "2015-02-07T13:28:17-05:00"),
                newDecimalPart("r4DecimalPart", 72.42),
                newIdPart("r4IdPart", "example-id"),
                newInstantPart("r4InstantPart", "2015-02-07T13:28:17.239+02:00"),
                newIntegerPart("r4IntegerPart", 72),
                newMarkdownPart("r4MarkdownPart", "## Markdown Title"),
                newOidPart("r4OidPart", "urn:oid:1.2.3.4.5"),
                newPositiveIntPart("r4PositiveIntPart", 1),
                newStringPart("r4StringPart", "example string"),
                newTimePart("r4TimePart", "12:30:30.500"),
                newUnsignedIntPart("r4UnsignedIntPart", 0),
                newUriPart("r4UriPart", "news:comp.infosystems.www.servers.unix"),
                newUrlPart("r4UrlPart", "https://example.com"),
                newUuidPart("r4UuidPart", "urn:uuid:c757873d-ec9a-4326-a141-556f43239520"));

        org.hl7.fhir.r4.model.Type r4Type = parameters.getParameter("r4Base64BinaryPart");
        assertTrue(r4Type instanceof org.hl7.fhir.r4.model.Base64BinaryType);
        assertEquals("SGVsbG8gV29ybGQh", ((org.hl7.fhir.r4.model.Base64BinaryType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4BooleanPart");
        assertTrue(r4Type instanceof org.hl7.fhir.r4.model.BooleanType);
        assertTrue(((org.hl7.fhir.r4.model.BooleanType) r4Type).getValue());

        r4Type = parameters.getParameter("r4CanonicalPart");
        assertTrue(r4Type instanceof org.hl7.fhir.r4.model.CanonicalType);
        assertEquals("https://example.com/Library/example-library", ((org.hl7.fhir.r4.model.CanonicalType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4CodePart");
        assertTrue(r4Type instanceof org.hl7.fhir.r4.model.CodeType);
        assertEquals("active", ((org.hl7.fhir.r4.model.CodeType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4DatePart");
        assertTrue(r4Type instanceof org.hl7.fhir.r4.model.DateType);
        assertEquals("2012-12-31", ((org.hl7.fhir.r4.model.DateType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4DateTimePart");
        assertTrue(r4Type instanceof org.hl7.fhir.r4.model.DateTimeType);
        assertEquals("2015-02-07T13:28:17-05:00", ((org.hl7.fhir.r4.model.DateTimeType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4DecimalPart");
        assertTrue(r4Type instanceof org.hl7.fhir.r4.model.DecimalType);
        assertEquals("72.42", ((org.hl7.fhir.r4.model.DecimalType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4IdPart");
        assertTrue(r4Type instanceof org.hl7.fhir.r4.model.IdType);
        assertEquals("example-id", ((org.hl7.fhir.r4.model.IdType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4InstantPart");
        assertTrue(r4Type instanceof org.hl7.fhir.r4.model.InstantType);
        assertEquals("2015-02-07T13:28:17.239+02:00", ((org.hl7.fhir.r4.model.InstantType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4IntegerPart");
        assertTrue(r4Type instanceof org.hl7.fhir.r4.model.IntegerType);
        assertEquals(72, ((org.hl7.fhir.r4.model.IntegerType) r4Type).getValue());

        r4Type = parameters.getParameter("r4MarkdownPart");
        assertTrue(r4Type instanceof org.hl7.fhir.r4.model.MarkdownType);
        assertEquals("## Markdown Title", ((org.hl7.fhir.r4.model.MarkdownType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4OidPart");
        assertTrue(r4Type instanceof org.hl7.fhir.r4.model.OidType);
        assertEquals("urn:oid:1.2.3.4.5", ((org.hl7.fhir.r4.model.OidType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4PositiveIntPart");
        assertTrue(r4Type instanceof org.hl7.fhir.r4.model.PositiveIntType);
        assertEquals(1, ((org.hl7.fhir.r4.model.PositiveIntType) r4Type).getValue());

        r4Type = parameters.getParameter("r4StringPart");
        assertTrue(r4Type instanceof org.hl7.fhir.r4.model.StringType);
        assertEquals("example string", ((org.hl7.fhir.r4.model.StringType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4TimePart");
        assertTrue(r4Type instanceof org.hl7.fhir.r4.model.TimeType);
        assertEquals("12:30:30.500", ((org.hl7.fhir.r4.model.TimeType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4UnsignedIntPart");
        assertTrue(r4Type instanceof org.hl7.fhir.r4.model.UnsignedIntType);
        assertEquals(0, ((org.hl7.fhir.r4.model.UnsignedIntType) r4Type).getValue());

        r4Type = parameters.getParameter("r4UriPart");
        assertTrue(r4Type instanceof org.hl7.fhir.r4.model.UriType);
        assertEquals("news:comp.infosystems.www.servers.unix", ((org.hl7.fhir.r4.model.UriType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4UrlPart");
        assertTrue(r4Type instanceof org.hl7.fhir.r4.model.UrlType);
        assertEquals("https://example.com", ((org.hl7.fhir.r4.model.UrlType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4UuidPart");
        assertTrue(r4Type instanceof org.hl7.fhir.r4.model.UuidType);
        assertEquals("urn:uuid:c757873d-ec9a-4326-a141-556f43239520", ((org.hl7.fhir.r4.model.UuidType) r4Type).getValueAsString());
    }

    @Test
    void getParameterByNameTest() {
        org.hl7.fhir.r4.model.Parameters parameters = newParameters(
                newStringPart("testName", "testValue"),
                newStringPart("testName1", "testValue1")
        );

        List<org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent> parts = getPartsByName(parameters, "testName");
        assertEquals(1, parts.size());

        parameters = newParameters(
                newStringPart("testName", "testValue"),
                newStringPart("testName", "testValue"),
                newStringPart("testName1", "testValue1")
        );

        parts = getPartsByName(parameters, "testName");
        assertEquals(2, parts.size());
    }
}
