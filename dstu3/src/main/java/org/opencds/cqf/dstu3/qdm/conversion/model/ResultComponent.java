package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import org.hl7.fhir.dstu3.model.Range;

@DatatypeDef(name="ResultComponent")
public class ResultComponent extends Component {

    @Child(name="referenceRange", order=0)
            private Range referenceRange;
    public Range getReferenceRange() {
        return referenceRange;
    }
    public ResultComponent setReferenceRange(Range referenceRange) {
        this.referenceRange = referenceRange;
        return this;
    }	

    @Override
    protected ResultComponent typedCopy() {
        ResultComponent retVal = new ResultComponent();
        super.copyValues(retVal);
        retVal.referenceRange = referenceRange;
        return retVal;
    }
}
