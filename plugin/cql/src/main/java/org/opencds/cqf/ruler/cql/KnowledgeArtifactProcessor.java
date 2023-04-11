package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.apache.commons.lang3.NotImplementedException;
import org.cqframework.fhir.api.FhirDal;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.ContactDetail;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.ValueSet;
import org.jetbrains.annotations.Nullable;
import org.opencds.cqf.cql.engine.exception.InvalidOperatorArgument;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.ruler.builder.BundleBuilder;
import org.opencds.cqf.ruler.cql.r4.ArtifactCommentExtension;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.web.client.ResourceAccessException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

@Configurable
// TODO: This belongs in the Evaluator. Only included in Ruler at dev time for
// shorter cycle.
public class KnowledgeArtifactProcessor {
	public static final String CPG_INFERENCEEXPRESSION = "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-inferenceExpression";
	public static final String CPG_ASSERTIONEXPRESSION = "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-assertionExpression";
	public static final String CPG_FEATUREEXPRESSION = "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-featureExpression";

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

	private MetadataResource retrieveResourcesByReference(String reference, FhirDal fhirDal) {
		MetadataResource resource = null;

		Bundle referencedResourceBundle = searchResourceByUrl(reference, fhirDal);
		MetadataResource referencedResource = KnowledgeArtifactAdapter.findLatestVersion(referencedResourceBundle);
		if (referencedResource != null) {
			resource = referencedResource;
		} else {
			throw new ResourceNotFoundException(String.format("Resource for Canonical '%s' not found.", reference));
		}

		return resource;
	}

	/* approve */
	/*
	 * The operation sets the date and approvalDate elements of the approved
	 * artifact, and is otherwise only allowed to add artifactComment elements
	 * to the artifact and to add or update an endorser.
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
		if (approvalDate == null){
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
			ArtifactCommentExtension artifactCommentExtension = new ArtifactCommentExtension(artifactCommentType, artifactCommentText, artifactCommentTarget, artifactCommentReference, artifactCommentUser);
			if (!artifactCommentExtension.getExtension().isEmpty()) {
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
			Bundle.BundleEntryComponent referencedResourceEntry = referencedResourceBundle.getEntryFirstRep();
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
		List<RelatedArtifact> resolvedRelatedArtifacts = new ArrayList<>();

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

	/* $package */
	public IBaseBundle packageOperation(FhirDal fhirDal, IdType idType) {
		MetadataResource resource = (MetadataResource) fhirDal.read(idType);
		if (resource.getId() == null ||  resource.getId().isEmpty()) {
			throw new ResourceAccessException(String.format("A valid resource ID is required."));
		}
		if (resource == null) {
			throw new IllegalArgumentException(String.format("Resource with ID: '%s' not found.", idType.getId()));
		}

		List<MetadataResource> resources;
		resources = internalPackage(resource, fhirDal);

		// TODO: Validate all resources that declare conformance to a profile.

		Bundle packageBundle = new BundleBuilder<>(Bundle.class)
			.withType(Bundle.BundleType.COLLECTION.toString())
			.build();
		for (MetadataResource resourceToAdd : resources) {
			// Check if the metadata resource has already been added to the bundle
			boolean entryExists = packageBundle.getEntry().stream().anyMatch(
				e -> e.hasResource()
					&& ((MetadataResource)e.getResource()).getUrl().equals(resourceToAdd.getUrl())
					&& ((MetadataResource)e.getResource()).getVersion().equals(resourceToAdd.getVersion())
			);

			if (!entryExists) {
				// Create a new BundleEntryComponent for the metadata resource
				Bundle.BundleEntryComponent bundleEntry = new Bundle.BundleEntryComponent();
				bundleEntry.setResource(resourceToAdd);
				bundleEntry.getRequest().setMethod(Bundle.HTTPVerb.POST);
				packageBundle.addEntry(bundleEntry);
			}
		}

		return packageBundle;
	}

	private List<MetadataResource> internalPackage(MetadataResource resource, FhirDal fhirDal) {
		List<MetadataResource> resources = new ArrayList<>();
		resources.add(resource);

		List<String> references = new ArrayList<>();
		switch (resource.getClass().getSimpleName()) {
//			case "ActivityDefinition":
//				((ActivityDefinition) resource).setApprovalDate(theApprovalDate);
//				break;
			case "Library":
				references.addAll(getLibraryDependencies((Library)resource));
				break;
			case "PlanDefinition":
				references.addAll(getPlanDefinitionDependencies((PlanDefinition)resource));
				break;
			case "StructureDefinition":
				references.addAll(getStructureDefinitionDependencies((StructureDefinition)resource));
				break;
			case "ValueSet":
				references.addAll(getValueSetDependencies((ValueSet)resource));
		}

		for (String reference : references) {
			MetadataResource referencedResource = retrieveResourcesByReference(reference, fhirDal);
			resources.addAll(internalPackage(referencedResource, fhirDal));
		}

		return resources;
	}

	private List<String> getRelatedArtifactReferences(MetadataResource resource) {
		KnowledgeArtifactAdapter<MetadataResource> adapter = new KnowledgeArtifactAdapter<>(resource);
		List<String> references = new ArrayList<>();

		for (RelatedArtifact ra : adapter.getRelatedArtifact()) {
			// If it is a composed-of relation then do a deep copy, else shallow
			if (ra.getType() == RelatedArtifact.RelatedArtifactType.COMPOSEDOF
				|| ra.getType() == RelatedArtifact.RelatedArtifactType.DEPENDSON) {
				if (ra.hasUrl()) {
					references.add(ra.getUrl());
//					Bundle referencedResourceBundle = searchResourceByUrl(ra.getUrl(), fhirDal);
//					processReferencedResourceForPackage(referencedResourceBundle, resources, fhirDal);
				} else if (ra.hasResource()) {
					references.add(ra.getResourceElement().getValueAsString());
//					Bundle referencedResourceBundle = searchResourceByUrl(ra.getResourceElement().getValueAsString(), fhirDal);
//					processReferencedResourceForPackage(referencedResourceBundle, resources, fhirDal);
				}
			}
		}

		return references;
	}

	/* Library Package
	* 		relatedArtifact[].resource
	*		dataRequirement[].profile[]
	*		dataRequirement[].codeFilter[].valueSet
	*/
	private List<String> getLibraryDependencies(Library library) {
		List<String> references = new ArrayList<>();

		// relatedArtifact[].resource
		references.addAll(getRelatedArtifactReferences(library));

		// dataRequirement
		List<DataRequirement> dataRequirements = library.getDataRequirement();
		for (DataRequirement dr : dataRequirements) {
			// dataRequirement.profile[]
			List<CanonicalType> profiles = dr.getProfile();
			for (CanonicalType ct : profiles) {
				if (ct.hasValue()) {
					references.add(ct.getValue());
//					Bundle referencedResourceBundle = searchResourceByUrl(ct.getValue(), fhirDal);
//					MetadataResource referencedResource = KnowledgeArtifactAdapter.findLatestVersion(referencedResourceBundle);
//					if (referencedResource == null) {
//						throw new ResourceNotFoundException(String.format("Resource for Canonical '%s' not found.", ct.getValue()));
//					} else {
//						resources.addAll(internalPackage(referencedResource, fhirDal));
//					}
				}
			}

			// dataRequirement.codeFilter[].valueset
			List<DataRequirement.DataRequirementCodeFilterComponent> codeFilters = dr.getCodeFilter();
			for (DataRequirement.DataRequirementCodeFilterComponent cf : codeFilters) {
				if (cf.hasValueSet()) {
					references.add(cf.getValueSet());
//					Bundle referencedResourceBundle = searchResourceByUrl(cf.getValueSet(), fhirDal);
//					MetadataResource referencedResource = KnowledgeArtifactAdapter.findLatestVersion(referencedResourceBundle);
//					if (referencedResource == null) {
//						throw new ResourceNotFoundException(String.format("Resource for Canonical '%s' not found.", cf.getValueSet()));
//					} else {
//						resources.addAll(internalPackage(referencedResource, fhirDal));
//					}
				}
			}
		}
		return references;
	}

	/* StructureDefinition Package
	* 		baseDefinition
	* 		differential.element[].constraint[].source
	* 		differential.element[].binding.valueSet
	* 		extension[cpg-inferenceExpression].reference
	* 		extension[cpg-assertionExpression].reference
 	* 		extension[cpg-featureExpression].reference
	*/
	private List<String> getStructureDefinitionDependencies(StructureDefinition structureDefinition) {
		/* baseDefinition */
		List<String> references = new ArrayList<>();
//		String baseDefinition = structureDefinition.getBaseDefinition();
//		references.add(baseDefinition);

		/* differential.element[] */
		List<ElementDefinition> elements = structureDefinition.getDifferential().getElement();
		for (ElementDefinition element : elements) {
			/* differential.element[].constraint[].source */
			List<ElementDefinition.ElementDefinitionConstraintComponent> constraints = element.getConstraint();
			for (ElementDefinition.ElementDefinitionConstraintComponent constraint : constraints) {
				if (constraint.hasSource()) {
					references.add(constraint.getSource());
				}
			}

			/* differential.element[].binding.valueSet */
			if (element.hasBinding()) {
				ElementDefinition.ElementDefinitionBindingComponent binding = element.getBinding();
				if (binding.hasValueSet()) {
					references.add(binding.getValueSet());
				}
			}
		}

		/* extension */
		List<Extension> relevantExtensions = new ArrayList<>();
		relevantExtensions.addAll(structureDefinition.getExtensionsByUrl(CPG_INFERENCEEXPRESSION));
		relevantExtensions.addAll(structureDefinition.getExtensionsByUrl(CPG_ASSERTIONEXPRESSION));
		relevantExtensions.addAll(structureDefinition.getExtensionsByUrl(CPG_FEATUREEXPRESSION));

		for (Extension extension : relevantExtensions) {
			if (extension.hasValue()) {
				Type value = extension.getValue();

				if (value instanceof Expression) {
					if (((Expression)value).hasReference()) {
						references.add(((Expression)value).getReference());
					}
				}
			}
		}

		return references;
	}

	/* PlanDefinition Package */
	private List<String> getPlanDefinitionDependencies(PlanDefinition planDefinition) {
		List<String> references = new ArrayList<>();
		List<CanonicalType> libraries = planDefinition.getLibrary();
		for (CanonicalType ct : libraries) {
			if (ct.hasValue()) {
				references.add(ct.getValue());
			}
		}

		references.addAll(getRelatedArtifactReferences(planDefinition));
		return references;
	}

	/* ValueSet Package */
	private List<String> getValueSetDependencies(ValueSet valueSet) {
		List<String> references = new ArrayList<>();
		if (valueSet.hasCompose()) {
			ValueSet.ValueSetComposeComponent compose = valueSet.getCompose();
			if (compose.hasInclude()) {
				for (ValueSet.ConceptSetComponent include : compose.getInclude()) {
					if (include.hasValueSet()) {
						for (CanonicalType reference : include.getValueSet()) {
							references.add(reference.getValue());
						}
					}
				}
			}

			if (compose.hasExclude()) {
				for (ValueSet.ConceptSetComponent exclude : compose.getExclude()) {
					if (exclude.hasValueSet()) {
						for (CanonicalType reference : exclude.getValueSet()) {
							references.add(reference.getValue());
						}
					}
				}
			}

			references.addAll(getRelatedArtifactReferences(valueSet));
		}
		return references;
	}
}
