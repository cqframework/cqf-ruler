package org.opencds.cqf.ruler.cql.r5;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.ValueSet;

import ca.uhn.fhir.context.support.ValueSetExpansionOptions;
import ca.uhn.fhir.jpa.term.TermReadSvcR5;

public class PreExpandedTermReadSvcR5 extends TermReadSvcR5 {

	@Override
	public IBaseResource expandValueSet(ValueSetExpansionOptions theExpansionOptions, IBaseResource theInput) {
		ValueSet vs = (ValueSet) theInput;
		if (vs != null && vs.hasExpansion()) {
			return vs;
		}

		return super.expandValueSet(theExpansionOptions, theInput);
	}
}
