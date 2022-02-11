package org.opencds.cqf.ruler.cdshooks.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.cqframework.cql.elm.execution.Library;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.Resource;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.ruler.cdshooks.hooks.Hook;
import org.opencds.cqf.ruler.cdshooks.providers.ProviderConfiguration;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class Stu3EvaluationContext extends EvaluationContext<PlanDefinition> {

    public Stu3EvaluationContext(Hook hook, IGenericClient fhirClient, Context context, Library library, PlanDefinition planDefinition, ProviderConfiguration providerConfiguration, ModelResolver modelResolver) {
        super(hook, FhirVersionEnum.DSTU3, fhirClient, context, library, planDefinition, providerConfiguration, modelResolver);
    }

    @Override
    List<Object> applyCqlToResources(List<Object> resources) {
        if (resources == null || resources.isEmpty()) {
            return new ArrayList<>();
        }

        Bundle bundle = new Bundle();
        for (Object res : resources) {
            bundle.addEntry(new Bundle.BundleEntryComponent().setResource((Resource) res));
        }
        Parameters parameters = new Parameters();
        parameters.addParameter().setName("resourceBundle").setResource(bundle);

        Parameters ret = this.getSystemFhirClient().operation().onType(Bundle.class).named("$apply-cql")
                .withParameters(parameters).execute();
        Bundle appliedResources = (Bundle) ret.getParameter().get(0).getResource();
        return appliedResources.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)
                .collect(Collectors.toList());
    }
}
