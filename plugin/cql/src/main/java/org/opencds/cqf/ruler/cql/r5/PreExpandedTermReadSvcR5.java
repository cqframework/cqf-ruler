package org.opencds.cqf.ruler.cql.r5;

import ca.uhn.fhir.jpa.term.TermReadSvcImpl;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.ValueSet;

import ca.uhn.fhir.context.support.ValueSetExpansionOptions;

public class PreExpandedTermReadSvcR5 extends TermReadSvcImpl {

	@Override
	public IBaseResource expandValueSet(ValueSetExpansionOptions theExpansionOptions, IBaseResource theInput) {
		ValueSet vs = (ValueSet) theInput;
		if (vs != null && vs.hasExpansion()) {
			return vs;
		}

		return super.expandValueSet(theExpansionOptions, theInput);
	}
}
