package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.instance.model.api.ICompositeType;

@DatatypeDef(name="FacilityLocation")
public class FacilityLocation extends Type implements ICompositeType {

    @Child(name = "code", order = 0, min = 1)
    private Coding code;
    public Coding getCode() {
        return code;
    }
    public FacilityLocation setCode(Coding code) {
        this.code = code;
        return this;
    }

    @Child(name = "locationPeriod", order = 1)
    private Period locationPeriod = new Period();
    public Period getLocationPeriod() {
        return locationPeriod;
    }
    public FacilityLocation setLocationPeriod(Period locationPeriod) {
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
