package org.opencds.cqf.ruler.cdshooks.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.JsonParser;
import ca.uhn.fhir.parser.LenientErrorHandler;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.hl7.fhir.dstu3.model.*;
import org.opencds.cqf.ruler.cdshooks.request.CdsHooksRequest;

import java.util.*;

import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newParameters;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newPart;

public class CdsHooksResources {

    /*

    Cases:
        remote server with prefetch (useServerData = false)
            the least simple case
            resolve data missing from prefetch into bundle -> call library/$evaluate specifying remote endpoint
        remote server without prefetch
            call library/$evaluate specifying remote endpoint
        no remote server with prefetch
            resolve data missing from prefetch into bundle -> call library/$evaluate
        no remote server without prefetch
            the simplest case
            call library/$evaluate

    */

    private static final JsonParser parser = new JsonParser(FhirContext.forDstu3(), new LenientErrorHandler());

    // NOTE: Making an assumption here that the parameter in the CQL will be named "ContextPrescriptions"
    public static Parameters getParameters(JsonObject contextResources) {
        Parameters parameters = newParameters();
        Bundle contextBundle = parser.parseResource(Bundle.class, contextResources.toString());
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
                    resource = (Resource) parser.parseResource(
                            entry.getValue().getAsJsonObject().toString());
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
