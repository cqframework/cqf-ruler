package org.opencds.cqf.qdm.types;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.instance.model.api.ICompositeType;

@DatatypeDef(name="Id")
public class Id extends Type implements ICompositeType {

    @Child(name = "namingSystem", order = 0)
    private String namingSystem;
    public String getNamingSystem() {
        return namingSystem;
    }
    public Id setNamingSystem(String namingSystem) {
        this.namingSystem = namingSystem;
        return this;
    }

    @Child(name = "value", order = 0)
    private String value;
    public String getValue() {
        return value;
    }
    public Id setValue(String value) {
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
