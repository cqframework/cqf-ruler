package org.opencds.cqf.ruler.cdshooks.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.JsonParser;
import ca.uhn.fhir.parser.LenientErrorHandler;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.ParameterDefinition;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.opencds.cqf.ruler.cdshooks.request.CdsHooksRequest;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newParameters;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newPart;

public class CdsHooksUtil {

    // NOTE: Making an assumption here that the parameter in the CQL will be named "ContextPrescriptions"
    public static Parameters getParameters(JsonObject contextResources) {
        Parameters parameters = newParameters();
        Bundle contextBundle = new JsonParser(FhirContext.forDstu3Cached(), new LenientErrorHandler())
                .parseResource(Bundle.class, contextResources.toString());
        contextBundle.getEntry().forEach(
                x -> parameters.addParameter(newPart("ContextPrescriptions", x.getResource())));
        if (parameters.getParameter().size() == 1) {
            Extension listExtension = new Extension(
                    "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition",
                    new ParameterDefinition().setMax("*").setName("ContextPrescriptions"));
            parameters.getParameterFirstRep().addExtension(listExtension);
        }
        return parameters;
    }

    public static Map<String, Resource> getResourcesFromBundle(Bundle bundle) {
        // using HashMap to avoid duplicates
        Map<String, Resource> resourceMap = new HashMap<>();
        bundle.getEntry().forEach(
                x -> resourceMap.put(x.fhirType() + x.getResource().getId(), x.getResource()));
        return resourceMap;
    }

    public static Bundle getPrefetchResources(CdsHooksRequest request) {
        // using HashMap to avoid duplicates
        Map<String, Resource> resourceMap = new HashMap<>();
        Bundle prefetchResources = new Bundle();
        Resource resource;
        if (request.prefetch != null) {
            for (Map.Entry<String, JsonElement> entry : request.prefetch.resources.entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    resource = (Resource) new JsonParser(FhirContext.forDstu3Cached(), new LenientErrorHandler())
                            .parseResource(entry.getValue().getAsJsonObject().toString());
                    if (resource instanceof Bundle) {
                        resourceMap.putAll(getResourcesFromBundle((Bundle) resource));
                    } else {
                        resourceMap.put(resource.fhirType() + resource.getId(), resource);
                    }
                }
            }
        }
        else return null;
        resourceMap.forEach((key, value) -> prefetchResources.addEntry().setResource(value));
        return prefetchResources;
    }

    public static List<String> getExpressions(PlanDefinition planDefinition) {
        return getExpressionsFromActions(planDefinition.getAction());
    }

    // Assumption that expressions are using CQL and will be references (text/cql.identifier introduced in FHIR R4)
    private static List<String> getExpressionsFromActions(List<PlanDefinition.PlanDefinitionActionComponent> actions) {
        Set<String> expressions = new HashSet<>();
        if (actions != null) {
            for (PlanDefinition.PlanDefinitionActionComponent action : actions) {
                if (action.hasCondition()) {
                    action.getCondition().forEach(
                            x -> {
                                if (x.hasExpression()) {
                                    expressions.add(x.getExpression());
                                }
                            }
                    );
                }
                if (action.hasDynamicValue()) {
                    action.getDynamicValue().forEach(
                            x -> {
                                if (x.hasExpression()) {
                                    expressions.add(x.getExpression());
                                }
                            }
                    );
                }
                if (action.hasAction()) {
                    expressions.addAll(getExpressionsFromActions(action.getAction()));
                }
            }
        }
        return new ArrayList<>(expressions);
    }
}
