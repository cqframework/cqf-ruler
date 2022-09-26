package org.opencds.cqf.ruler.utility.r5;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.ruler.utility.r5.Parameters.getPartsByName;
import static org.opencds.cqf.ruler.utility.r5.Parameters.base64BinaryPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.booleanPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.canonicalPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.codePart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.datePart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.dateTimePart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.decimalPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.idPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.instantPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.integer64Part;
import static org.opencds.cqf.ruler.utility.r5.Parameters.integerPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.markdownPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.oidPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.parameters;
import static org.opencds.cqf.ruler.utility.r5.Parameters.positiveIntPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.stringPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.timePart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.unsignedIntPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.uriPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.urlPart;
import static org.opencds.cqf.ruler.utility.r5.Parameters.uuidPart;

class ParametersTest {
    @Test
    void testParametersPartTypes() {
        org.hl7.fhir.r5.model.Parameters parameters = parameters(
                base64BinaryPart("r5Base64BinaryPart", "SGVsbG8gV29ybGQh"),
                booleanPart("r5BooleanPart", true),
                canonicalPart("r5CanonicalPart", "https://example.com/Library/example-library"),
                codePart("r5CodePart", "active"),
                datePart("r5DatePart", "2012-12-31"),
                dateTimePart("r5DateTimePart", "2015-02-07T13:28:17-05:00"),
                decimalPart("r5DecimalPart", 72.42),
                idPart("r5IdPart", "example-id"),
                instantPart("r5InstantPart", "2015-02-07T13:28:17.239+02:00"),
                integerPart("r5IntegerPart", 72),
                integer64Part("r5Integer64Part", 9223372036854775807L),
                markdownPart("r5MarkdownPart", "## Markdown Title"),
                oidPart("r5OidPart", "urn:oid:1.2.3.4.5"),
                positiveIntPart("r5PositiveIntPart", 1),
                stringPart("r5StringPart", "example string"),
                timePart("r5TimePart", "12:30:30.500"),
                unsignedIntPart("r5UnsignedIntPart", 0),
                uriPart("r5UriPart", "s:comp.infosystems.www.servers.unix"),
                urlPart("r5UrlPart", "https://example.com"),
                uuidPart("r5UuidPart", "urn:uuid:c757873d-ec9a-4326-a141-556f43239520"));

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
        assertEquals("s:comp.infosystems.www.servers.unix", ((org.hl7.fhir.r5.model.UriType) r5Type).getValueAsString());

        r5Type = parameters.getParameter("r5UrlPart");
        assertTrue(r5Type instanceof org.hl7.fhir.r5.model.UrlType);
        assertEquals("https://example.com", ((org.hl7.fhir.r5.model.UrlType) r5Type).getValueAsString());

        r5Type = parameters.getParameter("r5UuidPart");
        assertTrue(r5Type instanceof org.hl7.fhir.r5.model.UuidType);
        assertEquals("urn:uuid:c757873d-ec9a-4326-a141-556f43239520", ((org.hl7.fhir.r5.model.UuidType) r5Type).getValueAsString());
    }

    @Test
    void getParameterByNameTest() {
        org.hl7.fhir.r5.model.Parameters parameters = parameters(
                stringPart("testName", "testValue"),
                stringPart("testName1", "testValue1")
        );

        List<org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent> parts = getPartsByName(parameters, "testName");
        assertEquals(1, parts.size());

        parameters = parameters(
                stringPart("testName", "testValue"),
                stringPart("testName", "testValue"),
                stringPart("testName1", "testValue1")
        );

        parts = getPartsByName(parameters, "testName");
        assertEquals(2, parts.size());
    }
}
