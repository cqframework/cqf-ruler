package org.opencds.cqf.dstu3.builders;

import java.util.Date;

import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.common.builders.BaseBuilder;
import org.opencds.cqf.cql.engine.runtime.Interval;

public class MeasureReportBuilder extends BaseBuilder<MeasureReport> {

    public MeasureReportBuilder() {
        super(new MeasureReport());
    }

    public MeasureReportBuilder buildStatus(String status) {
        try {
            this.complexProperty.setStatus(MeasureReport.MeasureReportStatus.fromCode(status));
        } catch (FHIRException e) {
            // default to complete
            this.complexProperty.setStatus(MeasureReport.MeasureReportStatus.COMPLETE);
        }
        return this;
    }

    public MeasureReportBuilder buildType(MeasureReport.MeasureReportType type) {
        this.complexProperty.setType(type);
        return this;
    }

    public MeasureReportBuilder buildMeasureReference(String measureRef) {
        this.complexProperty.setMeasure(new Reference(measureRef));
        return this;
    }

    public MeasureReportBuilder buildPatientReference(String patientRef) {
        this.complexProperty.setPatient(new Reference(patientRef));
        return this;
    }

    public MeasureReportBuilder buildPeriod(Interval period) {
        this.complexProperty.setPeriod(new Period().setStart((Date) period.getStart()).setEnd((Date) period.getEnd()));
        return this;
    }
}
