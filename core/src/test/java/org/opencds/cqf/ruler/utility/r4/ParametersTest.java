package org.opencds.cqf.ruler.utility.r4;

import org.hl7.fhir.r4.model.Base64BinaryType;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.MarkdownType;
import org.hl7.fhir.r4.model.OidType;
import org.hl7.fhir.r4.model.PositiveIntType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.TimeType;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UnsignedIntType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.UrlType;
import org.hl7.fhir.r4.model.UuidType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.ruler.utility.r4.Parameters.getPartsByName;
import static org.opencds.cqf.ruler.utility.r4.Parameters.base64BinaryPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.booleanPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.canonicalPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.codePart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.datePart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.dateTimePart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.decimalPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.idPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.instantPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.integerPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.markdownPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.oidPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.parameters;
import static org.opencds.cqf.ruler.utility.r4.Parameters.positiveIntPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.stringPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.timePart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.unsignedIntPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.uriPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.urlPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.uuidPart;

class ParametersTest {
    @Test
    void testParametersPartTypes() {
        org.hl7.fhir.r4.model.Parameters parameters = parameters(
                base64BinaryPart("r4Base64BinaryPart", "SGVsbG8gV29ybGQh"),
                booleanPart("r4BooleanPart", true),
                canonicalPart("r4CanonicalPart", "https://example.com/Library/example-library"),
                codePart("r4CodePart", "active"),
                datePart("r4DatePart", "2012-12-31"),
                dateTimePart("r4DateTimePart", "2015-02-07T13:28:17-05:00"),
                decimalPart("r4DecimalPart", 72.42),
                idPart("r4IdPart", "example-id"),
                instantPart("r4InstantPart", "2015-02-07T13:28:17.239+02:00"),
                integerPart("r4IntegerPart", 72),
                markdownPart("r4MarkdownPart", "## Markdown Title"),
                oidPart("r4OidPart", "urn:oid:1.2.3.4.5"),
                positiveIntPart("r4PositiveIntPart", 1),
                stringPart("r4StringPart", "example string"),
                timePart("r4TimePart", "12:30:30.500"),
                unsignedIntPart("r4UnsignedIntPart", 0),
                uriPart("r4UriPart", "s:comp.infosystems.www.servers.unix"),
                urlPart("r4UrlPart", "https://example.com"),
                uuidPart("r4UuidPart", "urn:uuid:c757873d-ec9a-4326-a141-556f43239520"));

        Type r4Type = parameters.getParameter("r4Base64BinaryPart");
        assertTrue(r4Type instanceof Base64BinaryType);
        assertEquals("SGVsbG8gV29ybGQh", ((Base64BinaryType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4BooleanPart");
        assertTrue(r4Type instanceof BooleanType);
        assertTrue(((BooleanType) r4Type).getValue());

        r4Type = parameters.getParameter("r4CanonicalPart");
        assertTrue(r4Type instanceof CanonicalType);
        assertEquals("https://example.com/Library/example-library", ((CanonicalType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4CodePart");
        assertTrue(r4Type instanceof CodeType);
        assertEquals("active", ((CodeType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4DatePart");
        assertTrue(r4Type instanceof DateType);
        assertEquals("2012-12-31", ((DateType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4DateTimePart");
        assertTrue(r4Type instanceof DateTimeType);
        assertEquals("2015-02-07T13:28:17-05:00", ((DateTimeType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4DecimalPart");
        assertTrue(r4Type instanceof DecimalType);
        assertEquals("72.42", ((DecimalType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4IdPart");
        assertTrue(r4Type instanceof IdType);
        assertEquals("example-id", ((IdType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4InstantPart");
        assertTrue(r4Type instanceof InstantType);
        assertEquals("2015-02-07T13:28:17.239+02:00", ((InstantType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4IntegerPart");
        assertTrue(r4Type instanceof IntegerType);
        assertEquals(72, ((IntegerType) r4Type).getValue());

        r4Type = parameters.getParameter("r4MarkdownPart");
        assertTrue(r4Type instanceof MarkdownType);
        assertEquals("## Markdown Title", ((MarkdownType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4OidPart");
        assertTrue(r4Type instanceof OidType);
        assertEquals("urn:oid:1.2.3.4.5", ((OidType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4PositiveIntPart");
        assertTrue(r4Type instanceof PositiveIntType);
        assertEquals(1, ((PositiveIntType) r4Type).getValue());

        r4Type = parameters.getParameter("r4StringPart");
        assertTrue(r4Type instanceof StringType);
        assertEquals("example string", ((StringType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4TimePart");
        assertTrue(r4Type instanceof TimeType);
        assertEquals("12:30:30.500", ((TimeType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4UnsignedIntPart");
        assertTrue(r4Type instanceof UnsignedIntType);
        assertEquals(0, ((UnsignedIntType) r4Type).getValue());

        r4Type = parameters.getParameter("r4UriPart");
        assertTrue(r4Type instanceof UriType);
        assertEquals("s:comp.infosystems.www.servers.unix", ((UriType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4UrlPart");
        assertTrue(r4Type instanceof UrlType);
        assertEquals("https://example.com", ((UrlType) r4Type).getValueAsString());

        r4Type = parameters.getParameter("r4UuidPart");
        assertTrue(r4Type instanceof UuidType);
        assertEquals("urn:uuid:c757873d-ec9a-4326-a141-556f43239520", ((UuidType) r4Type).getValueAsString());
    }

    @Test
    void getParameterByNameTest() {
        org.hl7.fhir.r4.model.Parameters parameters = parameters(
                stringPart("testName", "testValue"),
                stringPart("testName1", "testValue1")
        );

        List<org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent> parts = getPartsByName(parameters, "testName");
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
