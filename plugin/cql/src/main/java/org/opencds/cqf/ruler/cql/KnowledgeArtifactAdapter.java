package org.opencds.cqf.ruler.cql;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.RelatedArtifact;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class KnowledgeArtifactAdapter<T extends MetadataResource> {
	protected T resource;

	public KnowledgeArtifactAdapter(T resource) {
		this.resource = resource;
	}

	public Date getApprovalDate() {
		switch (resource.getClass().getSimpleName()) {
			case "ActivityDefinition":
				return ((ActivityDefinition) resource).getApprovalDate();
			case "Library":
				return ((Library) resource).getApprovalDate();
			case "Measure":
				return ((Measure) resource).getApprovalDate();
			case "PlanDefinition":
				return ((PlanDefinition) resource).getApprovalDate();
			default: return null;
		}
	}

	public MetadataResource setApprovalDate(Date theApprovalDate) {
		System.out.println("getName: " + resource.getName());
		switch (resource.getName()) {
			case "ActivityDefinition":
				((ActivityDefinition) resource).setApprovalDate(theApprovalDate);
			case "Library":
				((Library) resource).setApprovalDate(theApprovalDate);
			case "Measure":
				((Measure) resource).setApprovalDate(theApprovalDate);
			case "PlanDefinition":
				((PlanDefinition) resource).setApprovalDate(theApprovalDate);
		}
		return resource;
	}

	public java.util.List<RelatedArtifact> getRelatedArtifact() {
		switch (resource.getClass().getSimpleName()) {
			case "Library":
				return ((Library) resource).getRelatedArtifact();
			case "PlanDefinition":
				return ((PlanDefinition) resource).getRelatedArtifact();
			default:
				return new ArrayList<>();
		}
	}

	public MetadataResource setRelatedArtifact(List<RelatedArtifact> theRelatedArtifact) {
		System.out.println("getName: " + resource.getName());
		switch (resource.getName()) {
			case "Library":
				((Library) resource).setRelatedArtifact(theRelatedArtifact);
			case "PlanDefinition":
				((PlanDefinition) resource).setRelatedArtifact(theRelatedArtifact);
		}
		return resource;
	}

	public List<RelatedArtifact> getComponents() {
		switch (resource.getClass().getSimpleName()) {
			case "Library":
				return getComponentsOfLibrary((Library)resource);
			case "PlanDefinition":
				return getComponentsOfPlanDefinition((PlanDefinition)resource);
			default :
				return new ArrayList<>();
		}
	}

	private List<RelatedArtifact> getComponentsOfLibrary(Library library) {
		List<RelatedArtifact> components = library.getRelatedArtifact().stream()
			.filter(ra -> ra.getType() == RelatedArtifact.RelatedArtifactType.COMPOSEDOF)
			.collect(Collectors.toList());

		return components;
	}

	private List<RelatedArtifact> getComponentsOfPlanDefinition(PlanDefinition planDefinition) {
		List<RelatedArtifact> components = planDefinition.getRelatedArtifact().stream()
			.filter(ra -> ra.getType() == RelatedArtifact.RelatedArtifactType.COMPOSEDOF)
			.collect(Collectors.toList());

		return components;
	}

	public List<RelatedArtifact> getDependencies() {
		switch (resource.getClass().getSimpleName()) {
			case "Library":
				return getDependenciesOfLibrary((Library)resource);
			case "PlanDefinition":
				return getDependenciesOfPlanDefinition((PlanDefinition)resource);
			default :
				return new ArrayList<>();
		}
	}

	private List<RelatedArtifact> getDependenciesOfLibrary(Library library) {
		List<RelatedArtifact> dependencies = library.getRelatedArtifact().stream()
			.filter(ra -> ra.getType() == RelatedArtifact.RelatedArtifactType.DEPENDSON)
			.collect(Collectors.toList());

		return dependencies;
	}

	private List<RelatedArtifact> getDependenciesOfPlanDefinition(PlanDefinition planDefinition) {
		List<RelatedArtifact> dependencies = planDefinition.getRelatedArtifact().stream()
			.filter(ra -> ra.getType() == RelatedArtifact.RelatedArtifactType.DEPENDSON)
			.collect(Collectors.toList());

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
			case "Library":
				return ((Library) resource).copy();
			case "PlanDefinition":
				return ((PlanDefinition) resource).copy();
			default: return resource.copy();
			//TODO: Is calling MetadataResource.copy() the right default behavior?
		}
	}
}
