package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.qdm.conversion.providers.QdmAdapter;

public abstract class QdmBaseType extends DomainResource {

    @Child(name="theId", order=0)
    private Id id;
    public Id getTheId() {
        return id;
    }
    public QdmBaseType setId(Id id) {
        this.id = id;
        return this;
    }

    @Child(name="code", order=1)
    private Coding code;
    public Code getCode() {
        return QdmAdapter.createCode(code);
    }
    public QdmBaseType setCode(Coding code) {
        this.code = code;
        return this;
    }

    @Child(name="patientId", order=2)
    private Id patientId;
    public Id getPatientId() {
        return patientId;
    }
    public QdmBaseType setPatientId(Id patientId) {
        this.patientId = patientId;
        return this;
    }

    @Child(name="reporter", order=3)
    private Id reporter;
    public Id getReporter() {
        return reporter;
    }
    public QdmBaseType setReporter(Id reporter) {
        this.reporter = reporter;
        return this;
    }

    @Child(name="recorder", order=4)
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
