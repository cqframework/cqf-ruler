package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelInfoLoader;
import org.cqframework.cql.cql2elm.ModelInfoProvider;
import org.cqframework.cql.cql2elm.ModelManager;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.elm_modelinfo.r1.ModelInfo;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.builders.CarePlanBuilder;
import org.opencds.cqf.builders.JavaDateBuilder;
import org.opencds.cqf.config.STU3LibraryLoader;
import org.opencds.cqf.config.STU3LibrarySourceProvider;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.cql.runtime.DateTime;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

public class FHIRPlanDefinitionResourceProvider extends JpaResourceProviderDstu3<PlanDefinition> {

    private JpaDataProvider provider;
    private CqlExecutionProvider executionProvider;

    public FHIRPlanDefinitionResourceProvider(Collection<IResourceProvider> providers) {
        this.provider = new JpaDataProvider(providers);
        this.executionProvider = new CqlExecutionProvider(providers);
    }

    private ModelManager modelManager;
    private ModelManager getModelManager() {
        if (modelManager == null) {
            modelManager = new ModelManager();
            ModelInfoProvider infoProvider = () -> {
                Path p = Paths.get("src/main/resources/cds/OMTK-modelinfo-0.1.0.xml").toAbsolutePath();
                return JAXB.unmarshal(new File(p.toString()), ModelInfo.class);
            };
            ModelInfoLoader.registerModelInfoProvider(new VersionedIdentifier().withId("OMTK").withVersion("0.1.0"), infoProvider);
        }
        return modelManager;
    }

    private LibraryManager libraryManager;
    private LibraryManager getLibraryManager() {
        if (libraryManager == null) {
            libraryManager = new LibraryManager(getModelManager());
            libraryManager.getLibrarySourceLoader().clearProviders();
            libraryManager.getLibrarySourceLoader().registerProvider(getLibrarySourceProvider());
            ModelInfoProvider infoProvider = () -> {
                Path p = Paths.get("src/main/resources/cds/OMTK-modelinfo-0.1.0.xml").toAbsolutePath();
                return JAXB.unmarshal(new File(p.toString()), ModelInfo.class);
            };
            ModelInfoLoader.registerModelInfoProvider(new VersionedIdentifier().withId("OMTK").withVersion("0.1.0"), infoProvider);
        }
        return libraryManager;
    }

    private LibraryLoader libraryLoader;
    private LibraryLoader getLibraryLoader() {
        if (libraryLoader == null) {
            libraryLoader = new STU3LibraryLoader(getLibraryResourceProvider(), getLibraryManager(), getModelManager());
        }
        return libraryLoader;
    }

    private STU3LibrarySourceProvider librarySourceProvider;
    private STU3LibrarySourceProvider getLibrarySourceProvider() {
        if (librarySourceProvider == null) {
            librarySourceProvider = new STU3LibrarySourceProvider(getLibraryResourceProvider());
        }
        return librarySourceProvider;
    }

    private LibraryResourceProvider getLibraryResourceProvider() {
        return (LibraryResourceProvider)provider.resolveResourceProvider("Library");
    }

    @Operation(name = "$apply", idempotent = true)
    public CarePlan apply(@IdParam IdType theId,
                          @RequiredParam(name="patient") String patientId,
                          @OptionalParam(name="encounter") String encounterId,
                          @OptionalParam(name="practitioner") String practitionerId,
                          @OptionalParam(name="organization") String organizationId,
                          @OptionalParam(name="userType") String userType,
                          @OptionalParam(name="userLanguage") String userLanguage,
                          @OptionalParam(name="userTaskContext") String userTaskContext,
                          @OptionalParam(name="setting") String setting,
                          @OptionalParam(name="settingContext") String settingContext)
            throws IOException, JAXBException, FHIRException
    {
        PlanDefinition planDefinition = this.getDao().read(theId);

        if (planDefinition == null) {
            throw new IllegalArgumentException("Couldn't find PlanDefintion " + theId);
        }

        CarePlanBuilder builder = new CarePlanBuilder();

        builder
                .buildDefinition(new Reference(planDefinition.getIdElement().getIdPart()))
                .buildSubject(new Reference(patientId))
                .buildStatus(CarePlan.CarePlanStatus.DRAFT);

        if (encounterId != null) builder.buildContext(new Reference(encounterId));
        if (practitionerId != null) builder.buildAuthor(new Reference(practitionerId));
        if (organizationId != null) builder.buildAuthor(new Reference(organizationId));
        if (userLanguage != null) builder.buildLanguage(userLanguage);

        return resolveActions(planDefinition, builder, patientId);
    }

    private CarePlan resolveActions(PlanDefinition planDefinition, CarePlanBuilder builder,
                                        String patientId) throws FHIRException
    {
        for (PlanDefinition.PlanDefinitionActionComponent action : planDefinition.getAction())
        {
            // TODO - Apply input/output dataRequirements?

            if (meetsConditions(planDefinition, patientId, action)) {
                return resolveDynamicValues(planDefinition, builder.build(), patientId, action);
            }
        }

        return builder.build();
    }

    private Boolean meetsConditions(PlanDefinition planDefinition, String patientId,
                                        PlanDefinition.PlanDefinitionActionComponent action)
    {
        for (PlanDefinition.PlanDefinitionActionConditionComponent condition: action.getCondition()) {
            // TODO start
            // TODO stop
            if (condition.getKind() == PlanDefinition.ActionConditionKind.APPLICABILITY) {
                if (!condition.getLanguage().equals("text/cql")) {
                    // TODO - log this
                    continue;
                }

                if (!condition.hasExpression()) {
                    // TODO - log this
                    continue;
                }

                String cql = condition.getExpression();
                Object result = executionProvider.evaluateInContext(planDefinition, cql, patientId);

                if (!(result instanceof Boolean)) {
                    // TODO - log this
                    // maybe try an int value check (i.e. 0 or 1)?
                    continue;
                }

                if (!(Boolean) result) {
                    return false;
                }
            }
        }

        return true;
    }

    private CarePlan resolveDynamicValues(PlanDefinition planDefinition, CarePlan carePlan, String patientId,
                                          PlanDefinition.PlanDefinitionActionComponent action) throws FHIRException
    {
        for (PlanDefinition.PlanDefinitionActionDynamicValueComponent dynamicValue: action.getDynamicValue())
        {
            if (dynamicValue.hasExpression()) {
                Object result =
                        executionProvider
                                .evaluateInContext(planDefinition, dynamicValue.getExpression(), patientId);

                if (dynamicValue.hasPath() && dynamicValue.getPath().equals("$this"))
                {
                    carePlan = (CarePlan) result;
                }

                else {

                    if (result instanceof DateTime) {
                        result =
                                new JavaDateBuilder()
                                        .buildFromDateTime((DateTime) result)
                                        .build();
                    }

                    // TODO - set the property in the CarePlan
                    // Want something like this:
                    // carePlan.setProperty(dynamicValue.getPath(), (Base) result);
                }
            }
        }

        return carePlan;
    }
}
