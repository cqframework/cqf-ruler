package org.opencds.cqf.ruler.cdshooks.r4;

import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.part;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.uhn.fhir.util.BundleUtil;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.ParameterDefinition;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Reference;
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
				x -> {
					if (x.getResource().fhirType().toLowerCase().endsWith("request") || x.getResource().fhirType().toLowerCase().endsWith("order")) {
						parameters.addParameter(part("ContextPrescriptions", x.getResource()));
					}
				});
		if (parameters.getParameter().size() == 1) {
			Extension listExtension = new Extension(
					"http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition",
					new ParameterDefinition().setMax("*").setName("ContextPrescriptions"));
			parameters.getParameterFirstRep().addExtension(listExtension);
		}
		return parameters;
	}

	public static void addNonRequestResourcesFromContextToDataBundle(JsonObject contextResources, Bundle data) {
		Bundle contextBundle = new JsonParser(FhirContext.forR4Cached(), new LenientErrorHandler())
			.parseResource(Bundle.class, contextResources.toString());
		contextBundle.getEntry().forEach(
			x -> {
				if (!x.getResource().fhirType().toLowerCase().endsWith("request")
					&& !x.getResource().fhirType().toLowerCase().endsWith("order")
					&& data.getEntry().stream().noneMatch(
						entry -> entry.getResource().getResourceType().equals(x.getResource().getResourceType())
							&& entry.getResource().getIdPart().equals(x.getResource().getIdPart()))) {
						data.addEntry().setResource(x.getResource());
					}

			});
	}

	public static Bundle resolveMedicationReferences(FhirContext fhirContext, Bundle bundle) {
		var newBundle = new Bundle().setType(Bundle.BundleType.SEARCHSET);
		var medications = BundleUtil.toListOfResourcesOfType(fhirContext, bundle, Medication.class);
		bundle.getEntry().forEach(
			entry -> {
				var resource = entry.getResource();
				if (resource instanceof MedicationRequest && ((MedicationRequest) resource).getMedication() instanceof Reference) {
					for (var med : medications) {
						if (med.getIdPart().equals(((MedicationRequest) resource).getMedicationReference().getReference().replace("Medication/", ""))) {
							((MedicationRequest) resource).setMedication(med.getCode());
							break;
						}
					}
				}
				newBundle.addEntry().setResource(resource);
			}
		);
		return newBundle;
	}

	public static Parameters getParameters(List<MedicationRequest> draftOrders) {
		Parameters parameters = parameters();
		draftOrders.forEach(
			x -> parameters.addParameter(part("ContextPrescriptions", x)));
		if (parameters.getParameter().size() == 1) {
			Extension listExtension = new Extension(
				"http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition",
				new ParameterDefinition().setMax("*").setName("ContextPrescriptions"));
			parameters.getParameterFirstRep().addExtension(listExtension);
		}
		return parameters;
	}

	public static List<MedicationRequest> getDraftOrders(JsonObject contextResources) {
		Bundle contextBundle = new JsonParser(FhirContext.forR4Cached(), new LenientErrorHandler())
			.parseResource(Bundle.class, contextResources.toString());
		return BundleUtil.toListOfResourcesOfType(FhirContext.forR4Cached(), contextBundle, MedicationRequest.class);
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
