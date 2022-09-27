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
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.base64BinaryPart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.booleanPart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.codePart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.datePart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.dateTimePart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.decimalPart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.idPart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.instantPart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.integerPart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.oidPart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.parameters;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.positiveIntPart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.stringPart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.timePart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.unsignedIntPart;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.uriPart;

class ParametersTest {
    @Test
    void testParametersPartTypes() {
        org.hl7.fhir.dstu3.model.Parameters parameters = parameters(
                base64BinaryPart("stu3Base64BinaryPart", "SGVsbG8gV29ybGQh"),
                booleanPart("stu3BooleanPart", true),
                codePart("stu3CodePart", "active"),
                datePart("stu3DatePart", "2012-12-31"),
                dateTimePart("stu3DateTimePart", "2015-02-07T13:28:17-05:00"),
                decimalPart("stu3DecimalPart", 72.42),
                idPart("stu3IdPart", "example-id"),
                instantPart("stu3InstantPart", "2015-02-07T13:28:17.239+02:00"),
                integerPart("stu3IntegerPart", 72),
                oidPart("stu3OidPart", "urn:oid:1.2.3.4.5"),
                positiveIntPart("stu3PositiveIntPart", 1),
                stringPart("stu3StringPart", "example string"),
                timePart("stu3TimePart", "12:30:30.500"),
                unsignedIntPart("stu3UnsignedIntPart", 0),
                uriPart("stu3UriPart", "news:comp.infosystems.www.servers.unix"));

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
        org.hl7.fhir.dstu3.model.Parameters parameters = parameters(
                stringPart("testName", "testValue"),
                stringPart("testName1", "testValue1")
        );

        List<org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent> parts = getPartsByName(parameters, "testName");
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
