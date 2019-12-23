package org.opencds.cqf.dstu3.builders;

import org.opencds.cqf.common.builders.BaseBuilder;
import org.hl7.fhir.dstu3.model.Period;

import java.util.Date;

public class PeriodBuilder extends BaseBuilder<Period> {

    public PeriodBuilder() {
        super(new Period());
    }

    public PeriodBuilder buildStart(Date start) {
        complexProperty.setStart(start);
        return this;
    }

    public PeriodBuilder buildEnd(Date end) {
        complexProperty.setEnd(end);
        return this;
    }
}
