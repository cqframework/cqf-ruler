package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveEncounterPerformed", profile="TODO")
public class PositiveEncounterPerformed extends EncounterPerformed {
    @Override
    public PositiveEncounterPerformed copy() {
        PositiveEncounterPerformed retVal = new PositiveEncounterPerformed();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.admissionSource = admissionSource;
        retVal.relevantPeriod = relevantPeriod;
        retVal.dischargeDisposition = dischargeDisposition;
        retVal.diagnoses = diagnoses;
        retVal.facilityLocation = facilityLocation;
        retVal.principalDiagnosis = principalDiagnosis;
        retVal.negationRationale = negationRationale;
        retVal.lengthOfStay = lengthOfStay;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "PositiveEncounterPerformed";
    }
}
