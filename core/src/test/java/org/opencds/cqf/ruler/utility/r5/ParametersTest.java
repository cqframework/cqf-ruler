package org.opencds.cqf.ruler.utility.r5;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.ruler.utility.r5.Parameters.getPartsByName;
import static org.opencds.cqf.ruler.utility.r5.Parameters.newBase64BinaryPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.newBooleanPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.newCanonicalPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.newCodePart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.newDatePart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.newDateTimePart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.newDecimalPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.newIdPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.newInstantPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.newInteger64Part;
import static org.opencds.cqf.ruler.utility.r5.Parameters.newIntegerPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.newMarkdownPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.newOidPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.newParameters;
import static org.opencds.cqf.ruler.utility.r5.Parameters.newPositiveIntPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.newStringPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.newTimePart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.newUnsignedIntPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.newUriPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.newUrlPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.newUuidPart;

class ParametersTest {
    @Test
    void testParametersPartTypes() {
        org.hl7.fhir.r5.model.Parameters parameters = newParameters(
                newBase64BinaryPart("r5Base64BinaryPart", "SGVsbG8gV29ybGQh"),
                newBooleanPart("r5BooleanPart", true),
                newCanonicalPart("r5CanonicalPart", "https://example.com/Library/example-library"),
                newCodePart("r5CodePart", "active"),
                newDatePart("r5DatePart", "2012-12-31"),
                newDateTimePart("r5DateTimePart", "2015-02-07T13:28:17-05:00"),
                newDecimalPart("r5DecimalPart", 72.42),
                newIdPart("r5IdPart", "example-id"),
                newInstantPart("r5InstantPart", "2015-02-07T13:28:17.239+02:00"),
                newIntegerPart("r5IntegerPart", 72),
                newInteger64Part("r5Integer64Part", 9223372036854775807L),
                newMarkdownPart("r5MarkdownPart", "## Markdown Title"),
                newOidPart("r5OidPart", "urn:oid:1.2.3.4.5"),
                newPositiveIntPart("r5PositiveIntPart", 1),
                newStringPart("r5StringPart", "example string"),
                newTimePart("r5TimePart", "12:30:30.500"),
                newUnsignedIntPart("r5UnsignedIntPart", 0),
                newUriPart("r5UriPart", "news:comp.infosystems.www.servers.unix"),
                newUrlPart("r5UrlPart", "https://example.com"),
                newUuidPart("r5UuidPart", "urn:uuid:c757873d-ec9a-4326-a141-556f43239520"));

        org.hl7.fhir.r5.model.DataType r5Type = parameters.getParameter("r5Base64BinaryPart");
        assertTrue(r5Type instanceof org.hl7.fhir.r5.model.Base64BinaryType);
        assertEquals("SGVsbG8gV29ybGQh", ((org.hl7.fhir.r5.model.Base64BinaryType) r5Type).getValueAsString());

        r5Type = parameters.getParameter("r5BooleanPart");
        assertTrue(r5Type instanceof org.hl7.fhir.r5.model.BooleanType);
        assertTrue(((org.hl7.fhir.r5.model.BooleanType) r5Type).getValue());

        r5Type = parameters.getParameter("r5CanonicalPart");
        assertTrue(r5Type instanceof org.hl7.fhir.r5.model.CanonicalType);
        assertEquals("https://example.com/Library/example-library", ((org.hl7.fhir.r5.model.CanonicalType) r5Type).getValueAsString());

        r5Type = parameters.getParameter("r5CodePart");
        assertTrue(r5Type instanceof org.hl7.fhir.r5.model.CodeType);
        assertEquals("active", ((org.hl7.fhir.r5.model.CodeType) r5Type).getValueAsString());

        r5Type = parameters.getParameter("r5DatePart");
        assertTrue(r5Type instanceof org.hl7.fhir.r5.model.DateType);
        assertEquals("2012-12-31", ((org.hl7.fhir.r5.model.DateType) r5Type).getValueAsString());

        r5Type = parameters.getParameter("r5DateTimePart");
        assertTrue(r5Type instanceof org.hl7.fhir.r5.model.DateTimeType);
        assertEquals("2015-02-07T13:28:17-05:00", ((org.hl7.fhir.r5.model.DateTimeType) r5Type).getValueAsString());

        r5Type = parameters.getParameter("r5DecimalPart");
        assertTrue(r5Type instanceof org.hl7.fhir.r5.model.DecimalType);
        assertEquals("72.42", ((org.hl7.fhir.r5.model.DecimalType) r5Type).getValueAsString());

        r5Type = parameters.getParameter("r5IdPart");
        assertTrue(r5Type instanceof org.hl7.fhir.r5.model.IdType);
        assertEquals("example-id", ((org.hl7.fhir.r5.model.IdType) r5Type).getValueAsString());

        r5Type = parameters.getParameter("r5InstantPart");
        assertTrue(r5Type instanceof org.hl7.fhir.r5.model.InstantType);
        assertEquals("2015-02-07T13:28:17.239+02:00", ((org.hl7.fhir.r5.model.InstantType) r5Type).getValueAsString());

        r5Type = parameters.getParameter("r5IntegerPart");
        assertTrue(r5Type instanceof org.hl7.fhir.r5.model.IntegerType);
        assertEquals(72, ((org.hl7.fhir.r5.model.IntegerType) r5Type).getValue());

        r5Type = parameters.getParameter("r5Integer64Part");
        assertTrue(r5Type instanceof org.hl7.fhir.r5.model.Integer64Type);
        assertEquals(9223372036854775807L, ((org.hl7.fhir.r5.model.Integer64Type) r5Type).getValue());

        r5Type = parameters.getParameter("r5MarkdownPart");
        assertTrue(r5Type instanceof org.hl7.fhir.r5.model.MarkdownType);
        assertEquals("## Markdown Title", ((org.hl7.fhir.r5.model.MarkdownType) r5Type).getValueAsString());

        r5Type = parameters.getParameter("r5OidPart");
        assertTrue(r5Type instanceof org.hl7.fhir.r5.model.OidType);
        assertEquals("urn:oid:1.2.3.4.5", ((org.hl7.fhir.r5.model.OidType) r5Type).getValueAsString());

        r5Type = parameters.getParameter("r5PositiveIntPart");
        assertTrue(r5Type instanceof org.hl7.fhir.r5.model.PositiveIntType);
        assertEquals(1, ((org.hl7.fhir.r5.model.PositiveIntType) r5Type).getValue());

        r5Type = parameters.getParameter("r5StringPart");
        assertTrue(r5Type instanceof org.hl7.fhir.r5.model.StringType);
        assertEquals("example string", ((org.hl7.fhir.r5.model.StringType) r5Type).getValueAsString());

        r5Type = parameters.getParameter("r5TimePart");
        assertTrue(r5Type instanceof org.hl7.fhir.r5.model.TimeType);
        assertEquals("12:30:30.500", ((org.hl7.fhir.r5.model.TimeType) r5Type).getValueAsString());

        r5Type = parameters.getParameter("r5UnsignedIntPart");
        assertTrue(r5Type instanceof org.hl7.fhir.r5.model.UnsignedIntType);
        assertEquals(0, ((org.hl7.fhir.r5.model.UnsignedIntType) r5Type).getValue());

        r5Type = parameters.getParameter("r5UriPart");
        assertTrue(r5Type instanceof org.hl7.fhir.r5.model.UriType);
        assertEquals("news:comp.infosystems.www.servers.unix", ((org.hl7.fhir.r5.model.UriType) r5Type).getValueAsString());

        r5Type = parameters.getParameter("r5UrlPart");
        assertTrue(r5Type instanceof org.hl7.fhir.r5.model.UrlType);
        assertEquals("https://example.com", ((org.hl7.fhir.r5.model.UrlType) r5Type).getValueAsString());

        r5Type = parameters.getParameter("r5UuidPart");
        assertTrue(r5Type instanceof org.hl7.fhir.r5.model.UuidType);
        assertEquals("urn:uuid:c757873d-ec9a-4326-a141-556f43239520", ((org.hl7.fhir.r5.model.UuidType) r5Type).getValueAsString());
    }

    @Test
    void getParameterByNameTest() {
        org.hl7.fhir.r5.model.Parameters parameters = newParameters(
                newStringPart("testName", "testValue"),
                newStringPart("testName1", "testValue1")
        );

        List<org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent> parts = getPartsByName(parameters, "testName");
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
