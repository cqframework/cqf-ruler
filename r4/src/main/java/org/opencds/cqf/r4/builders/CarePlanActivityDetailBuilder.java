package org.opencds.cqf.r4.builders;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.SimpleQuantity;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.common.builders.BaseBuilder;

public class CarePlanActivityDetailBuilder extends BaseBuilder<CarePlan.CarePlanActivityDetailComponent> {

    public CarePlanActivityDetailBuilder() {
        super(new CarePlan.CarePlanActivityDetailComponent());
    }

    public CarePlanActivityDetailBuilder buildKind(String category) {
        complexProperty.setKind(CarePlan.CarePlanActivityKind.fromCode(category));
        return this;
    }

    public CarePlanActivityDetailBuilder buildInstantiatesCanonical(List<CanonicalType> canonicals) {
        complexProperty.setInstantiatesCanonical(canonicals);
        return this;
    }

    public CarePlanActivityDetailBuilder buildCode(CodeableConcept code) {
        complexProperty.setCode(code);
        return this;
    }

    public CarePlanActivityDetailBuilder buildReasonCode(List<CodeableConcept> concepts) {
        complexProperty.setReasonCode(concepts);
        return this;
    }

    public CarePlanActivityDetailBuilder buildReasonCode(CodeableConcept concept) {
        if (!complexProperty.hasReasonCode()) {
            complexProperty.setReasonCode(new ArrayList<>());
        }

        complexProperty.addReasonCode(concept);
        return this;
    }

    public CarePlanActivityDetailBuilder buildReasonReference(List<Reference> references) {
        complexProperty.setReasonReference(references);
        return this;
    }

    public CarePlanActivityDetailBuilder buildReasonReference(Reference reference) {
        if (!complexProperty.hasReasonReference()) {
            complexProperty.setReasonReference(new ArrayList<>());
        }

        complexProperty.addReasonReference(reference);
        return this;
    }

    public CarePlanActivityDetailBuilder buildGoal(List<Reference> goals) {
        complexProperty.setGoal(goals);
        return this;
    }

    public CarePlanActivityDetailBuilder buildGoal(Reference goal) {
        if (!complexProperty.hasGoal()) {
            complexProperty.setGoal(new ArrayList<>());
        }

        complexProperty.addGoal(goal);
        return this;
    }

    // required
    public CarePlanActivityDetailBuilder buildStatus(CarePlan.CarePlanActivityStatus status) {
        complexProperty.setStatus(status);
        return this;
    }

    // String overload
    public CarePlanActivityDetailBuilder buildStatus(String status) throws FHIRException {
        complexProperty.setStatus(CarePlan.CarePlanActivityStatus.fromCode(status));
        return this;
    }

    public CarePlanActivityDetailBuilder buildStatusReason(CodeableConcept reason) {
        complexProperty.setStatusReason(reason);
        return this;
    }

    public CarePlanActivityDetailBuilder buildDoNotPerform(boolean doNotPerform) {
        complexProperty.setDoNotPerform(doNotPerform);
        return this;
    }

    // Type is one of the following: Timing, Period, or String
    public CarePlanActivityDetailBuilder buildScheduled(Type type) {
        complexProperty.setScheduled(type);
        return this;
    }

    public CarePlanActivityDetailBuilder buildLocation(Reference location) {
        complexProperty.setLocation(location);
        return this;
    }

    public CarePlanActivityDetailBuilder buildPerformer(List<Reference> performers) {
        complexProperty.setPerformer(performers);
        return this;
    }

    public CarePlanActivityDetailBuilder buildPerformer(Reference performer) {
        if (!complexProperty.hasPerformer()) {
            complexProperty.setPerformer(new ArrayList<>());
        }

        complexProperty.addPerformer(performer);
        return this;
    }

    // Type is one of the following: CodeableConcept or Reference
    public CarePlanActivityDetailBuilder buildProduct(Type type) {
        complexProperty.setProduct(type);
        return this;
    }

    public CarePlanActivityDetailBuilder buildDailyAmount(SimpleQuantity amount) {
        complexProperty.setDailyAmount(amount);
        return this;
    }

    public CarePlanActivityDetailBuilder buildQuantity(SimpleQuantity quantity) {
        complexProperty.setQuantity(quantity);
        return this;
    }

    public CarePlanActivityDetailBuilder buildDescription(String description) {
        complexProperty.setDescription(description);
        return this;
    }
}
