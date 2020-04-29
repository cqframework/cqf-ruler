package org.opencds.cqf.r4.builders;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.common.builders.BaseBuilder;

public class CarePlanBuilder extends BaseBuilder<CarePlan> {

    public CarePlanBuilder() {
        super(new CarePlan());
    }

    public CarePlanBuilder(CarePlan carePlan) {
        super(carePlan);
    }

    public CarePlanBuilder buildIdentifier(List<Identifier> identifiers) {
        complexProperty.setIdentifier(identifiers);
        return this;
    }

    public CarePlanBuilder buildIdentifier(Identifier identifier) {
        if (!complexProperty.hasIdentifier()) {
            complexProperty.setIdentifier(new ArrayList<>());
        }

        complexProperty.addIdentifier(identifier);
        return this;
    }

    public CarePlanBuilder buildInstantiatesCanonical(List<CanonicalType> references) {
        complexProperty.setInstantiatesCanonical(references);
        return this;
    }

    public CarePlanBuilder buildInstantiatesCanonical(String reference) {
        if (!complexProperty.hasInstantiatesCanonical()) {
            complexProperty.setInstantiatesCanonical(new ArrayList<>());
        }

        complexProperty.addInstantiatesCanonical(reference);
        return this;
    }

    public CarePlanBuilder buildBasedOn(List<Reference> references) {
        complexProperty.setBasedOn(references);
        return this;
    }

    public CarePlanBuilder buildBasedOn(Reference reference) {
        if (!complexProperty.hasBasedOn()) {
            complexProperty.setBasedOn(new ArrayList<>());
        }

        complexProperty.addBasedOn(reference);
        return this;
    }

    public CarePlanBuilder buildReplaces(List<Reference> references) {
        complexProperty.setReplaces(references);
        return this;
    }

    public CarePlanBuilder buildReplaces(Reference reference) {
        if (!complexProperty.hasReplaces()) {
            complexProperty.setReplaces(new ArrayList<>());
        }

        complexProperty.addReplaces(reference);
        return this;
    }

    public CarePlanBuilder buildPartOf(List<Reference> references) {
        complexProperty.setPartOf(references);
        return this;
    }

    public CarePlanBuilder buildPartOf(Reference reference) {
        if (!complexProperty.hasPartOf()) {
            complexProperty.setPartOf(new ArrayList<>());
        }

        complexProperty.addPartOf(reference);
        return this;
    }

    // required
    public CarePlanBuilder buildStatus(CarePlan.CarePlanStatus status) {
        complexProperty.setStatus(status);
        return this;
    }

    // String overload
    public CarePlanBuilder buildStatus(String status) throws FHIRException {
        complexProperty.setStatus(CarePlan.CarePlanStatus.fromCode(status));
        return this;
    }

    // required
    public CarePlanBuilder buildIntent(CarePlan.CarePlanIntent intent) {
        complexProperty.setIntent(intent);
        return this;
    }

    // String overload
    public CarePlanBuilder buildIntent(String intent) throws FHIRException {
        complexProperty.setIntent(CarePlan.CarePlanIntent.fromCode(intent));
        return this;
    }

    public CarePlanBuilder buildCategory(List<CodeableConcept> categories) {
        complexProperty.setCategory(categories);
        return this;
    }

    public CarePlanBuilder buildCategory(CodeableConcept category) {
        if (!complexProperty.hasCategory()) {
            complexProperty.setCategory(new ArrayList<>());
        }

        complexProperty.addCategory(category);
        return this;
    }

    public CarePlanBuilder buildTitle(String title) {
        complexProperty.setTitle(title);
        return this;
    }

    public CarePlanBuilder buildDescription(String description) {
        complexProperty.setDescription(description);
        return this;
    }

    // required
    public CarePlanBuilder buildSubject(Reference reference) {
        complexProperty.setSubject(reference);
        return this;
    }

    public CarePlanBuilder buildEncounter(Reference reference) {
        complexProperty.setEncounter(reference);
        return this;
    }

    public CarePlanBuilder buildPeriod(Period period) {
        complexProperty.setPeriod(period);
        return this;
    }

    public CarePlanBuilder buildAuthor(Reference reference) {
        complexProperty.setAuthor(reference);
        return this;
    }

    public CarePlanBuilder buildCareTeam(List<Reference> careTeams) {
        complexProperty.setCareTeam(careTeams);
        return this;
    }

    public CarePlanBuilder buildCareTeam(Reference careTeam) {
        if (!complexProperty.hasCareTeam()) {
            complexProperty.setCareTeam(new ArrayList<>());
        }

        complexProperty.addCareTeam(careTeam);
        return this;
    }

    public CarePlanBuilder buildAddresses(List<Reference> addresses) {
        complexProperty.setAddresses(addresses);
        return this;
    }

    public CarePlanBuilder buildAddresses(Reference address) {
        if (!complexProperty.hasAddresses()) {
            complexProperty.setAddresses(new ArrayList<>());
        }

        complexProperty.addAddresses(address);
        return this;
    }

    public CarePlanBuilder buildSupportingInfo(List<Reference> supportingInfo) {
        complexProperty.setSupportingInfo(supportingInfo);
        return this;
    }

    public CarePlanBuilder buildSupportingInfo(Reference supportingInfo) {
        if (!complexProperty.hasSupportingInfo()) {
            complexProperty.setSupportingInfo(new ArrayList<>());
        }

        complexProperty.addSupportingInfo(supportingInfo);
        return this;
    }

    public CarePlanBuilder buildGoal(List<Reference> goals) {
        complexProperty.setGoal(goals);
        return this;
    }

    public CarePlanBuilder buildGoal(Reference goal) {
        if (!complexProperty.hasGoal()) {
            complexProperty.setGoal(new ArrayList<>());
        }

        complexProperty.addGoal(goal);
        return this;
    }

    public CarePlanBuilder buildActivity(List<CarePlan.CarePlanActivityComponent> activities) {
        complexProperty.setActivity(activities);
        return this;
    }

    public CarePlanBuilder buildActivity(CarePlan.CarePlanActivityComponent activity) {
        complexProperty.addActivity(activity);
        return this;
    }

    public CarePlanBuilder buildNotes(List<Annotation> notes) {
        complexProperty.setNote(notes);
        return this;
    }

    public CarePlanBuilder buildNotes(Annotation note) {
        if (!complexProperty.hasNote()) {
            complexProperty.setNote(new ArrayList<>());
        }

        complexProperty.addNote(note);
        return this;
    }

    public CarePlanBuilder buildLanguage(String language) {
        complexProperty.setLanguage(language);
        return this;
    }

    public CarePlanBuilder buildContained(Resource result) {
        complexProperty.addContained(result);
        return this;
    }
}
