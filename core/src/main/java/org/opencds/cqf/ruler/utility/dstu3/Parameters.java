package org.opencds.cqf.ruler.utility.dstu3;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.StringType;
import org.opencds.cqf.ruler.utility.Ids;

public class Parameters {

	private Parameters() {
	}

	public static org.hl7.fhir.dstu3.model.Parameters newParameters(IdType theId,
			org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent... parts) {
		checkNotNull(theId);

		org.hl7.fhir.dstu3.model.Parameters p = newParameters(parts);
		p.setId(theId);
		return p;
	}

	public static org.hl7.fhir.dstu3.model.Parameters newParameters(String theIdPart,
			org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent... parts) {
		checkNotNull(theIdPart);

		return newParameters((IdType) Ids.newId(org.hl7.fhir.dstu3.model.Parameters.class, theIdPart), parts);
	}

	public static org.hl7.fhir.dstu3.model.Parameters newParameters(
			org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent... parts) {
		org.hl7.fhir.dstu3.model.Parameters p = new org.hl7.fhir.dstu3.model.Parameters();
		for (org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent c : parts) {
			p.addParameter(c);
		}

		return p;
	}

	public static org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent newPart(String name,
			String value, org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent... parts) {
		checkNotNull(name);
		checkNotNull(value);

		org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent c = newPart(name, parts);
		c.setValue(new StringType(value));
		return c;
	}

	public static org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent newPart(String name,
			org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent... parts) {
		checkNotNull(name);

		org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent c = new org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent();
		c.setName(name);
		for (org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent p : parts) {
			c.addPart(p);
		}

		return c;
	}

	public static org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent newPart(String name,
			org.hl7.fhir.dstu3.model.Type value,
			org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent... parts) {
		checkNotNull(name);
		checkNotNull(value);

		org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent c = newPart(name, parts);
		c.setValue(value);
		return c;
	}

	public static org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent newPart(String name,
			org.hl7.fhir.dstu3.model.Resource resource,
			org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent... parts) {
		checkNotNull(name);
		checkNotNull(resource);

		org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent c = newPart(name, parts);
		c.setResource(resource);
		return c;
	}

	public static List<org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent> getPartsByName(
			org.hl7.fhir.dstu3.model.Parameters parameters, String name) {
		checkNotNull(parameters);
		checkNotNull(name);

		return parameters.getParameter().stream().filter(x -> name.equals(x.getName())).collect(Collectors.toList());
	}
}
