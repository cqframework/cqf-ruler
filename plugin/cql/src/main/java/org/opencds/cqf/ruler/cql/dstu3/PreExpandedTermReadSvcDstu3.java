package org.opencds.cqf.ruler.cql.dstu3;

import ca.uhn.fhir.jpa.term.TermReadSvcImpl;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.context.support.ValueSetExpansionOptions;

public class PreExpandedTermReadSvcDstu3 extends TermReadSvcImpl {
	@Override
	public IBaseResource expandValueSet(ValueSetExpansionOptions theExpansionOptions, IBaseResource theInput) {
		ValueSet vs = (ValueSet) theInput;
		if (vs != null && vs.hasExpansion()) {
			return vs;
		}

		return super.expandValueSet(theExpansionOptions, theInput);
	}
}
