package org.opencds.cqf.operations;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.TriggerDefinition;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.execution.LibraryLoader;

import java.time.Instant;
import java.util.*;

/**
 * Created by Christopher Schuler on 6/6/2017.
 */
public class PlanDefinitionApply {

    // Official $apply parameters
    private String encounterId = "";
    private String practitionerId = "";
    private String organizationId = "";
    private String userType = "";
    private String userLanguage = "";
    private String userTaskContext = "";
    private String setting = "";
    private String settingContext = "";

    // Structure content
    private String status = "";
    private Period effectivePeriod = new Period();
    private Library library = new Library();
    private List<Map<String, Object> > actions = new ArrayList<>();
    private String triggerEvent = "";

    private PlanDefinition planDefinition = new PlanDefinition();
    private LibraryLoader libraryLoader;

    public PlanDefinitionApply(LibraryLoader libraryLoader) {
        this.libraryLoader = libraryLoader;
    }

    public PlanDefinitionApply(LibraryLoader libraryLoader, PlanDefinition planDefinition) {
        this.libraryLoader = libraryLoader;
        this.planDefinition = planDefinition;
    }

    public PlanDefinition getPlanDefinition() {
        return this.planDefinition;
    }

    public PlanDefinitionApply setPlanDefinition(PlanDefinition planDefinition) {
        this.planDefinition = planDefinition;
        return this;
    }

    public boolean resolveStatus() {
        this.status = planDefinition.getStatus().toCode();
        return status.toLowerCase().equals("unknown");
    }

    public boolean resolveEffectivePeriod() {
        if (planDefinition.hasEffectivePeriod()) {
            this.effectivePeriod = planDefinition.getEffectivePeriod();
            Date start = this.effectivePeriod.getStart();
            Date end = this.effectivePeriod.getEnd();

            return Date.from(Instant.now()).before(end) && Date.from(Instant.now()).after(start);
        }

        return true;
    }

    public void resolveLibrary(LibraryResourceProvider provider) {
        if (planDefinition.hasLibrary()) {
            // Assuming a single library reference
            // TODO: handle multiple library references
            String id = planDefinition.getLibraryFirstRep().getId();
            this.library = libraryLoader.load(new VersionedIdentifier().withId(id));
            return;
        }

        throw new IllegalArgumentException("Could not resolve the PlanDefinition library reference.");
    }

    public void resolveActions() {
        for (PlanDefinition.PlanDefinitionActionComponent action : planDefinition.getAction()) {
            resolveCondition(action);
            resolveTriggerDefinition(action);
        }
    }

    public void resolveTriggerDefinition(PlanDefinition.PlanDefinitionActionComponent action) {
        for (TriggerDefinition trigger : action.getTriggerDefinition()) {
            String triggerType = trigger.getType().toCode();

            // TODO: resolve other trigger types
            if (triggerType.toLowerCase().equals("named-event") && trigger.hasEventName()) {
                this.triggerEvent = trigger.getEventName();
            }
        }
    }

    public void resolveTitle() {
        switch (this.triggerEvent.toLowerCase()) {
            case "medication-prescribe":

        }
    }

    public void resolveCondition(PlanDefinition.PlanDefinitionActionComponent action) {
        for (PlanDefinition.PlanDefinitionActionConditionComponent condition : action.getCondition()) {
            if (condition.getKind().toCode().equals("applicability") && condition.hasExpression()) {
                Context context = new Context(this.library);
                Object result = context
                        .resolveExpressionRef(condition.getExpression())
                        .getExpression()
                        .evaluate(context);

                if ((Boolean) result) {
                    // TODO
                }
            }
        }
    }
}
