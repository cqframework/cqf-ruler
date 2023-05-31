package org.opencds.cqf.ruler.cql.r4;

import ca.uhn.fhir.jpa.term.TermReadSvcImpl;
import org.hl7.fhir.r4.model.ValueSet;

import ca.uhn.fhir.context.support.ValueSetExpansionOptions;

public class PreExpandedTermReadSvcR4 extends TermReadSvcImpl {

	@Override
	public ValueSet expandValueSet(ValueSetExpansionOptions theExpansionOptions, ValueSet theValueSetToExpand) {
		if (theValueSetToExpand.hasExpansion()) {
			return theValueSetToExpand;
		}

		return super.expandValueSet(theExpansionOptions, theValueSetToExpand);
	}
}
