package org.opencds.cqf.ruler.utility.r4;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.stream.Collectors;

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
import org.hl7.fhir.r4.model.UnsignedIntType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.UrlType;
import org.hl7.fhir.r4.model.UuidType;
import org.opencds.cqf.ruler.utility.Ids;

public class Parameters {

	private Parameters() {
	}

	public static org.hl7.fhir.r4.model.Parameters parameters(
			IdType theId, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		checkNotNull(theId);
		org.hl7.fhir.r4.model.Parameters p = parameters(parts);
		p.setId(theId);
		return p;
	}

	public static org.hl7.fhir.r4.model.Parameters parameters(
			String theIdPart, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		checkNotNull(theIdPart);
		return parameters((IdType) Ids.newId(org.hl7.fhir.r4.model.Parameters.class, theIdPart), parts);
	}

	public static org.hl7.fhir.r4.model.Parameters parameters(
			org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		org.hl7.fhir.r4.model.Parameters p = new org.hl7.fhir.r4.model.Parameters();
		for (org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent c : parts) {
			p.addParameter(c);
		}
		return p;
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent part(
			String name, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		checkNotNull(name);
		org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent c = new org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent();
		c.setName(name);
		for (org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent p : parts) {
			c.addPart(p);
		}
		return c;
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent part(
			String name, String value, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		checkNotNull(name);
		checkNotNull(value);
		org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent c = part(name, parts);
		c.setValue(new StringType(value));
		return c;
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent part(
			String name, org.hl7.fhir.r4.model.Type value, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		checkNotNull(name);
		checkNotNull(value);
		org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent c = part(name, parts);
		c.setValue(value);
		return c;
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent part(
			String name, org.hl7.fhir.r4.model.Resource resource,
			org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		checkNotNull(name);
		checkNotNull(resource);
		org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent c = part(name, parts);
		c.setResource(resource);
		return c;
	}

	public static List<org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent> getPartsByName(
			org.hl7.fhir.r4.model.Parameters parameters, String name) {
		checkNotNull(parameters);
		checkNotNull(name);
		return parameters.getParameter().stream().filter(x -> name.equals(x.getName())).collect(Collectors.toList());
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent base64BinaryPart(
			String name, String value, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		return part(name, new Base64BinaryType(value), parts);
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent booleanPart(
			String name, boolean value, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		return part(name, new BooleanType(value), parts);
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent canonicalPart(
			String name, String value, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		return part(name, new CanonicalType(value), parts);
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent codePart(
			String name, String value, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		return part(name, new CodeType(value), parts);
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent datePart(
			String name, String value, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		return part(name, new DateType(value), parts);
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent dateTimePart(
			String name, String value, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		return part(name, new DateTimeType(value), parts);
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent decimalPart(
			String name, double value, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		return part(name, new DecimalType(value), parts);
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent idPart(
			String name, String value, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		return part(name, new IdType(value), parts);
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent instantPart(
			String name, String value, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		return part(name, new InstantType(value), parts);
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent integerPart(
			String name, int value, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		return part(name, new IntegerType(value), parts);
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent markdownPart(
			String name, String value, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		return part(name, new MarkdownType(value), parts);
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent oidPart(
			String name, String value, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		return part(name, new OidType(value), parts);
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent positiveIntPart(
			String name, int value, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		return part(name, new PositiveIntType(value), parts);
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent stringPart(
			String name, String value, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		return part(name, new StringType(value), parts);
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent timePart(
			String name, String value, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		return part(name, new TimeType(value), parts);
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent unsignedIntPart(
			String name, int value, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		return part(name, new UnsignedIntType(value), parts);
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent uriPart(
			String name, String value, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		return part(name, new UriType(value), parts);
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent urlPart(
			String name, String value, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		return part(name, new UrlType(value), parts);
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent uuidPart(
			String name, String value, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		return part(name, new UuidType(value), parts);
	}
}
