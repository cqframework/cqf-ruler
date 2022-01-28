package org.opencds.cqf.ruler.utility.r4;

import org.hl7.fhir.r4.model.IdType;
import org.opencds.cqf.ruler.utility.Ids;

public class Parameters {

	private Parameters() {
	}

	public static org.hl7.fhir.r4.model.Parameters newParameters(IdType theId,
			org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		org.hl7.fhir.r4.model.Parameters p = newParameters(parts);
		p.setId(theId);
		return p;
	}

	public static org.hl7.fhir.r4.model.Parameters newParameters(String theIdPart,
			org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		return newParameters((IdType) Ids.newId(org.hl7.fhir.r4.model.Parameters.class, theIdPart), parts);
	}

	public static org.hl7.fhir.r4.model.Parameters newParameters(
			org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		org.hl7.fhir.r4.model.Parameters p = new org.hl7.fhir.r4.model.Parameters();
		for (org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent c : parts) {
			p.addParameter(c);
		}

		return p;
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent newPart(String name,
			org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent c = new org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent();
		c.setName(name);
		for (org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent p : parts) {
			c.addPart(p);
		}

		return c;
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent newPart(String name,
			org.hl7.fhir.r4.model.Type value, org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent c = newPart(name, parts);
		c.setValue(value);
		return c;
	}

	public static org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent newPart(String name,
			org.hl7.fhir.r4.model.Resource resource,
			org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent... parts) {
		org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent c = newPart(name, parts);
		c.setResource(resource);
		return c;
	}
}
