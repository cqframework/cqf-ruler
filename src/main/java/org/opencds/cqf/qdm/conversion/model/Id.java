package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.instance.model.api.ICompositeType;

@DatatypeDef(name="Id")
public class Id extends Type implements ICompositeType {

    @Child(name = "namingSystem", order = 0)
    private StringType namingSystem;
    public StringType getNamingSystem() {
        return namingSystem;
    }
    public Id setNamingSystem(StringType namingSystem) {
        this.namingSystem = namingSystem;
        return this;
    }

    @Child(name = "value", order = 1)
    private StringType value;
    public StringType getValue() {
        return value;
    }
    public Id setValue(StringType value) {
        this.value = value;
        return this;
    }

    @Override
    protected Id typedCopy() {
        Id retVal = new Id();
        super.copyValues(retVal);
        retVal.namingSystem = namingSystem;
        retVal.value = value;
        return retVal;
    }
}
