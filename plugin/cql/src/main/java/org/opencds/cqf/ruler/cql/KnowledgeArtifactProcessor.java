package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.UriParam;
import org.cqframework.fhir.api.FhirDal;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.opencds.cqf.ruler.utility.Canonicals;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configurable
// TODO: This belongs in the Evaluator. Only included in Ruler at dev time for shorter cycle.
public class KnowledgeArtifactProcessor {

	public MetadataResource newVersion(MetadataResource resource, FhirDal fhirDal) {
		MetadataResource newResource;
		KnowledgeArtifactAdapter<MetadataResource> adapter = new KnowledgeArtifactAdapter<>(resource);
		newResource = adapter.copy();
		newResource.setStatus(Enumerations.PublicationStatus.DRAFT);
		newResource.setVersion(null);

		for (RelatedArtifact ra : adapter.getRelatedArtifact()) {
			// If it is a composed-of relation then do a deep copy, else shallow
			if (ra.getType() == RelatedArtifact.RelatedArtifactType.COMPOSEDOF) {
				if(ra.hasUrl()) {
					// Canonical handling
					List<IQueryParameterType> list = new ArrayList<>();
					list.add(new UriParam(ra.getUrl()));

					Map<String, List<List<IQueryParameterType>>> searchParams = new HashMap<>();
					searchParams.put("url", List.of(list));
					Bundle referencedResourceBundle = (Bundle)fhirDal.search(Canonicals.getResourceType(ra.getUrl()), searchParams);
					if (!referencedResourceBundle.getEntryFirstRep().isEmpty()) {
						Bundle.BundleEntryComponent referencedResourceEntry = referencedResourceBundle.getEntry().get(0);
						if (referencedResourceEntry.hasResource() && referencedResourceEntry.getResource() instanceof MetadataResource) {
							MetadataResource referencedResource = (MetadataResource)referencedResourceEntry.getResource();
							newVersion(referencedResource, fhirDal);
						}
					}
				} else if (ra.hasResource()) {
					CanonicalType canonical = ra.getResourceElement();
					List<IQueryParameterType> list = new ArrayList<>();
					list.add(new UriParam(ra.getResourceElement().getValueAsString()));

					Map<String, List<List<IQueryParameterType>>> searchParams = new HashMap<>();
					searchParams.put("url", List.of(list));
					Bundle referencedResourceBundle = (Bundle)fhirDal.search(Canonicals.getResourceType(canonical), searchParams);
					if (!referencedResourceBundle.getEntryFirstRep().isEmpty()) {
						Bundle.BundleEntryComponent referencedResourceEntry = referencedResourceBundle.getEntry().get(0);
						if (referencedResourceEntry.hasResource() && referencedResourceEntry.getResource() instanceof MetadataResource) {
							MetadataResource referencedResource = (MetadataResource)referencedResourceEntry.getResource();
							newVersion(referencedResource, fhirDal);
						}
					}
				}
			}
			fhirDal.create(newResource);
		}

		return newResource;
	}

	public MetadataResource releaseVersion(IdType iIdType, FhirDal fhirDal) {
		MetadataResource resource = (MetadataResource) fhirDal.read(iIdType);

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

		fhirDal.update(resource);

		return resource;

	}

	public MetadataResource publishVersion(IdType iIdType, FhirDal fhirDal, MetadataResource resource) {
		String resourceVersionId = resource.getMeta().getVersionId();
		if(!resourceVersionId.contains("draft")) {
			throw new FHIRException("Resource found by " + iIdType + " is not a draft version.  Please select a draft resource.");
		}
		if(!resourceVersionId.startsWith("draft")) {
			throw new FHIRException("Resource found by " + iIdType + " is not a draft version.  The version does not start with draft.");
		}
		String[] resourceIdArray = resourceVersionId.split("draft-");
		String updatedResourceId = resourceIdArray[1];
		resource.setId(updatedResourceId);

		Bundle bundle = new Bundle();
		bundle.getEntry().forEach(
			entry -> {
				if(entry.hasResource() && entry.getResource() instanceof Library) {
					Enumerations.PublicationStatus status = ((Library) entry.getResource()).getStatus();
					if(status.equals(Enumerations.PublicationStatus.ACTIVE)) {
						fhirDal.create(resource);
					}
				}
			}
		);

		return resource;

	}
}
