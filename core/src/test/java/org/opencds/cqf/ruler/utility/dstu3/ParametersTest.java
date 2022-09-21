package org.opencds.cqf.ruler.utility.dstu3;

import org.hl7.fhir.dstu3.model.Base64BinaryType;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.DecimalType;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.InstantType;
import org.hl7.fhir.dstu3.model.IntegerType;
import org.hl7.fhir.dstu3.model.OidType;
import org.hl7.fhir.dstu3.model.PositiveIntType;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.TimeType;
import org.hl7.fhir.dstu3.model.UnsignedIntType;
import org.hl7.fhir.dstu3.model.UriType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.getPartsByName;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newBase64BinaryPart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newBooleanPart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newCodePart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newDatePart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newDateTimePart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newDecimalPart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newIdPart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newInstantPart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newIntegerPart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newOidPart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newParameters;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newPositiveIntPart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newStringPart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newTimePart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newUnsignedIntPart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newUriPart;

class ParametersTest {
    @Test
    void testParametersPartTypes() {
        org.hl7.fhir.dstu3.model.Parameters parameters = newParameters(
                newBase64BinaryPart("stu3Base64BinaryPart", "SGVsbG8gV29ybGQh"),
                newBooleanPart("stu3BooleanPart", true),
                newCodePart("stu3CodePart", "active"),
                newDatePart("stu3DatePart", "2012-12-31"),
                newDateTimePart("stu3DateTimePart", "2015-02-07T13:28:17-05:00"),
                newDecimalPart("stu3DecimalPart", 72.42),
                newIdPart("stu3IdPart", "example-id"),
                newInstantPart("stu3InstantPart", "2015-02-07T13:28:17.239+02:00"),
                newIntegerPart("stu3IntegerPart", 72),
                newOidPart("stu3OidPart", "urn:oid:1.2.3.4.5"),
                newPositiveIntPart("stu3PositiveIntPart", 1),
                newStringPart("stu3StringPart", "example string"),
                newTimePart("stu3TimePart", "12:30:30.500"),
                newUnsignedIntPart("stu3UnsignedIntPart", 0),
                newUriPart("stu3UriPart", "news:comp.infosystems.www.servers.unix"));

        for (org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent stu3Component : parameters.getParameter()) {
            if (stu3Component.getName().equals("stu3Base64BinaryPart")) {
                assertTrue(stu3Component.getValue() instanceof Base64BinaryType);
                assertEquals("SGVsbG8gV29ybGQh", ((Base64BinaryType) stu3Component.getValue()).getValueAsString());
            }
            else if (stu3Component.getName().equals("stu3BooleanPart")) {
                assertTrue(stu3Component.getValue() instanceof BooleanType);
                assertTrue(((BooleanType) stu3Component.getValue()).getValue());
            }
            else if (stu3Component.getName().equals("stu3CodePart")) {
                assertTrue(stu3Component.getValue() instanceof CodeType);
                assertEquals("active", ((CodeType) stu3Component.getValue()).getValueAsString());
            }
            else if (stu3Component.getName().equals("stu3DatePart")) {
                assertTrue(stu3Component.getValue() instanceof DateType);
                assertEquals("2012-12-31", ((DateType) stu3Component.getValue()).getValueAsString());
            }
            else if (stu3Component.getName().equals("stu3DateTimePart")) {
                assertTrue(stu3Component.getValue() instanceof DateTimeType);
                assertEquals("2015-02-07T13:28:17-05:00", ((DateTimeType) stu3Component.getValue()).getValueAsString());
            }
            else if (stu3Component.getName().equals("stu3DecimalPart")) {
                assertTrue(stu3Component.getValue() instanceof DecimalType);
                assertEquals("72.42", ((DecimalType) stu3Component.getValue()).getValueAsString());
            }
            else if (stu3Component.getName().equals("stu3IdPart")) {
                assertTrue(stu3Component.getValue() instanceof IdType);
                assertEquals("example-id", ((IdType) stu3Component.getValue()).getValueAsString());
            }
            else if (stu3Component.getName().equals("stu3InstantPart")) {
                assertTrue(stu3Component.getValue() instanceof InstantType);
                assertEquals("2015-02-07T13:28:17.239+02:00", ((InstantType) stu3Component.getValue()).getValueAsString());
            }
            else if (stu3Component.getName().equals("stu3IntegerPart")) {
                assertTrue(stu3Component.getValue() instanceof IntegerType);
                assertEquals(72, ((IntegerType) stu3Component.getValue()).getValue());
            }
            else if (stu3Component.getName().equals("stu3OidPart")) {
                assertTrue(stu3Component.getValue() instanceof OidType);
                assertEquals("urn:oid:1.2.3.4.5", ((OidType) stu3Component.getValue()).getValueAsString());
            }
            else if (stu3Component.getName().equals("stu3PositiveIntPart")) {
                assertTrue(stu3Component.getValue() instanceof PositiveIntType);
                assertEquals(1, ((PositiveIntType) stu3Component.getValue()).getValue());
            }
            else if (stu3Component.getName().equals("stu3StringPart")) {
                assertTrue(stu3Component.getValue() instanceof StringType);
                assertEquals("example string", ((StringType) stu3Component.getValue()).getValueAsString());
            }
            else if (stu3Component.getName().equals("stu3TimePart")) {
                assertTrue(stu3Component.getValue() instanceof TimeType);
                assertEquals("12:30:30.500", ((TimeType) stu3Component.getValue()).getValueAsString());
            }
            else if (stu3Component.getName().equals("stu3UnsignedIntPart")) {
                assertTrue(stu3Component.getValue() instanceof UnsignedIntType);
                assertEquals(0, ((UnsignedIntType) stu3Component.getValue()).getValue());
            }
            else if (stu3Component.getName().equals("stu3UriPart")) {
                assertTrue(stu3Component.getValue() instanceof UriType);
                assertEquals("news:comp.infosystems.www.servers.unix", ((UriType) stu3Component.getValue()).getValueAsString());
            }
        }
    }

    @Test
    void getParameterByNameTest() {
        org.hl7.fhir.dstu3.model.Parameters parameters = newParameters(
                newStringPart("testName", "testValue"),
                newStringPart("testName1", "testValue1")
        );

        List<org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent> parts = getPartsByName(parameters, "testName");
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
