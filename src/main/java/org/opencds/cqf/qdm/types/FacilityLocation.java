package org.opencds.cqf.qdm.types;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.Interval;

@DatatypeDef(name="FacilityLocation")
public class FacilityLocation extends Type implements ICompositeType {

    @Child(name = "code", order = 0, min = 1)
    private Code code;
    public Code getCode() {
        return code;
    }
    public FacilityLocation setNumerator(Code code) {
        this.code = code;
        return this;
    }

    @Child(name = "locationPeriod", order = 1)
    private Interval locationPeriod;
    public Interval getLocationPeriod() {
        return locationPeriod;
    }
    public FacilityLocation setLocationPeriod(Interval locationPeriod) {
        this.locationPeriod = locationPeriod;
        return this;
    }

    @Override
    protected FacilityLocation typedCopy() {
        FacilityLocation retVal = new FacilityLocation();
        super.copyValues(retVal);
        retVal.code = code;
        retVal.locationPeriod = locationPeriod;
        return retVal;
    }
}
