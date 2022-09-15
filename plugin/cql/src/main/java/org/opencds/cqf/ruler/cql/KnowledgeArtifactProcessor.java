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
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.opencds.cqf.ruler.utility.Canonicals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: This belongs in the Evaluator. Only included in Ruler at dev time for shorter cycle.
public class KnowledgeArtifactProcessor  extends DaoRegistryOperationProvider {
	// TODO: Autowire?
	// private KnowledgeArtifactAdapter adapter;

	public MetadataResource newVersion(MetadataResource resource, FhirDal fhirDal) {
		MetadataResource newResource = null;
		KnowledgeArtifactAdapter adapter = new KnowledgeArtifactAdapter(resource);
		newResource = adapter.copy();
		newResource.setStatus(Enumerations.PublicationStatus.DRAFT);
		newResource.setVersion(null);

		for (RelatedArtifact ra : (List<RelatedArtifact>)adapter.getRelatedArtifact()) {
			// If it is a composed-of relation then do a deep copy, else shallow
			if (ra.getType() == RelatedArtifact.RelatedArtifactType.COMPOSEDOF) {
				// TODO: Canonical handling
				if (ra.hasResource()) {
					CanonicalType canonical = ra.getResourceElement();
					List<IQueryParameterType> list = new ArrayList<>();
					list.add(new UriParam(ra.getResourceElement().getValueAsString()));

					Map<String, List<IQueryParameterType>> searchParams = new HashMap<String, List<IQueryParameterType>>();
					searchParams.put("url", list);
					Bundle referencedResourceBundle = (Bundle)fhirDal.search(Canonicals.getResourceType(canonical), searchParams);
					if (!referencedResourceBundle.getEntry().isEmpty()) {
						//TODO: Can I assume a single result here? Doubt it! If the reference had a version, then it would have
						// returned just that version, right? It the reference wasn't version-specific, take the latest version?
						// Maybe that's the default search behavior?
						Bundle.BundleEntryComponent referencedResourceEntry = referencedResourceBundle.getEntry().get(0);
						if (referencedResourceEntry.hasResource() && referencedResourceEntry.getResource() instanceof MetadataResource) {
							MetadataResource referencedResource = (MetadataResource)referencedResourceEntry.getResource();
							newVersion(referencedResource, fhirDal);
						}
					}
				}
			}
//			else if (ra.getType() == RelatedArtifact.RelatedArtifactType.DEPENDSON) {
//
//			}

			fhirDal.create(newResource);
		}

		return newResource;
	}

	public MetadataResource releaseVersion(IdType iIdType, FhirDal fhirDal) {

		MetadataResource resource = read(iIdType);
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
					if(!status.equals(Enumerations.PublicationStatus.ACTIVE)) {
						((Library) entry.getResource()).setStatus(Enumerations.PublicationStatus.ACTIVE);
					}
				}
			}
		);

		update(resource);

		return resource;

	}

	public MetadataResource publishVersion(IdType iIdType, FhirDal fhirDal) {

		MetadataResource resource = read(iIdType);
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
						create(resource);
					}
				}
			}
		);

		return resource;

	}
}
