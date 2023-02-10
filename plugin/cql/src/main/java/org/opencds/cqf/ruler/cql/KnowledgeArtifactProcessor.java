package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import org.cqframework.fhir.api.FhirDal;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.ruler.builder.BundleBuilder;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.web.client.ResourceAccessException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Configurable
// TODO: This belongs in the Evaluator. Only included in Ruler at dev time for shorter cycle.
public class KnowledgeArtifactProcessor {

	private List<RelatedArtifact> finalRelatedArtifactList = new ArrayList<>();
	private List<Bundle.BundleEntryComponent> bundleEntryComponentList = new ArrayList<>();

	public MetadataResource draft(IdType idType, FhirDal fhirDal) {
		//TODO: Needs to be transactional
		MetadataResource resource = (MetadataResource) fhirDal.read(idType);

		// Root artifact must have status of 'Active'. Existing drafts of reference artifacts will be adopted. This check is
		// performed here to facilitate that different treatment for the root artifact and those referenced by it.
		if (resource.getStatus() != Enumerations.PublicationStatus.ACTIVE) {
			throw new IllegalArgumentException(
				String.format("Drafts can only be created from artifacts with status of 'active'. Resource '%s' has a status of: %s", resource.getUrl(), resource.getStatus().toString()));
		}

		return internalDraft(resource, fhirDal);
	}

	private MetadataResource internalDraft(MetadataResource resource, FhirDal fhirDal) {
		Bundle existingArtifactsForUrl = searchResourceByUrl(resource.getUrl(), fhirDal);
		Optional<Bundle.BundleEntryComponent> existingDrafts = existingArtifactsForUrl.getEntry().stream().filter(
			e -> ((MetadataResource) e.getResource()).getStatus() == Enumerations.PublicationStatus.DRAFT).findFirst();

		MetadataResource newResource = null;
		if (existingDrafts.isPresent()) {
			newResource = (MetadataResource) existingDrafts.get().getResource();
		}

		if (newResource == null) {
			KnowledgeArtifactAdapter<MetadataResource> sourceResourceAdapter = new KnowledgeArtifactAdapter<>(resource);
			newResource = sourceResourceAdapter.copy();
			newResource.setStatus(Enumerations.PublicationStatus.DRAFT);
			newResource.setId((String)null);
			newResource.setVersion(null);

			KnowledgeArtifactAdapter<MetadataResource> newResourceAdapter = new KnowledgeArtifactAdapter<>(newResource);

			// For each Resource relatedArtifact, strip the version of the reference.
			newResourceAdapter.getRelatedArtifact().stream().filter(ra -> ra.hasResource()).collect(Collectors.toList())
				.replaceAll(ra -> ra.setResource(Canonicals.getUrl(ra.getResource())));

			fhirDal.create(newResource);

			for (RelatedArtifact ra : sourceResourceAdapter.getRelatedArtifact()) {
				// If it is a composed-of relation then do a deep copy, else shallow
				if (ra.getType() == RelatedArtifact.RelatedArtifactType.COMPOSEDOF) {
					if (ra.hasUrl()) {
						Bundle referencedResourceBundle = searchResourceByUrl(ra.getUrl(), fhirDal);
						processReferencedResource(fhirDal, referencedResourceBundle, ra);
					} else if (ra.hasResource()) {
						Bundle referencedResourceBundle = searchResourceByUrl(ra.getResourceElement().getValueAsString(), fhirDal);
						processReferencedResource(fhirDal, referencedResourceBundle, ra);
					}
				}
			}
		}

		return newResource;
	}

	private void processReferencedResource(FhirDal fhirDal, Bundle referencedResourceBundle, RelatedArtifact ra) {
		if (!referencedResourceBundle.getEntryFirstRep().isEmpty()) {
			Bundle.BundleEntryComponent referencedResourceEntry = referencedResourceBundle.getEntry().get(0);
			if (referencedResourceEntry.hasResource() && referencedResourceEntry.getResource() instanceof MetadataResource) {
				MetadataResource referencedResource = (MetadataResource) referencedResourceEntry.getResource();

				internalDraft(referencedResource, fhirDal);
			}
		}
	}

	private Bundle searchResourceByUrl(String url, FhirDal fhirDal) {
		Map<String, List<List<IQueryParameterType>>> searchParams = new HashMap<>();

		List<IQueryParameterType> urlList = new ArrayList<>();
		urlList.add(new UriParam(Canonicals.getUrl(url)));
		searchParams.put("url", List.of(urlList));

		List<IQueryParameterType> versionList = new ArrayList<>();
		String version = Canonicals.getVersion(url);
		if (version != null && !version.isEmpty()) {
			versionList.add(new TokenParam(Canonicals.getVersion((url))));
			searchParams.put("version", List.of(versionList));
		}

		Bundle searchResultsBundle = (Bundle)fhirDal.search(Canonicals.getResourceType(url), searchParams);
		return searchResultsBundle;
	}

	public MetadataResource releaseVersion(IdType iIdType, FhirDal fhirDal) {
		MetadataResource resource = (MetadataResource) fhirDal.read(iIdType);
		KnowledgeArtifactAdapter<MetadataResource> adapter = new KnowledgeArtifactAdapter<>(resource);

		finalRelatedArtifactList = adapter.getRelatedArtifact();
		int listCounter=0;
		int listSize = finalRelatedArtifactList.size();
		for (RelatedArtifact ra : finalRelatedArtifactList) {
			getAdditionReleaseData(finalRelatedArtifactList, fhirDal, ra, true, bundleEntryComponentList);
			listCounter++;
			if(listCounter == listSize) {
				break;
			}
		}

		adapter.setRelatedArtifact(finalRelatedArtifactList);

		fhirDal.update(resource);
		return resource;
	}

	private void getAdditionReleaseData(List<RelatedArtifact> finalRelatedArtifactList, FhirDal fhirDal, RelatedArtifact ra, boolean release, List<Bundle.BundleEntryComponent> bundleEntryComponentList) {
		// update root artifact with relatedArtifacts that reflect all of its direct and transitive references.
		// This bit should be its own method so that we can call it recursively:

			if (ra.hasResource()) {
				if(release) {
					ra.setType(RelatedArtifact.RelatedArtifactType.DEPENDSON);
				}
				String resourceData = ra.getResource();
				if(Canonicals.getUrl(resourceData) != null) {
					List < IQueryParameterType > list = new ArrayList<>();
					if(Canonicals.getVersion(resourceData) != null) {
						list.add(new UriParam(resourceData));
					} else {
						list.add(new UriParam(Canonicals.getUrl(resourceData)));
					}
					Map<String, List<List<IQueryParameterType>>> searchParams = new HashMap<>();
					searchParams.put("url", List.of(list));

					Bundle referencedResourceBundle = (Bundle) fhirDal.search(Canonicals.getResourceType(Canonicals.getUrl(resourceData)), searchParams);
					if (!referencedResourceBundle.getEntryFirstRep().isEmpty()) {
						Bundle.BundleEntryComponent referencedResourceEntry = referencedResourceBundle.getEntry().get(0);

						if (referencedResourceEntry.hasResource()) {
							MetadataResource referencedResource = (MetadataResource) referencedResourceEntry.getResource();
							if(release) {
								KnowledgeArtifactAdapter<MetadataResource> adapterNew = new KnowledgeArtifactAdapter<>(referencedResource);
								List<RelatedArtifact> newRelatedArtifactList = adapterNew.getRelatedArtifact();
								boolean found = false;
								for(RelatedArtifact newOne : newRelatedArtifactList) {
									for (RelatedArtifact nextOne : finalRelatedArtifactList) {
										if (newOne.getResource().equals(nextOne.getResource())) {
											found = true;
											break;
										}
									}
									if (!found) {
										finalRelatedArtifactList.add(newOne);
										getAdditionReleaseData(finalRelatedArtifactList, fhirDal, newOne, release, bundleEntryComponentList);
									}
								}
							} else {
								bundleEntryComponentList.add(referencedResourceEntry);
								referencedResourceBundle.setEntry(bundleEntryComponentList);
							}
						}
					}
				}
			}
	}

	public MetadataResource publish(FhirDal fhirDal, MetadataResource resource) {
		if (!resource.getStatus().equals(Enumerations.PublicationStatus.ACTIVE)) {
			throw new ResourceAccessException(String.format("The posted resource must have status of 'active'. The proposed resource has status: %s", resource.getStatus().toString()));
		}

		if (resource.getId() == null || resource.getId().isEmpty()) {
			fhirDal.create(resource);
		} else {
			MetadataResource existingResource = (MetadataResource) fhirDal.read(resource.getIdElement());
			if (existingResource != null) {
				fhirDal.update(resource);
			}
		}

		//TODO: This is wrong. Once the FhirDal implementation supports returning the resource
		// (or at least ID so it can be retrieved) that resource should be returned rather than the proposed resource.
		return resource;
	}

	public MetadataResource revise(FhirDal fhirDal, MetadataResource resource) {
		MetadataResource existingResource = (MetadataResource) fhirDal.read(resource.getIdElement());
		if (existingResource == null) {
			throw new IllegalArgumentException(String.format("Resource with ID: '%s' not found.", resource.getId()));
		}

		if (!existingResource.getStatus().equals(Enumerations.PublicationStatus.DRAFT)) {
			throw new ResourceAccessException(String.format("Current resource status is '%s'. Only resources with status of 'draft' can be revised.", resource.getStatus().toString()));
		}

		if (!resource.getStatus().equals(Enumerations.PublicationStatus.DRAFT)) {
			throw new ResourceAccessException(String.format("The resource status can not be updated from 'draft'. The proposed resource has status: %s", resource.getStatus().toString()));
		}

		fhirDal.update(resource);
		//TODO: This is a short-term and incorrect solution. Once the FhirDal supports returning the
		// resource from the update method, the updated resource should be returned (i.e., not the proposed resource).
		return resource;
	}


	public IBaseBundle packageOperation(FhirDal fhirDal, IdType idType) {

		MetadataResource existingResource = (MetadataResource) fhirDal.read(idType);

		if (existingResource.getId() == null ||  existingResource.getId().isEmpty()) {
			throw new ResourceAccessException(String.format("A valid resource ID is required."));
		}

		if (existingResource == null) {
			throw new IllegalArgumentException(String.format("Resource with ID: '%s' not found.", idType.getId()));
		}

		MetadataResource resource = (MetadataResource) fhirDal.read(idType);
		KnowledgeArtifactAdapter<MetadataResource> adapter = new KnowledgeArtifactAdapter<>(resource);

		Bundle packageBundle = new BundleBuilder<>(Bundle.class)
			.withType(Bundle.BundleType.COLLECTION.toString())
			.build();

		finalRelatedArtifactList = adapter.getRelatedArtifact();
		int listCounter=0;
		int listSize = finalRelatedArtifactList.size();
		for (RelatedArtifact ra : finalRelatedArtifactList) {
			getAdditionReleaseData(finalRelatedArtifactList, fhirDal, ra, true, bundleEntryComponentList);
			listCounter++;
			if(listCounter == listSize) {
				break;
			}
		}

		adapter.setRelatedArtifact(finalRelatedArtifactList);

		fhirDal.update(resource);
		packageBundle.addEntry().setResource(resource);

		return (IBaseBundle) packageBundle;
	}
}
