package org.opencds.cqf.ruler.cr.r4.helper;

import java.util.HashMap;
import java.util.Map;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.ChargeItemDefinition;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CompartmentDefinition;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.EffectEvidenceSynthesis;
import org.hl7.fhir.r4.model.EventDefinition;
import org.hl7.fhir.r4.model.Evidence;
import org.hl7.fhir.r4.model.EvidenceVariable;
import org.hl7.fhir.r4.model.ExampleScenario;
import org.hl7.fhir.r4.model.GraphDefinition;
import org.hl7.fhir.r4.model.ImplementationGuide;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MessageDefinition;
import org.hl7.fhir.r4.model.NamingSystem;
import org.hl7.fhir.r4.model.OperationDefinition;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.ResearchDefinition;
import org.hl7.fhir.r4.model.ResearchElementDefinition;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.RiskEvidenceSynthesis;
import org.hl7.fhir.r4.model.SearchParameter;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.StructureMap;
import org.hl7.fhir.r4.model.TerminologyCapabilities;
import org.hl7.fhir.r4.model.TestScript;
import org.hl7.fhir.r4.model.ValueSet;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class ResourceClassMapHelper {
  public static Map<ResourceType, Class<? extends IBaseResource>> resourceTypeToClass  = new HashMap<ResourceType, Class<? extends IBaseResource>>() {{
		put(ResourceType.ActivityDefinition, ActivityDefinition.class);
		put(ResourceType.CapabilityStatement, CapabilityStatement.class);
		put(ResourceType.ChargeItemDefinition, ChargeItemDefinition.class);
		put(ResourceType.CompartmentDefinition, CompartmentDefinition.class);
		put(ResourceType.ConceptMap, ConceptMap.class);
		put(ResourceType.EffectEvidenceSynthesis, EffectEvidenceSynthesis.class);
		put(ResourceType.EventDefinition, EventDefinition.class);
		put(ResourceType.Evidence, Evidence.class);
		put(ResourceType.EvidenceVariable, EvidenceVariable.class);
		put(ResourceType.ExampleScenario, ExampleScenario.class);
		put(ResourceType.GraphDefinition, GraphDefinition.class);
		put(ResourceType.ImplementationGuide, ImplementationGuide.class);
		put(ResourceType.Library, Library.class);
		put(ResourceType.Measure, Measure.class);
		put(ResourceType.MessageDefinition, MessageDefinition.class);
		put(ResourceType.NamingSystem, NamingSystem.class);
		put(ResourceType.OperationDefinition, OperationDefinition.class);
		put(ResourceType.PlanDefinition, PlanDefinition.class);
		put(ResourceType.Questionnaire, Questionnaire.class);
		put(ResourceType.ResearchDefinition, ResearchDefinition.class);
		put(ResourceType.ResearchElementDefinition, ResearchElementDefinition.class);
		put(ResourceType.RiskEvidenceSynthesis, RiskEvidenceSynthesis.class);
		put(ResourceType.SearchParameter, SearchParameter.class);
		put(ResourceType.StructureDefinition, StructureDefinition.class);
		put(ResourceType.StructureMap, StructureMap.class);
		put(ResourceType.TerminologyCapabilities, TerminologyCapabilities.class);
		put(ResourceType.TestScript, TestScript.class);
		put(ResourceType.ValueSet, ValueSet.class);
		put(ResourceType.CodeSystem, CodeSystem.class);
	}};
  public static Class<? extends IBaseResource> getClass(String resourceType) throws UnprocessableEntityException {
    ResourceType type = null;
    try {
      type = ResourceType.fromCode(resourceType);
    } catch (FHIRException e) {
      throw new UnprocessableEntityException("Could not find resource type : " + resourceType);
    }
    return resourceTypeToClass.get(type);
  } 
}
