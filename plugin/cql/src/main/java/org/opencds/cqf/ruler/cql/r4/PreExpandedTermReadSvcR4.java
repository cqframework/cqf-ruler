package org.opencds.cqf.ruler.cql.r4;

import org.hl7.fhir.r4.model.ValueSet;

import ca.uhn.fhir.context.support.ValueSetExpansionOptions;
import ca.uhn.fhir.jpa.term.TermReadSvcR4;

public class PreExpandedTermReadSvcR4 extends TermReadSvcR4 {

	@Override
	public ValueSet expandValueSet(ValueSetExpansionOptions theExpansionOptions, ValueSet theValueSetToExpand) {
		if (theValueSetToExpand.hasExpansion()) {
			return theValueSetToExpand;
		}

		return super.expandValueSet(theExpansionOptions, theValueSetToExpand);
	}
}
