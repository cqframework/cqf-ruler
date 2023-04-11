package org.opencds.cqf.ruler.cql;

import static java.util.Comparator.comparing;

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
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.ContactDetail;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MarkdownType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.jetbrains.annotations.Nullable;
import org.opencds.cqf.cql.engine.exception.InvalidOperatorArgument;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.ruler.cql.r4.ArtifactAssessment;
import org.opencds.cqf.ruler.cql.r4.ArtifactAssessment.ArtifactAssessmentContentInformationType;
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
	 * artifact, and is otherwise only allowed to add artifactComment elements
	 * to the artifact and to add or update an endorser.
	 */
	public MetadataResource approve(MetadataResource resource, IPrimitiveType<Date> approvalDate,
			ContactDetail endorser, ArtifactAssessment assessment) {

		KnowledgeArtifactAdapter<MetadataResource> targetResourceAdapter = new KnowledgeArtifactAdapter<MetadataResource>(resource);
		Date currentDate = new Date();

		// 1. Set approvalDate
		if (approvalDate == null){
			targetResourceAdapter.setApprovalDate(currentDate);
		} else {
			targetResourceAdapter.setApprovalDate(approvalDate.getValue());
		}

		// 2. Set date
		DateTimeType theDate = new DateTimeType(currentDate);
		resource.setDateElement(theDate);

		// 3. add/update endorser
		if (endorser != null) {
			targetResourceAdapter.updateEndorser(endorser);
		}
		return resource;
	}
	ArtifactAssessment createApprovalAssessment(IdType id, String artifactCommentType,
	String artifactCommentText, CanonicalType artifactCommentTarget, CanonicalType artifactCommentReference,
	Reference artifactCommentUser) throws UnprocessableEntityException {
		// TODO: check for existing matching comment?
		ArtifactAssessment artifactAssessment;
		try {
			artifactAssessment = new ArtifactAssessment(new Reference(id));
			artifactAssessment.createArtifactComment(
				ArtifactAssessmentContentInformationType.fromCode(artifactCommentType),
				new MarkdownType(artifactCommentText),
				artifactCommentReference,
				artifactCommentUser,
				artifactCommentTarget,
				null
				);
		} catch (FHIRException e) {
			throw new UnprocessableEntityException(e.getMessage());
		}
		return artifactAssessment;
	}

	/* $draft */
	public MetadataResource draft(IdType idType, FhirDal fhirDal, String version) {
		//TODO: Needs to be transactional
		MetadataResource resource = (MetadataResource) fhirDal.read(idType);
		if (resource == null) {
			throw new ResourceNotFoundException(idType);
		}

		if (version.contains(".")) {
			String[] versionParts = version.split("\\.");
			for(int i = 0; i < versionParts.length; i++) {
				String section = "";
				if(Integer.parseInt(versionParts[i]) < 0) {
					if(i == 0) {
						section = "Major";
					} else if(i == 1) {
						section = "Minor";
					} else if (i == 2) {
						section = "Patch";
					}
					throw new IllegalArgumentException("The " + section + " version part should be greater than 0.");
				}
			}

		}

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

		return internalDraft(resource, fhirDal, version);
	}

	private MetadataResource internalDraft(MetadataResource resource, FhirDal fhirDal, String version) {
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
			newResource.setVersion(version + "-draft");

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
						processReferencedResourceForDraft(fhirDal, referencedResourceBundle, ra, version);
					} else if (ra.hasResource()) {
						Bundle referencedResourceBundle = searchResourceByUrl(ra.getResourceElement().getValueAsString(), fhirDal);
						processReferencedResourceForDraft(fhirDal, referencedResourceBundle, ra, version);
					}
				}
			}
		}

		return newResource;
	}

	private void processReferencedResourceForDraft(FhirDal fhirDal, Bundle referencedResourceBundle, RelatedArtifact ra, String version) {
		if (!referencedResourceBundle.getEntryFirstRep().isEmpty()) {
			Bundle.BundleEntryComponent referencedResourceEntry = referencedResourceBundle.getEntry().get(0);
			if (referencedResourceEntry.hasResource() && referencedResourceEntry.getResource() instanceof MetadataResource) {
				MetadataResource referencedResource = (MetadataResource) referencedResourceEntry.getResource();

				internalDraft(referencedResource, fhirDal, version);
			}
		}
	}

	@Nullable
	private String getReleaseVersion(String version, CodeType versionBehavior, String existingVersion) {
		String releaseVersion = null;

		// If no version exists use the version argument provided
		if (existingVersion == null || existingVersion.isEmpty() || existingVersion.isBlank()) {
			return version;
		}

		if (versionBehavior.getCode().equals("default")) {
			releaseVersion = existingVersion != null && !existingVersion.isEmpty() ? existingVersion : version;
		} else if (versionBehavior.getCode().equals("check")) {
			if (existingVersion.equals(version)) {
				throw new IllegalStateException(String.format("versionBehavior specified is 'check and the version provided ('%s') does not match the version currently specified on the root artifact ('%s')."));
			}
		} else if (versionBehavior.getCode().equals("force")) {
			releaseVersion = version;
		}
		return releaseVersion;
	}

	/* $release */
	public MetadataResource releaseVersion(IdType idType, String version, CodeType versionBehavior, boolean latestFromTxServer, FhirDal fhirDal) {
		// TODO: This check is to avoid partial releases and should be removed once the argument is supported (or it is transactional).
		if (latestFromTxServer) {
			throw new NotImplementedException("Support for 'latestFromTxServer' is not yet implemented.");
		}
		// TODO: This needs to be transactional!

		if (version == null || version.isEmpty()) {
			throw new InvalidOperatorArgument("version must be provided as an argument to the $release operation.");
		}

		if (versionBehavior == null || versionBehavior.getCode() == null || versionBehavior.getCode().isEmpty()) {
			throw new InvalidOperatorArgument("versionBehavior must be provided as an argument to the $release operation. Valid values are 'default', 'check', 'force'.");
		}

		if (!versionBehavior.getCode().equals("default") && !versionBehavior.getCode().equals("check") && !versionBehavior.getCode().equals("force")) {
			throw new InvalidOperatorArgument(String.format("'%s' is not a valid versionBehavior. Valid values are 'default', 'check', 'force'", versionBehavior.getCode()));
		}

		MetadataResource rootArtifact = (MetadataResource) fhirDal.read(idType);
		if (rootArtifact == null) {
			throw new IllegalArgumentException(String.format("Resource with ID: '%s' not found.", idType.getIdPart()));
		}

		KnowledgeArtifactAdapter<MetadataResource> rootArtifactAdapter = new KnowledgeArtifactAdapter<>(rootArtifact);
		Date currentApprovalDate = rootArtifactAdapter.getApprovalDate();
		if (currentApprovalDate == null) {
			throw new InvalidOperatorArgument(String.format("The artifact must be approved (indicated by approvalDate) before it is eligible for release."));
		}

		if (currentApprovalDate.before(rootArtifact.getDate())) {
			throw new InvalidOperatorArgument(
				String.format("The artifact was approved on '%s', but was last modified on '%s'. An approval must be provided after the most-recent update.", currentApprovalDate, rootArtifact.getDate()));
		}

		// Determine which version should be used.
		String existingVersion = rootArtifact.hasVersion() ? rootArtifact.getVersion() : null;
		String releaseVersion = getReleaseVersion(version, versionBehavior, existingVersion);

		List<RelatedArtifact> resolvedRelatedArtifacts = internalRelease(rootArtifactAdapter, releaseVersion, versionBehavior, latestFromTxServer, fhirDal);

		// once iteration is complete, delete all depends-on RAs in the root artifact
		rootArtifactAdapter.getRelatedArtifact().removeIf(ra -> ra.getType() == RelatedArtifact.RelatedArtifactType.DEPENDSON);

		// removed duplicates and add
		List<RelatedArtifact> distinctResolvedRelatedArtifacts = new ArrayList<>();
		for (RelatedArtifact ra: resolvedRelatedArtifacts) {
			if (!distinctResolvedRelatedArtifacts.stream().anyMatch(r -> r.getResource().equals(ra.getResource()))) {
				distinctResolvedRelatedArtifacts.add(ra);
			}
		}
		rootArtifactAdapter.getRelatedArtifact().addAll(distinctResolvedRelatedArtifacts);
		fhirDal.update(rootArtifact);

		return rootArtifact;
	}

	private List<RelatedArtifact> internalRelease(KnowledgeArtifactAdapter<MetadataResource> artifactAdapter, String version,
																 CodeType versionBehavior, boolean latestFromTxServer, FhirDal fhirDal) {
		List<RelatedArtifact> resolvedRelatedArtifacts = new ArrayList<RelatedArtifact>();

		// Need to update the Date element because we're changing the status
		artifactAdapter.resource.setDate(new Date());
		artifactAdapter.resource.setStatus(Enumerations.PublicationStatus.ACTIVE);
		artifactAdapter.resource.setVersion(getReleaseVersion(version, versionBehavior, artifactAdapter.resource.getVersion()));

		fhirDal.update(artifactAdapter.resource);

		// TODO: This is likely the wrong characteristic to use for distinguishing between those things that are
		// part of the spec library and the leaf value sets. Likely needs be a profile. For now though, relatedArtifact.Type
		for (RelatedArtifact ra : artifactAdapter.getComponents()) {
			if (ra.hasResource()) {
				MetadataResource referencedResource;
				CanonicalType resourceReference = ra.getResourceElement();
				String reference = resourceReference.getValueAsString();

				// For composition references, if a version is not specified in the reference then the latest version
				// of the referenced artifact should be used.
				if (Canonicals.getVersion(resourceReference) == null || Canonicals.getVersion(resourceReference).isEmpty()) {
					List<MetadataResource> matchingResources =
						getResourcesFromBundle(searchResourceByUrl(resourceReference.getValueAsString(), fhirDal));
					if (matchingResources.isEmpty()) {
						throw new ResourceNotFoundException(
							String.format("Resource with URL '%s' is referenced by resource '%s', but is not found.",
								resourceReference.getValueAsString(),
								artifactAdapter.resource.getUrl()));
					} else {
						// TODO: Log which version was selected
						matchingResources.sort(comparing(r -> ((MetadataResource) r).getVersion()).reversed());
						referencedResource = matchingResources.get(0);
						String releaseVersion = getReleaseVersion(version, versionBehavior, referencedResource.getVersion());
						reference = String.format("%s|%s", referencedResource.getUrl(), releaseVersion);
					}
				} else {
					referencedResource = getResourcesFromBundle(searchResourceByUrl(reference, fhirDal)).get(0);
				}

				resolvedRelatedArtifacts.add(new RelatedArtifact().setType(RelatedArtifact.RelatedArtifactType.DEPENDSON).setResource(reference));
				KnowledgeArtifactAdapter<MetadataResource> searchResultAdapter = new KnowledgeArtifactAdapter<>(referencedResource);
				resolvedRelatedArtifacts.addAll(internalRelease(searchResultAdapter, version, versionBehavior, latestFromTxServer, fhirDal));
			}
		}

		for (RelatedArtifact ra : artifactAdapter.getDependencies()) {
			if (ra.hasResource()) {
				MetadataResource referencedResource;
				CanonicalType resourceReference = ra.getResourceElement();
				String reference = resourceReference.getValueAsString();

				// For dependencies, if a specific version is referenced, use it. Otherwise, if latest-from-tx-server is true
				// then lookup the latest version from tx server else get the latest version from our cache.
				if (Canonicals.getVersion(resourceReference) == null || Canonicals.getVersion(resourceReference).isEmpty()) {
					if (latestFromTxServer) {
						throw new NotImplementedException("Support for 'latestFromTxServer' is not yet implemented.");
						// TODO: Will need to query the configured (will need to know the configured TxServer from client) TxServer
						// to get the latest version of the ValueSet, download it into the cache - will need to augment the same way
						// as client.
					} else {
						List<MetadataResource> matchingResources =
							getResourcesFromBundle(searchResourceByUrlAndStatus(reference, "active", fhirDal));
						if (matchingResources.isEmpty()) {
							throw new ResourceNotFoundException(
								String.format("Resource with URL '%s' is referenced by resource '%s', but no active version of that resource is found.",
									resourceReference.getValueAsString(),
									artifactAdapter.resource.getUrl()));
						} else {
							// TODO: Log which version was selected
							matchingResources.sort(comparing(r -> ((MetadataResource) r).getVersion()).reversed());
							referencedResource = matchingResources.get(0);
							String releaseVersion = getReleaseVersion(version, versionBehavior, referencedResource.getVersion());
							reference = String.format("%s|%s", referencedResource.getUrl(), releaseVersion);
						}
					}
				} else {
					// Search for the referenced resource and validate that it is eligible for release (i.e., Active).
					List<MetadataResource> matchingResources = getResourcesFromBundle(searchResourceByUrl(reference, fhirDal));

					if (matchingResources.isEmpty()) {
						throw new ResourceNotFoundException(
							String.format("Resource with URL '%s' is referenced by resource '%s', but no active version of that resource is found.",
								resourceReference.getValueAsString(),
								artifactAdapter.resource.getUrl()));
					} else {
						// TODO: Log which version was selected
						matchingResources.sort(comparing(r -> ((MetadataResource) r).getVersion()).reversed());
						referencedResource = matchingResources.get(0);
						String releaseVersion = getReleaseVersion(version, versionBehavior, referencedResource.getVersion());
						reference = String.format("%s|%s", referencedResource.getUrl(), releaseVersion);
					}

					if (referencedResource.getStatus() != Enumerations.PublicationStatus.ACTIVE) {
						throw new IllegalStateException(String.format("Resource '%s' is not in active status and cannot be reference in this release.", reference));
					}
				}

				resolvedRelatedArtifacts.add(new RelatedArtifact().setType(RelatedArtifact.RelatedArtifactType.DEPENDSON).setResource(reference));
			}
		}

		return resolvedRelatedArtifacts;
	}

	private List<MetadataResource> getResourcesFromBundle(Bundle bundle) {
		List<MetadataResource> resourceList = new ArrayList<>();

		if (!bundle.getEntryFirstRep().isEmpty()) {
			List<Bundle.BundleEntryComponent> referencedResourceEntries = bundle.getEntry();
			for (Bundle.BundleEntryComponent entry: referencedResourceEntries) {
				if (entry.hasResource() && entry.getResource() instanceof MetadataResource) {
					MetadataResource referencedResource = (MetadataResource) entry.getResource();
					resourceList.add(referencedResource);
				}
			}
		}

		return resourceList;
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
