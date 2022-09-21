package org.opencds.cqf.ruler.utility.r5;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.stream.Collectors;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.StringType;

public class Parameters {

	private static final FhirContext fhirContext = FhirContext.forR5Cached();

	private Parameters() {
	}

	public static org.hl7.fhir.r5.model.Parameters newParameters(
			IdType theId, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters) org.opencds.cqf.ruler.utility.Parameters.newParameters(fhirContext, theId, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters newParameters(
			String theIdPart, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters) org.opencds.cqf.ruler.utility.Parameters.newParameters(fhirContext, theIdPart, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters newParameters(
			org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters) org.opencds.cqf.ruler.utility.Parameters.newParameters(fhirContext, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newPart(
			String name, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) org.opencds.cqf.ruler.utility.Parameters.newPart(fhirContext, name, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newPart(
			Class<org.hl7.fhir.r5.model.DataType> type, String name, Object value,
			org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) org.opencds.cqf.ruler.utility.Parameters.newPart(fhirContext, type, name, value, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newPart(
			String name, org.hl7.fhir.r5.model.Resource resource,
			org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) org.opencds.cqf.ruler.utility.Parameters.newPart(fhirContext, name, resource, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newPart(
			String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		checkNotNull(name);
		checkNotNull(value);
		org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent c = newPart(name, parts);
		c.setValue(new StringType(value));
		return c;
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newPart(
			String name, org.hl7.fhir.r5.model.DataType value,
			org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		checkNotNull(name);
		checkNotNull(value);
		org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent c = newPart(name, parts);
		c.setValue(value);
		return c;
	}

	public static List<org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent> getPartsByName(
			org.hl7.fhir.r5.model.Parameters parameters, String name) {
		return org.opencds.cqf.ruler.utility.Parameters.getPartsByName(fhirContext, parameters, name)
				.stream().map(x -> (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) x)
				.collect(Collectors.toList());
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newBase64BinaryPart(
			String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) org.opencds.cqf.ruler.utility.Parameters.newBase64BinaryPart(fhirContext, name, value, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newBooleanPart(
			String name, boolean value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) org.opencds.cqf.ruler.utility.Parameters.newBooleanPart(fhirContext, name, value, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newCanonicalPart(
			String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) org.opencds.cqf.ruler.utility.Parameters.newCanonicalPart(fhirContext, name, value, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newCodePart(
			String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) org.opencds.cqf.ruler.utility.Parameters.newCodePart(fhirContext, name, value, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newDatePart(
			String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) org.opencds.cqf.ruler.utility.Parameters.newDatePart(fhirContext, name, value, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newDateTimePart(
			String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) org.opencds.cqf.ruler.utility.Parameters.newDateTimePart(fhirContext, name, value, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newDecimalPart(
			String name, double value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) org.opencds.cqf.ruler.utility.Parameters.newDecimalPart(fhirContext, name, value, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newIdPart(
			String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) org.opencds.cqf.ruler.utility.Parameters.newIdPart(fhirContext, name, value, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newInstantPart(
			String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) org.opencds.cqf.ruler.utility.Parameters.newInstantPart(fhirContext, name, value, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newIntegerPart(
			String name, int value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) org.opencds.cqf.ruler.utility.Parameters.newIntegerPart(fhirContext, name, value, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newInteger64Part(
			String name, long value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) org.opencds.cqf.ruler.utility.Parameters.newInteger64Part(fhirContext, name, value, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newMarkdownPart(
			String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) org.opencds.cqf.ruler.utility.Parameters.newMarkdownPart(fhirContext, name, value, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newOidPart(
			String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) org.opencds.cqf.ruler.utility.Parameters.newOidPart(fhirContext, name, value, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newPositiveIntPart(
			String name, int value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) org.opencds.cqf.ruler.utility.Parameters.newPositiveIntPart(fhirContext, name, value, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newStringPart(
			String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) org.opencds.cqf.ruler.utility.Parameters.newStringPart(fhirContext, name, value, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newTimePart(
			String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) org.opencds.cqf.ruler.utility.Parameters.newTimePart(fhirContext, name, value, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newUnsignedIntPart(
			String name, int value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) org.opencds.cqf.ruler.utility.Parameters.newUnsignedIntPart(fhirContext, name, value, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newUriPart(
			String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) org.opencds.cqf.ruler.utility.Parameters.newUriPart(fhirContext, name, value, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newUrlPart(
			String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) org.opencds.cqf.ruler.utility.Parameters.newUrlPart(fhirContext, name, value, parts);
	}

	public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent newUuidPart(
			String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
		return (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) org.opencds.cqf.ruler.utility.Parameters.newUuidPart(fhirContext, name, value, parts);
	}
}
