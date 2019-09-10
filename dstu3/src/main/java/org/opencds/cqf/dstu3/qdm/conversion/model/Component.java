package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.instance.model.api.ICompositeType;

@DatatypeDef(name="Component")
public class Component extends Type implements ICompositeType {

    @Child(name="code", min=1, order=0)
    private Coding code;
    public Coding getCode() {
        return code;
    }
    public Component setCode(Coding code) {
        this.code = code;
        return this;
    }

    @Child(name="result", order=1)
    private Type result;
    public Type getResult() {
        return result;
    }
    public Component setResult(Type result) {
        this.result = result;
        return this;
    }

    @Override
    protected Component typedCopy() {
        Component retVal = new Component();
        super.copyValues(retVal);
        retVal.code = code;
        retVal.result = result;
        return retVal;
    }
}
