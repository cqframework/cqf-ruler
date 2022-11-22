package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.UriParam;
import org.cqframework.fhir.api.FhirDal;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.opencds.cqf.ruler.utility.Canonicals;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Configurable
// TODO: This belongs in the Evaluator. Only included in Ruler at dev time for shorter cycle.
public class KnowledgeArtifactProcessor {

	public MetadataResource draft(IdType idType, FhirDal fhirDal) {
		MetadataResource resource = (MetadataResource) fhirDal.read(idType);

		// Root artifact must have status of 'Active'. Existing drafts of reference artifacts will be adopted. This check is
		// performed her to facilitate that different treatment for the root artifact and those referenced by it.
		if (resource.getStatus() != Enumerations.PublicationStatus.ACTIVE) {
			throw new IllegalArgumentException(
				String.format("Drafts can only be created from artifacts with status of 'active'. Resource '%s' has a status of: %s", resource.getUrl(), resource.getStatus().toString()));
		}

		return internalDraft(resource, fhirDal);
	}

	private MetadataResource internalDraft(MetadataResource resource, FhirDal fhirDal) {
		//TODO: Needs to be transactional

		//TODO: Can we really assume MetadataResource here?
		Bundle existingArtifactsForUrl = searchResourceByUrl(resource.getUrl(), fhirDal);
		Optional<Bundle.BundleEntryComponent> existingDrafts = existingArtifactsForUrl.getEntry().stream().filter(
			e -> ((MetadataResource) e.getResource()).getStatus() == Enumerations.PublicationStatus.DRAFT).findFirst();

		MetadataResource newResource = null;
		if (existingDrafts.isPresent()) {
			newResource = (MetadataResource) existingDrafts.get().getResource();
		}

		if (newResource == null) {
			KnowledgeArtifactAdapter<MetadataResource> adapter = new KnowledgeArtifactAdapter<>(resource);
			newResource = adapter.copy();
			newResource.setStatus(Enumerations.PublicationStatus.DRAFT);
			newResource.setVersion(null);

			fhirDal.create(newResource);

			for (RelatedArtifact ra : adapter.getRelatedArtifact()) {
				// If it is a composed-of relation then do a deep copy, else shallow
				if (ra.getType() == RelatedArtifact.RelatedArtifactType.COMPOSEDOF) {
					if (ra.hasUrl()) {
						Bundle referencedResourceBundle = searchResourceByUrl(ra.getUrl(), fhirDal);
						processReferencedResource(fhirDal, referencedResourceBundle);
					} else if (ra.hasResource()) {
						Bundle referencedResourceBundle = searchResourceByUrl(ra.getResourceElement().getValueAsString(), fhirDal);
						processReferencedResource(fhirDal, referencedResourceBundle);
					}
				}
			}
		}

		return newResource;
	}

	private void processReferencedResource(FhirDal fhirDal, Bundle referencedResourceBundle) {
		if (!referencedResourceBundle.getEntryFirstRep().isEmpty()) {
			Bundle.BundleEntryComponent referencedResourceEntry = referencedResourceBundle.getEntry().get(0);
			if (referencedResourceEntry.hasResource() && referencedResourceEntry.getResource() instanceof MetadataResource) {
				MetadataResource referencedResource = (MetadataResource) referencedResourceEntry.getResource();
				internalDraft(referencedResource, fhirDal);
			}
		}
	}

	private Bundle searchResourceByUrl(String url, FhirDal fhirDal) {
		List<IQueryParameterType> list = new ArrayList<>();
		list.add(new UriParam(url));

		Map<String, List<List<IQueryParameterType>>> searchParams = new HashMap<>();
		searchParams.put("url", List.of(list));
		Bundle searchResultsBundle = (Bundle)fhirDal.search(Canonicals.getResourceType(url), searchParams);
		return searchResultsBundle;
	}

	public MetadataResource releaseVersion(IdType idType, FhirDal fhirDal) {
		MetadataResource resource = (MetadataResource) fhirDal.read(idType);
		return publishVersion(fhirDal, resource);
	}

	// Update status
	// Main spec library
	// 	PlanDefinition
	//			Any DEPENDSON update to active
	// 	ValueSet Library
	// 		Grouping ValueSets
	// DEPENDSON - update or validate active status

	// FOR each Library, PlanDefinition, ValueSet Library, Grouping ValueSet
	//		check status for draft (throw if not draft)
	// 	update status
	// 	FOR ALL DEPENDSON
	//			check status for draft (throw if not draft)
	//			update status
	public MetadataResource publishVersion(FhirDal fhirDal, MetadataResource resource) {
		List<RelatedArtifact> artifacts = new ArrayList<>();
		if (resource instanceof Library) {
			artifacts = ((Library) resource).getRelatedArtifact();
		}
		else if (resource instanceof PlanDefinition) {
			artifacts = ((PlanDefinition) resource).getRelatedArtifact();
		}

		for (RelatedArtifact relatedArtifact : artifacts) {
			if (relatedArtifact.hasType() &&
				(relatedArtifact.getType() == RelatedArtifact.RelatedArtifactType.COMPOSEDOF
					|| relatedArtifact.getType() == RelatedArtifact.RelatedArtifactType.DEPENDSON)
				&& relatedArtifact.hasResource()) {
				publishVersion(fhirDal, (MetadataResource) fhirDal.read(new IdType(relatedArtifact.getResource())));
			}
		}

		resource.setStatus(Enumerations.PublicationStatus.ACTIVE);
		fhirDal.update(resource);
		return resource;
	}
}
