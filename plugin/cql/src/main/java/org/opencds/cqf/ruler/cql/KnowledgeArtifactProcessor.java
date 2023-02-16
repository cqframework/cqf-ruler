package org.opencds.cqf.ruler.cql;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.cqframework.fhir.api.FhirDal;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.ContactDetail;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.ruler.cql.r4.ArtifactCommentExtension;
import org.springframework.beans.factory.annotation.Configurable;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

@Configurable
// TODO: This belongs in the Evaluator. Only included in Ruler at dev time for
// shorter cycle.
public class KnowledgeArtifactProcessor {

	private List<RelatedArtifact> finalRelatedArtifactList = new ArrayList<>();
	private List<RelatedArtifact> finalRelatedArtifactListUpdated = new ArrayList<>();
	private List<Bundle.BundleEntryComponent> bundleEntryComponentList = new ArrayList<>();

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

	private Bundle searchResourceByUrlAndStatus(String url, String status, FhirDal fhirDal) {
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

		List<IQueryParameterType> statusList = new ArrayList<>();
		statusList.add(new TokenParam(status));
		searchParams.put("status", List.of(statusList));

		Bundle searchResultsBundle = (Bundle) fhirDal.search(Canonicals.getResourceType(url), searchParams);
		return searchResultsBundle;
	}

	/* approve */
	/*
	 * The operation sets the date and approvalDate elements of the approved
	 * artifact,
	 * and is otherwise only allowed to add artifactComment elements to the artifact
	 * and to add or update an endorser.
	 */
	public MetadataResource approve(IdType idType, Date approvalDate, String artifactCommentType,
			String artifactCommentText, String artifactCommentTarget, String artifactCommentReference,
			String artifactCommentUser,
			ContactDetail endorser, FhirDal fhirDal) {
		MetadataResource resource = (MetadataResource) fhirDal.read(idType);
		if (resource == null) {
			throw new ResourceNotFoundException(idType);
		}

		KnowledgeArtifactAdapter<MetadataResource> targetResourceAdapter = new KnowledgeArtifactAdapter<MetadataResource>(resource);
		Date currentDate = new Date();
		// 1. Set approvalDate
		if(approvalDate == null){
			targetResourceAdapter.setApprovalDate(currentDate);
		} else {
			targetResourceAdapter.setApprovalDate(approvalDate);
		}

		// 2. Set date
		DateTimeType theDate = new DateTimeType(currentDate);
		resource.setDateElement(theDate);
		// 3. Add artifactComment
		// TODO: check for existing matching comment?
		try {
			ArtifactCommentExtension artifactCommentExtension = new ArtifactCommentExtension(artifactCommentType,artifactCommentText,artifactCommentTarget,artifactCommentReference,artifactCommentUser);
			if(!artifactCommentExtension.getExtension().isEmpty()){
				resource.addExtension(artifactCommentExtension);
			}
		} catch (FHIRException e) {
			throw new UnprocessableEntityException(e.getMessage());
		}
		// 4. add/update endorser
		if (endorser != null) {
			targetResourceAdapter.updateEndorser(endorser);
		}
		fhirDal.update(resource);
		return resource;
	}

	/* $draft */
	public MetadataResource draft(IdType idType, FhirDal fhirDal) {
		//TODO: Needs to be transactional
		MetadataResource resource = (MetadataResource) fhirDal.read(idType);

		// Root artifact must have status of 'Active'. Existing drafts of reference artifacts will be adopted. This check is
		// performed here to facilitate that different treatment for the root artifact and those referenced by it.
		if (resource.getStatus() != Enumerations.PublicationStatus.ACTIVE) {
			throw new IllegalStateException(
				String.format("Drafts can only be created from artifacts with status of 'active'. Resource '%s' has a status of: %s", resource.getUrl(), resource.getStatus().toString()));
		}

		Bundle existingArtifactsForUrl = searchResourceByUrl(resource.getUrl(), fhirDal);
		Optional<Bundle.BundleEntryComponent> existingDrafts = existingArtifactsForUrl.getEntry().stream().filter(
			e -> ((MetadataResource) e.getResource()).getStatus() == Enumerations.PublicationStatus.DRAFT).findFirst();

		if (existingDrafts.isPresent()) {
			throw new IllegalStateException(
				String.format("A draft of Program '%s' already exists with ID: '%s'. Only one draft of a program can exist at a time.", resource.getUrl(), ((MetadataResource) existingDrafts.get().getResource()).getId()));
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
						processReferencedResourceForDraft(fhirDal, referencedResourceBundle, ra);
					} else if (ra.hasResource()) {
						Bundle referencedResourceBundle = searchResourceByUrl(ra.getResourceElement().getValueAsString(), fhirDal);
						processReferencedResourceForDraft(fhirDal, referencedResourceBundle, ra);
					}
				}
			}
		}

		return newResource;
	}

	private void processReferencedResourceForDraft(FhirDal fhirDal, Bundle referencedResourceBundle, RelatedArtifact ra) {
		if (!referencedResourceBundle.getEntryFirstRep().isEmpty()) {
			Bundle.BundleEntryComponent referencedResourceEntry = referencedResourceBundle.getEntry().get(0);
			if (referencedResourceEntry.hasResource() && referencedResourceEntry.getResource() instanceof MetadataResource) {
				MetadataResource referencedResource = (MetadataResource) referencedResourceEntry.getResource();

				internalDraft(referencedResource, fhirDal);
			}
		}
	}

	/* $release2 */
	public MetadataResource release2(IdType idType, String version, boolean latestFromTxServer, FhirDal fhirDal) {
		// TODO: This needs to be transactional!
		MetadataResource rootArtifact = (MetadataResource) fhirDal.read(idType);
		if (rootArtifact == null) {
			throw new IllegalArgumentException(String.format("Resource with ID: '%s' not found.", idType.getIdPart()));
		}

		String releaseVersion = version;
		if (releaseVersion == null || releaseVersion.isEmpty()) {
			releaseVersion = rootArtifact.hasVersion() ? rootArtifact.getVersion() : null;
		}

		if (releaseVersion == null || releaseVersion.isEmpty()) {
			throw new IllegalStateException(String.format("No version found. Either the resource targeted for release must have a version or a version must be provided as an argument to the $release operation."));
		}

		KnowledgeArtifactAdapter<MetadataResource> rootArtifactAdapter = new KnowledgeArtifactAdapter<>(rootArtifact);

		List<RelatedArtifact> resolvedRelatedArtifacts = internalRelease(rootArtifactAdapter, releaseVersion, latestFromTxServer, fhirDal);

		// once iteration is complete, delete all depends-on RAs in the root artifact
		rootArtifactAdapter.getRelatedArtifact().removeIf(ra -> ra.getType() == RelatedArtifact.RelatedArtifactType.DEPENDSON);
		rootArtifactAdapter.getRelatedArtifact().addAll(resolvedRelatedArtifacts);
		fhirDal.update(rootArtifact);

		return rootArtifact;
	}

	private List<RelatedArtifact> internalRelease(KnowledgeArtifactAdapter<MetadataResource> artifactAdapter,
																 String version, boolean latestFromTxServer, FhirDal fhirDal) {
		List<RelatedArtifact> resolvedRelatedArtifacts = new ArrayList<RelatedArtifact>();

		artifactAdapter.resource.setDate(new Date());
		artifactAdapter.resource.setVersion(version);

		fhirDal.update(artifactAdapter.resource);

		for (RelatedArtifact ra : artifactAdapter.getComponents()) {
			if (ra.hasResource()) {
				// TODO: This is likely the wrong characteristic to use for distinguishing between those things that are
				// part of the spec library and the leaf valuesets. Likely needs be a profile. For now though, relatedArtifact.Type
				String reference;
				Bundle searchBundle;
				CanonicalType resourceReference = ra.getResourceElement();
				String currentlyPinnedVersion = Canonicals.getVersion(resourceReference);

				// For composition references, if a version is not specified in the reference then the "draft" version
				// of the referenced artifact should be used.
				if (currentlyPinnedVersion == null || currentlyPinnedVersion.isEmpty()) {
					reference = resourceReference.getValueAsString().concat("|").concat(version);
					searchBundle = searchResourceByUrlAndStatus(resourceReference.getValueAsString(), "draft", fhirDal);
				} else {
					reference = resourceReference.getValueAsString();
					searchBundle = searchResourceByUrl(reference, fhirDal);
				}

				resolvedRelatedArtifacts.add(new RelatedArtifact().setType(RelatedArtifact.RelatedArtifactType.DEPENDSON).setResource(reference));
				KnowledgeArtifactAdapter<MetadataResource> searchResultAdapter = processSearchBundle(searchBundle);
				resolvedRelatedArtifacts.addAll(internalRelease(searchResultAdapter, version, latestFromTxServer, fhirDal));
			}
		}

		for (RelatedArtifact ra : artifactAdapter.getDependencies()) {
			if (ra.hasResource()) {
				String reference;
				Bundle searchBundle;
				CanonicalType resourceReference = ra.getResourceElement();
				String currentlyPinnedVersion = Canonicals.getVersion(resourceReference);

				// For dependencies, if a specific version is referenced, use it,
				// else if the check tx server is checked then lookup latest version from tx server,
				//   else get latest version from our cache.
				if (currentlyPinnedVersion == null || currentlyPinnedVersion.isEmpty()) {
					if (latestFromTxServer) {
						throw new NotImplementedException("Support for 'latestFromTxServer' is not yet implemented.");
						// TODO: Will need to query the configured (will need to know the configured TxServer from client) TxServer
						// to get the latest version of the ValueSet, download it into the cache - will need to augment the same way
						// as client.
					} else {
						// TODO: Lookup the latest from our cache.
						// In this case, all we need to do is determine the version so that we can add the version-specific refernece to the list.
						reference = resourceReference.getValueAsString();
						searchBundle = searchResourceByUrl(reference, fhirDal);
						// TODO: How to find latest?
						// search with sort descending by version
						// String latestVersion = latestResource.getVersion()
						// reference = resourceReference.getValueAsString().concat("|").concat(latest);
						// TODO: validate that status is "draft" and throw if not.
					}
				} else {
					reference = resourceReference.getValueAsString();
					searchBundle = searchResourceByUrl(reference, fhirDal);
					KnowledgeArtifactAdapter<MetadataResource> referencedResource = processSearchBundle(searchBundle);
					if (referencedResource.resource.getStatus() != Enumerations.PublicationStatus.ACTIVE) {
						throw new IllegalStateException(String.format("Resource '%s' is not in active status and cannot be reference in this release.", reference));
					}
				}

				resolvedRelatedArtifacts.add(new RelatedArtifact().setType(RelatedArtifact.RelatedArtifactType.DEPENDSON).setResource(reference));
			}
		}

		return resolvedRelatedArtifacts;
	}

	private KnowledgeArtifactAdapter<MetadataResource> processSearchBundle(Bundle searchBundle) {
		KnowledgeArtifactAdapter<MetadataResource> adapter = null;

		if (!searchBundle.getEntryFirstRep().isEmpty()) {
			Bundle.BundleEntryComponent referencedResourceEntry = searchBundle.getEntry().get(0);
			if (referencedResourceEntry.hasResource() && referencedResourceEntry.getResource() instanceof MetadataResource) {
				MetadataResource referencedResource = (MetadataResource) referencedResourceEntry.getResource();
				adapter = new KnowledgeArtifactAdapter<>(referencedResource);
			}
		}

		return adapter;
	}

	private String getLatestVersionOfResource(CanonicalType canonicalUrl) {
		return "Not Implemented";
	}

	/* $release */
	public MetadataResource release(IdType iIdType, FhirDal fhirDal) {
		MetadataResource resource = (MetadataResource) fhirDal.read(iIdType);
		KnowledgeArtifactAdapter<MetadataResource> adapter = new KnowledgeArtifactAdapter<>(resource);

		finalRelatedArtifactList = adapter.getRelatedArtifact();
		finalRelatedArtifactListUpdated = adapter.getRelatedArtifact();
		int listCounter = 0;
		int listSize = finalRelatedArtifactList.size();
		for (RelatedArtifact ra : finalRelatedArtifactList) {
			getAdditionReleaseData(finalRelatedArtifactListUpdated, fhirDal, ra, true, bundleEntryComponentList);
			listCounter++;
			if (listCounter == listSize) {
				break;
			}
		}

		adapter.setRelatedArtifact(finalRelatedArtifactListUpdated);

		fhirDal.update(resource);
		return resource;
	}

	private void getAdditionReleaseData(List<RelatedArtifact> finalRelatedArtifactList, FhirDal fhirDal, RelatedArtifact ra, boolean release, List<Bundle.BundleEntryComponent> bundleEntryComponentList) {
		// update root artifact with relatedArtifacts that reflect all of its direct and transitive references.
		// This bit should be its own method so that we can call it recursively:

		if (ra.hasResource()) {
			if (release) {
				ra.setType(RelatedArtifact.RelatedArtifactType.DEPENDSON);
			}
			String resourceData = ra.getResource();
			if (Canonicals.getUrl(resourceData) != null) {
				List < IQueryParameterType > list = new ArrayList<>();
				if (Canonicals.getVersion(resourceData) != null) {
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

	/* $revise */
	public MetadataResource revise(FhirDal fhirDal, MetadataResource resource) {
		MetadataResource existingResource = (MetadataResource) fhirDal.read(resource.getIdElement());
		if (existingResource == null) {
			throw new IllegalArgumentException(String.format("Resource with ID: '%s' not found.", resource.getId()));
		}

		if (!existingResource.getStatus().equals(Enumerations.PublicationStatus.DRAFT)) {
			throw new IllegalStateException(String.format("Current resource status is '%s'. Only resources with status of 'draft' can be revised.", resource.getStatus().toString()));
		}

		if (!resource.getStatus().equals(Enumerations.PublicationStatus.DRAFT)) {
			throw new IllegalStateException(String.format("The resource status can not be updated from 'draft'. The proposed resource has status: %s", resource.getStatus().toString()));
		}

		fhirDal.update(resource);

		return resource;
	}
}
