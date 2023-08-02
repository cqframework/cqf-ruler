package org.opencds.cqf.ruler.cdshooks.r4;

import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.part;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.ParameterDefinition;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.ruler.cdshooks.request.CdsHooksRequest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.JsonParser;
import ca.uhn.fhir.parser.LenientErrorHandler;

public class CdsHooksUtil {

	private CdsHooksUtil() {
	}

	// NOTE: Making an assumption here that the parameter in the CQL will be named
	// "ContextPrescriptions"
	public static Parameters getParameters(JsonObject contextResources) {
		Parameters parameters = parameters();
		Bundle contextBundle = new JsonParser(FhirContext.forR4Cached(), new LenientErrorHandler())
				.parseResource(Bundle.class, contextResources.toString());
		contextBundle.getEntry().forEach(
				x -> parameters.addParameter(part("ContextPrescriptions", x.getResource())));
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
					resource = (Resource) new JsonParser(FhirContext.forR4Cached(), new LenientErrorHandler())
							.parseResource(entry.getValue().getAsJsonObject().toString());
					if (resource instanceof Bundle) {
						resourceMap.putAll(getResourcesFromBundle((Bundle) resource));
					} else {
						resourceMap.put(resource.fhirType() + resource.getId(), resource);
					}
				}
			}
		} else
			return null;
		resourceMap.forEach((key, value) -> prefetchResources.addEntry().setResource(value));
		return prefetchResources;
	}

	public static List<String> getExpressions(PlanDefinition planDefinition) {
		return getExpressionsFromActions(planDefinition.getAction());
	}

	private static List<String> getExpressionsFromActions(List<PlanDefinition.PlanDefinitionActionComponent> actions) {
		Set<String> expressions = new HashSet<>();
		if (actions != null) {
			for (PlanDefinition.PlanDefinitionActionComponent action : actions) {
				if (action.hasCondition()) {
					action.getCondition().forEach(
							x -> {
								if (!x.hasExpression()) {
									return;
								}

								var lang = x.getExpression().getLanguage();
								if ("text/cql-identifier".equals(lang) || "text/cql.identifier".equals(lang)) {
									expressions.add(x.getExpression().getExpression());
								}
							});
				}
				if (action.hasDynamicValue()) {
					action.getDynamicValue().forEach(
							x -> {
								if (!x.hasExpression()) {
									return;
								}

								var lang = x.getExpression().getLanguage();
								if ("text/cql-identifier".equals(lang) || "text/cql.identifier".equals(lang)) {
									expressions.add(x.getExpression().getExpression());
								}
							});
				}
				if (action.hasAction()) {
					expressions.addAll(getExpressionsFromActions(action.getAction()));
				}
			}
		}
		return new ArrayList<>(expressions);
	}
}
