package org.opencds.cqf.qdm.types;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.opencds.cqf.cql.runtime.Code;


@DatatypeDef(name="Component")
public class Component extends Type implements ICompositeType {

    @Child(name="code", min=1, order=0)
    private Code code;
    public Code getCode() {
        return code;
    }
    public Component setCode(Code code) {
        this.code = code;
        return this;
    }

    @Child(name="result", order=1)
    private Object result;
    public Object getResult() {
        return result;
    }
    public Component setResult(Object result) {
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
