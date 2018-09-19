package org.opencds.cqf.qdm.types;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import org.opencds.cqf.cql.runtime.Interval;


@DatatypeDef(name="ResultComponent")
public class ResultComponent extends Component {

    @Child(name="referenceRange", order=0)
    private Interval referenceRange;
    public Interval getReferenceRange() {
        return referenceRange;
    }
    public ResultComponent setReferenceRange(Interval referenceRange) {
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
