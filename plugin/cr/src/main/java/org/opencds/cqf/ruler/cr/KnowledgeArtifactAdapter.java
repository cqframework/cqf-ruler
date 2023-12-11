package org.opencds.cqf.ruler.cr;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ContactDetail;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.ruler.utility.SemanticVersion;

public class KnowledgeArtifactAdapter<T extends MetadataResource> {
	protected T resource;

	public KnowledgeArtifactAdapter(T resource) {
		this.resource = resource;
	}
	public KnowledgeArtifactAdapter<MetadataResource> clone() {
		return new KnowledgeArtifactAdapter<MetadataResource>(this.copy());
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
		switch (resource.getClass().getSimpleName()) {
			case "ActivityDefinition":
				((ActivityDefinition) resource).setApprovalDate(theApprovalDate);
				break;
			case "Library":
				((Library) resource).setApprovalDate(theApprovalDate);
				break;
			case "Measure":
				((Measure) resource).setApprovalDate(theApprovalDate);
				break;
			case "PlanDefinition":
				((PlanDefinition) resource).setApprovalDate(theApprovalDate);
				break;
		}
		return resource;
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
		switch (resource.getClass().getSimpleName()) {
			case "ActivityDefinition":
				((ActivityDefinition) resource).setRelatedArtifact(theRelatedArtifact);
				break;
			case "Library":
				((Library) resource).setRelatedArtifact(theRelatedArtifact);
				break;
			case "Measure":
				((Measure) resource).setRelatedArtifact(theRelatedArtifact);
				break;
			case "PlanDefinition":
				((PlanDefinition) resource).setRelatedArtifact(theRelatedArtifact);
				break;
		}
		return resource;
	}

	public Period getEffectivePeriod(){
		switch (resource.getClass().getSimpleName()) {
			case "ActivityDefinition":
				return ((ActivityDefinition) resource).getEffectivePeriod();
			case "Library":
				return ((Library) resource).getEffectivePeriod();
			case "Measure":
				return ((Measure) resource).getEffectivePeriod();
			case "PlanDefinition":
				return ((PlanDefinition) resource).getEffectivePeriod();
			default:
				return new Period();
		}
	}
	public MetadataResource setEffectivePeriod(Period effectivePeriod) {
		switch (resource.getClass().getSimpleName()) {
			case "ActivityDefinition":
				return ((ActivityDefinition) resource).setEffectivePeriod(effectivePeriod);
			case "Library":
				return ((Library) resource).setEffectivePeriod(effectivePeriod);
			case "Measure":
				return ((Measure) resource).setEffectivePeriod(effectivePeriod);
			case "PlanDefinition":
				return ((PlanDefinition) resource).setEffectivePeriod(effectivePeriod);
			default:
				return resource;
		}
	}
	public List<RelatedArtifact> getRelatedArtifactsByType(RelatedArtifact.RelatedArtifactType relatedArtifactType) {
		List<RelatedArtifact> relatedArtifacts = getRelatedArtifact().stream()
			.filter(ra -> ra.getType() == relatedArtifactType)
			.collect(Collectors.toList());

		return relatedArtifacts;
	}

	public List<RelatedArtifact> getOwnedRelatedArtifacts(){
		switch (resource.getClass().getSimpleName()) {
			case "ActivityDefinition":
			case "Library":
			case "Measure":
			case "PlanDefinition":
				return getOwnedRelatedArtifactsOfKnowledgeArtifact();
			default :
				return new ArrayList<>();
		}
	}
	private List<RelatedArtifact> getOwnedRelatedArtifactsOfKnowledgeArtifact() {
		return getRelatedArtifact().stream()
			.filter(KnowledgeArtifactAdapter::checkIfRelatedArtifactIsOwned)
			.collect(Collectors.toList());
	}
	public static Boolean checkIfRelatedArtifactIsOwned(RelatedArtifact ra){
		return ra.getExtension()
					.stream()
					.filter(ext -> ext.getUrl().equals("http://hl7.org/fhir/StructureDefinition/crmi-isOwned"))
					.findAny()
					.map(e -> ((BooleanType) e.getValue()).getValue())
					.orElseGet(()-> false);
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
				return getDependenciesOfLibrary();
			case "Measure":
				return getDependenciesOfMeasure();
			case "PlanDefinition":
				return getDependenciesOfPlanDefinition();
			case "ValueSet":
				return getDependenciesOfValueSet();
			default :
				return new ArrayList<>();
		}
	}

	private List<RelatedArtifact> getDependenciesOfKnowledgeArtifact() {
		List<RelatedArtifact> dependencies = getRelatedArtifactsByType(RelatedArtifact.RelatedArtifactType.DEPENDSON);
		return dependencies;
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
		List<RelatedArtifact> dependencies = getDependenciesOfKnowledgeArtifact();
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

	private List<RelatedArtifact> getDependenciesOfValueSet() {
		List<RelatedArtifact> dependencies = new ArrayList<>();
		ValueSet valueSet = (ValueSet)resource;

		if (valueSet.hasCompose() && valueSet.getCompose().hasInclude()) {
			for (var conceptSet : valueSet.getCompose().getInclude()) {
				if (conceptSet.hasValueSet()) {
					for (var valueSetRef : conceptSet.getValueSet()) {
						dependencies.add(
							new RelatedArtifact()
								.setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
								.setResource(valueSetRef.getValue())
						);
					}
				}
			}
		}

		return dependencies;
	}

	public List<ContactDetail> getEndorser() {
		switch (resource.getClass().getSimpleName()) {
			case "Library":
				return ((Library) resource).getEndorser();
			default:
				return new ArrayList<>();
		}
	}

	public MetadataResource addEndorser(ContactDetail endorser) {
		switch (resource.getClass().getSimpleName()) {
			case "Library":
				return ((Library) resource).addEndorser(endorser);
			default:
				return resource;
		}
	}

	public MetadataResource setEndorser(List<ContactDetail> endorser) {
		switch (resource.getClass().getSimpleName()) {
			case "Library":
				return ((Library) resource).setEndorser(endorser);
			default:
				return resource;
		}
	}

	public MetadataResource updateEndorser(ContactDetail endorser) {
		List<ContactDetail> existingEndorsers = getEndorser();
		if (existingEndorsers != null) {
			Optional<ContactDetail> existingEndorser = existingEndorsers.stream()
					.filter(e -> e.getName().equals(endorser.getName())).findAny();
			if (existingEndorser.isPresent()) {
				int index = existingEndorsers.indexOf(existingEndorser.get());
				existingEndorsers.set(index, endorser);
				return setEndorser(existingEndorsers);
			}
		}
		return addEndorser(endorser);
	}

	public MetadataResource copy() {
		switch (resource.getClass().getSimpleName()) {
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

	public static Optional<MetadataResource> findLatestVersion(List<MetadataResource> resources) {
		Comparator<String> versionComparator = SemanticVersion.getVersionComparator();
		MetadataResource latestResource = null;

		for (MetadataResource resource : resources) {
			String version = resource.getVersion();
			if (latestResource == null || versionComparator.compare(version, latestResource.getVersion()) > 0) {
				latestResource = resource;
			}
		}

		return Optional.ofNullable(latestResource);
	}

	public static Optional<MetadataResource> findLatestVersion(Bundle bundle) {
		List<Bundle.BundleEntryComponent> entries = bundle.getEntry();
		List<MetadataResource> metadataResources = new ArrayList<>();

		for (Bundle.BundleEntryComponent entry : entries) {
			Resource resource = entry.getResource();
			if (resource instanceof MetadataResource) {
				MetadataResource metadataResource = (MetadataResource) resource;
				metadataResources.add(metadataResource);
			}
		}

		return findLatestVersion(metadataResources);
	}
}
