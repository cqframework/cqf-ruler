package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.instance.model.api.ICompositeType;

@DatatypeDef(name="Ratio")
public class Ratio extends Type implements ICompositeType {

    @Child(name = "numerator", order = 0, min = 1)
    private Quantity numerator;
    public Quantity getNumerator() {
        return numerator;
    }
    public Ratio setNumerator(Quantity numerator) {
        this.numerator = numerator;
        return this;
    }

    @Child(name = "denominator", order = 1, min = 1)
    private Quantity denominator;
    public Quantity getValue() {
        return denominator;
    }
    public Ratio setValue(Quantity denominator) {
        this.denominator = denominator;
        return this;
    }

    @Override
    protected Ratio typedCopy() {
        Ratio retVal = new Ratio();
        super.copyValues(retVal);
        retVal.numerator = numerator;
        retVal.denominator = denominator;
        return retVal;
    }
}
