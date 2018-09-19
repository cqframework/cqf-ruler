package org.opencds.cqf.qdm;

import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.qdm.types.Id;

public abstract class QdmBaseType extends DomainResource {

    private Id id;
    public Id getTheId() {
        return id;
    }
    public QdmBaseType setId(Id id) {
        this.id = id;
        return this;
    }

    private Code code;
    public Code getCode() {
        return code;
    }
    public QdmBaseType setCode(Code code) {
        this.code = code;
        return this;
    }

    private Id patientId;
    public Id getPatientId() {
        return patientId;
    }
    public QdmBaseType setPatientId(Id patientId) {
        this.patientId = patientId;
        return this;
    }

    private Id reporter;
    public Id getReporter() {
        return reporter;
    }
    public QdmBaseType setReporter(Id reporter) {
        this.reporter = reporter;
        return this;
    }

    private Id recorder;
    public Id getRecorder() {
        return recorder;
    }
    public QdmBaseType setRecorder(Id recorder) {
        this.recorder = recorder;
        return this;
    }

    @Override
    public abstract QdmBaseType copy();

    @Override
    public abstract ResourceType getResourceType();

    public abstract String getResourceName();
}
