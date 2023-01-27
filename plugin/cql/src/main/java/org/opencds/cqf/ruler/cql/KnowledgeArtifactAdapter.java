package org.opencds.cqf.ruler.cql;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.RelatedArtifact;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class KnowledgeArtifactAdapter<T extends MetadataResource> {
	protected T resource;

	public KnowledgeArtifactAdapter(T resource) {
		this.resource = resource;
	}

	public List<RelatedArtifact> getRelatedArtifact() {
		switch (resource.getClass().getSimpleName()) {
			case "ActivityDefinition":
				return ((ActivityDefinition) resource).getRelatedArtifact();
			case "Library":
				return ((Library) resource).getRelatedArtifact();
			case "Measure":
				return ((Measure) resource).getRelatedArtifact();
			case "PlanDefinition":
				return ((PlanDefinition) resource).getRelatedArtifact();
			default:
				return new ArrayList<>();
		}
	}

	public MetadataResource setRelatedArtifact(List<RelatedArtifact> theRelatedArtifact) {
		System.out.println("getName: " + resource.getName());
		switch (resource.getName()) {
			case "ActivityDefinition":
				((ActivityDefinition) resource).setRelatedArtifact(theRelatedArtifact);
			case "Library":
				((Library) resource).setRelatedArtifact(theRelatedArtifact);
			case "Measure":
				((Measure) resource).setRelatedArtifact(theRelatedArtifact);
			case "PlanDefinition":
				((PlanDefinition) resource).setRelatedArtifact(theRelatedArtifact);
		}
		return resource;
	}

	public List<RelatedArtifact> getRelatedArtifactsByType(RelatedArtifact.RelatedArtifactType relatedArtifactType) {
		List<RelatedArtifact> relatedArtifacts = getRelatedArtifact().stream()
			.filter(ra -> ra.getType() == relatedArtifactType)
			.collect(Collectors.toList());

		return relatedArtifacts;
	}

	public List<RelatedArtifact> getComponents() {
		switch (resource.getClass().getSimpleName()) {
			case "ActivityDefinition":
			case "Library":
			case "Measure":
			case "PlanDefinition":
				return getComponentsOfKnowledgeArtifact();
			default :
				return new ArrayList<>();
		}
	}

	private List<RelatedArtifact> getComponentsOfKnowledgeArtifact() {
		List<RelatedArtifact> components = getRelatedArtifactsByType(RelatedArtifact.RelatedArtifactType.COMPOSEDOF);
		return components;
	}

	public List<RelatedArtifact> getDependencies() {
		switch (resource.getClass().getSimpleName()) {
			case "ActivityDefinition":
				return getDependenciesOfActivityDefinition();
			case "Library":
				return getDependenciesOfKnowledgeArtifact();
			case "Measure":
				return getDependenciesOfMeasure();
			case "PlanDefinition":
				return getDependenciesOfPlanDefinition();
			default :
				return new ArrayList<>();
		}
	}

	private List<RelatedArtifact> getDependenciesOfKnowledgeArtifact() {
		List<RelatedArtifact> components = getRelatedArtifactsByType(RelatedArtifact.RelatedArtifactType.DEPENDSON);
		return components;
	}

	private List<RelatedArtifact> getDependenciesOfActivityDefinition() {
		List<RelatedArtifact> dependencies = getRelatedArtifactsByType(RelatedArtifact.RelatedArtifactType.DEPENDSON);

		ActivityDefinition activityDefinition = (ActivityDefinition)resource;
		if (activityDefinition.hasLibrary()) {
			for (var library : activityDefinition.getLibrary()) {
				dependencies.add(
					new RelatedArtifact()
						.setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
						.setResource(library.getValueAsString())
				);
			}
		}

		return dependencies;
	}

	private List<RelatedArtifact> getDependenciesOfLibrary() {
		List<RelatedArtifact> dependencies = getRelatedArtifactsByType(RelatedArtifact.RelatedArtifactType.DEPENDSON);
		return dependencies;
	}

	private List<RelatedArtifact> getDependenciesOfMeasure() {
		List<RelatedArtifact> dependencies = getRelatedArtifactsByType(RelatedArtifact.RelatedArtifactType.DEPENDSON);

		Measure measure = (Measure)resource;
		if (measure.hasLibrary()) {
			for (var library : measure.getLibrary()) {
				dependencies.add(
					new RelatedArtifact()
						.setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
						.setResource(library.getValueAsString())
				);
			}
		}

		return dependencies;
	}

	private List<RelatedArtifact> getDependenciesOfPlanDefinition() {
		List<RelatedArtifact> dependencies = getRelatedArtifactsByType(RelatedArtifact.RelatedArtifactType.DEPENDSON);

		PlanDefinition planDefinition = (PlanDefinition)resource;
		if (planDefinition.hasLibrary()) {
			for (var library : planDefinition.getLibrary()) {
				dependencies.add(
					new RelatedArtifact()
						.setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
						.setResource(library.getValueAsString())
				);
			}
		}

		return dependencies;
	}

	public MetadataResource copy() {
		switch (resource.getClass().getName()) {
			case "ActivityDefinition":
				return ((ActivityDefinition) resource).copy();
			case "Library":
				return ((Library) resource).copy();
			case "Measure":
				return ((Measure) resource).copy();
			case "PlanDefinition":
				return ((PlanDefinition) resource).copy();
			default: return resource.copy();
			//TODO: Is calling MetadataResource.copy() the right default behavior?
		}
	}
}
