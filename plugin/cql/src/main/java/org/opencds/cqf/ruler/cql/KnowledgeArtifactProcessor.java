package org.opencds.cqf.ruler.cql;
import static java.util.Comparator.comparing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cqframework.fhir.api.FhirDal;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MarkdownType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.UsageContext;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.ruler.cql.r4.ArtifactAssessment;
import org.opencds.cqf.ruler.cql.r4.ArtifactAssessment.ArtifactAssessmentContentInformationType;
import org.springframework.beans.factory.annotation.Configurable;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

@Configurable
// TODO: This belongs in the Evaluator. Only included in Ruler at dev time for
// shorter cycle.
public class KnowledgeArtifactProcessor {
	public static final String CPG_INFERENCEEXPRESSION = "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-inferenceExpression";
	public static final String CPG_ASSERTIONEXPRESSION = "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-assertionExpression";
	public static final String CPG_FEATUREEXPRESSION = "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-featureExpression";
	private BundleEntryComponent createEntry(IBaseResource theResource) {
		return new Bundle.BundleEntryComponent()
				.setResource((Resource) theResource)
				.setRequest(createRequest(theResource));
	}

	private BundleEntryRequestComponent createRequest(IBaseResource theResource) {
		Bundle.BundleEntryRequestComponent request = new Bundle.BundleEntryRequestComponent();
		if (theResource.getIdElement().hasValue() && !theResource.getIdElement().getValue().contains("urn:uuid")) {
			request
					.setMethod(Bundle.HTTPVerb.PUT)
					.setUrl(theResource.getIdElement().getValue());
		} else {
			request
					.setMethod(Bundle.HTTPVerb.POST)
					.setUrl(theResource.fhirType());
		}
		return request;
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

	private MetadataResource retrieveResourcesByCanonical(String reference, FhirDal fhirDal) throws ResourceNotFoundException {
		Bundle referencedResourceBundle = searchResourceByUrl(reference, fhirDal);
		Optional<MetadataResource> referencedResource = KnowledgeArtifactAdapter.findLatestVersion(referencedResourceBundle);
		if (referencedResource.isEmpty()) {
			throw new ResourceNotFoundException(String.format("Resource for Canonical '%s' not found.", reference));
		}
		return referencedResource.get();
	}

	/* approve */
	/*
	 * The operation sets the date and approvalDate elements of the approved
	 * artifact, and is otherwise only allowed to add artifactComment elements
	 * to the artifact and to add or update an endorser.
	 */
	public MetadataResource approve(MetadataResource resource, IPrimitiveType<Date> approvalDate, ArtifactAssessment assessment) {

		KnowledgeArtifactAdapter<MetadataResource> targetResourceAdapter = new KnowledgeArtifactAdapter<MetadataResource>(resource);
		Date currentDate = new Date();

		// 1. Set approvalDate
		if (approvalDate == null){
			targetResourceAdapter.setApprovalDate(currentDate);
		} else {
			targetResourceAdapter.setApprovalDate(approvalDate.getValue());
		}

		// 2. Set date
		DateTimeType theDate = new DateTimeType(currentDate, TemporalPrecisionEnum.DAY);
		resource.setDateElement(theDate);
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
	/*
	 * The operation creates a draft of the Base Artifact and
	 * related resources.
	 * 
	 * This method generates the transaction bundle for this operation.
	 * 
	 * This bundle consists of:
	 *  1. A new version of the base artifact where status is changed to
	 *     draft and version changed to a new version number + "-draft"
	 * 
	 *  2. New versions of owned related artifacts where status is changed to
	 *     draft and version changed to a new version number + "-draft"
	 * 
	 * Links and references between Bundle resources are updated to point to
	 * the new versions.
	 */
	public Bundle createDraftBundle(IdType baseArtifactId, FhirDal fhirDal, String version) throws ResourceNotFoundException, UnprocessableEntityException {
		checkVersionValidSemver(version);
		MetadataResource baseArtifact = (MetadataResource) fhirDal.read(baseArtifactId);

		if (baseArtifact == null) {
			throw new ResourceNotFoundException(baseArtifactId);
		}
		String draftVersion = version + "-draft";
		String draftVersionUrl = Canonicals.getUrl(baseArtifact.getUrl()) + "|" + draftVersion;

		// Root artifact must have status of 'Active'. Existing drafts of
		// reference artifacts with the right verison number will be adopted.
		// This check is performed here to facilitate that different treatment
		// for the root artifact and those referenced by it.
		if (baseArtifact.getStatus() != Enumerations.PublicationStatus.ACTIVE) {
			throw new PreconditionFailedException(
				String.format("Drafts can only be created from artifacts with status of 'active'. Resource '%s' has a status of: %s", baseArtifact.getUrl(), String.valueOf(baseArtifact.getStatus())));
		}
		// Ensure only one resource exists with this URL
		Bundle existingArtifactsForUrl = searchResourceByUrl(draftVersionUrl, fhirDal);
		if(existingArtifactsForUrl.getEntry().size() != 0){
			throw new PreconditionFailedException(
				String.format("A draft of Program '%s' already exists with version: '%s'. Only one draft of a program version can exist at a time.", baseArtifact.getUrl(), draftVersionUrl));
		}
		List<MetadataResource> resourcesToCreate = createDraftsOfArtifactAndRelated(baseArtifact, fhirDal, version, new ArrayList<MetadataResource>());
		Bundle transactionBundle = new Bundle()
			.setType(Bundle.BundleType.TRANSACTION);
		List<IdType> urnList = resourcesToCreate.stream().map(res -> new IdType("urn:uuid:" + UUID.randomUUID().toString())).collect(Collectors.toList());
		for(int i = 0; i < resourcesToCreate.size(); i++){
			KnowledgeArtifactAdapter<MetadataResource> newResourceAdapter = new KnowledgeArtifactAdapter<MetadataResource>(resourcesToCreate.get(i));
			updateUsageContextReferencesWithUrns(resourcesToCreate.get(i), resourcesToCreate, urnList);
			updateRelatedArtifactUrlsWithNewVersions(newResourceAdapter.getOwnedRelatedArtifacts(), draftVersion);
			MetadataResource updateIdForBundle = newResourceAdapter.copy();
			updateIdForBundle.setId(urnList.get(i));
			transactionBundle.addEntry(createEntry(updateIdForBundle));
		}
		return transactionBundle;
	}
	private void updateUsageContextReferencesWithUrns(MetadataResource newResource, List<MetadataResource> resourceListWithOriginalIds, List<IdType> idListForTransactionBundle){
		List<UsageContext> useContexts = newResource.getUseContext();
		for(UsageContext useContext : useContexts){
			// TODO: will we ever need to resolve these references?
			if(useContext.hasValueReference()){
				Reference useContextRef = useContext.getValueReference();
				if(useContextRef != null){
					resourceListWithOriginalIds.stream()
						.filter(resource -> (resource.getClass().getSimpleName() + "/" + resource.getIdElement().getIdPart()).equals(useContextRef.getReference()))
						.findAny()
						.ifPresent(resource -> {
							int indexOfDraftInIdList = resourceListWithOriginalIds.indexOf(resource);
							useContext.setValue(new Reference(idListForTransactionBundle.get(indexOfDraftInIdList)));
						});
				}
			}
		}
	}
	private void updateRelatedArtifactUrlsWithNewVersions(List<RelatedArtifact> relatedArtifactList, String updatedVersion){
			// For each  relatedArtifact, update the version of the reference.
			relatedArtifactList.stream()
				.filter(ra -> ra.hasResource())
				.collect(Collectors.toList())
				.replaceAll(ra -> ra.setResource(Canonicals.getUrl(ra.getResource()) + "|" + updatedVersion));
	}
	private void checkVersionValidSemver(String version) throws UnprocessableEntityException{
		if (version == null || version.isEmpty()) {
			throw new UnprocessableEntityException("The version argument is required");
		}
		if(version.contains("draft")){
			throw new UnprocessableEntityException("The version cannot contain 'draft'");
		}
		if(version.contains("/") || version.contains("\\") || version.contains("|")){
			throw new UnprocessableEntityException("The version contains illegal characters");
		}
		if (!version.contains(".")) {
				throw new UnprocessableEntityException("The version must be in the format MAJOR.MINOR.PATCH");
		} else {
			String[] versionParts = version.split("\\.");
			if(versionParts.length != 3){
				throw new UnprocessableEntityException("The version must be in the format MAJOR.MINOR.PATCH");
			}
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
					throw new UnprocessableEntityException("The " + section + " version part should be greater than 0.");
				}
			}
		}
	}
	private List<MetadataResource> createDraftsOfArtifactAndRelated(MetadataResource resourceToDraft, FhirDal fhirDal, String version, List<MetadataResource> resourcesToCreate) {
		String draftVersion = version + "-draft";
		String draftVersionUrl = Canonicals.getUrl(resourceToDraft.getUrl()) + "|" + draftVersion;

		// TODO: Decide if we need both of these checks
		Optional<MetadataResource> existingArtifactsWithMatchingUrl = KnowledgeArtifactAdapter.findLatestVersion(searchResourceByUrl(draftVersionUrl, fhirDal));
		Optional<MetadataResource> draftVersionAlreadyInBundle = resourcesToCreate.stream().filter(res -> res.getUrl().equals(Canonicals.getUrl(draftVersionUrl)) && res.getVersion().equals(draftVersion)).findAny();
		MetadataResource newResource = null;
		if (existingArtifactsWithMatchingUrl.isPresent()) {
			newResource = existingArtifactsWithMatchingUrl.get();
		} else if(draftVersionAlreadyInBundle.isPresent()){
			newResource = draftVersionAlreadyInBundle.get();
		}

		if (newResource == null) {
			KnowledgeArtifactAdapter<MetadataResource> sourceResourceAdapter = new KnowledgeArtifactAdapter<>(resourceToDraft);
			newResource = sourceResourceAdapter.copy();
			newResource.setStatus(Enumerations.PublicationStatus.DRAFT);
			newResource.setVersion(draftVersion);
			resourcesToCreate.add(newResource);
			for (RelatedArtifact ra : sourceResourceAdapter.getOwnedRelatedArtifacts()) {
				// If it’s an owned RelatedArtifact composed-of then we want to copy it
				// If it’s not owned, we just want to reference it, but not copy it
				// (references are updated in createDraftBundle before adding to the bundle
				// hence they are ignored here)
				if (ra.hasUrl()) {
					Bundle referencedResourceBundle = searchResourceByUrl(ra.getUrl(), fhirDal);
					processReferencedResourceForDraft(fhirDal, referencedResourceBundle, ra, version, resourcesToCreate);
				} else if (ra.hasResource()) {
					Bundle referencedResourceBundle = searchResourceByUrl(ra.getResourceElement().getValueAsString(), fhirDal);
					processReferencedResourceForDraft(fhirDal, referencedResourceBundle, ra, version, resourcesToCreate);
				}
			}
		}

		return resourcesToCreate;
	}
	
	private void processReferencedResourceForDraft(FhirDal fhirDal, Bundle referencedResourceBundle, RelatedArtifact ra, String version, List<MetadataResource> transactionBundle) {
		if (!referencedResourceBundle.getEntryFirstRep().isEmpty()) {
			Bundle.BundleEntryComponent referencedResourceEntry = referencedResourceBundle.getEntry().get(0);
			if (referencedResourceEntry.hasResource() && referencedResourceEntry.getResource() instanceof MetadataResource) {
				MetadataResource referencedResource = (MetadataResource) referencedResourceEntry.getResource();

				createDraftsOfArtifactAndRelated(referencedResource, fhirDal, version, transactionBundle);
			}
		}
	}

	private Optional<String> getReleaseVersion(String version, CodeType versionBehavior, String existingVersion) throws UnprocessableEntityException {
		Optional<String> releaseVersion = Optional.ofNullable(null);
		// If no version exists use the version argument provided
		if (existingVersion == null || existingVersion.isEmpty() || existingVersion.isBlank()) {
			return Optional.ofNullable(version);
		}
		String replaceDraftInExisting = existingVersion.replace("-draft","");

		if (versionBehavior.getCode().equals("default")) {
			if(replaceDraftInExisting != null && !replaceDraftInExisting.isEmpty()){
				releaseVersion = Optional.of(replaceDraftInExisting);
			} else {
				releaseVersion = Optional.ofNullable(version);
			}
		} else if (versionBehavior.getCode().equals("force")) {
			releaseVersion = Optional.ofNullable(version);
		} else if (versionBehavior.getCode().equals("check")) {
			if (!replaceDraftInExisting.equals(version)) {
				throw new UnprocessableEntityException(String.format("versionBehavior specified is 'check' and the version provided ('%s') does not match the version currently specified on the root artifact ('%s').",version,existingVersion));
			}
		}
		return releaseVersion;
	}

	/* $release */
	/*
	 * The operation changes the state of a Base Artifact to active
	 * 
	 * This method generates the transaction bundle for this operation.
	 * 
	 * This bundle consists of:
	 *  1. A new version of the base artifact where status is changed to
	 *     active and version changed to a new version number and removing "-draft"
	 * 
	 *  2. New versions of owned related artifacts where status is changed to
	 *     active and version changed to a new version number removing "-draft"
	 * 
	 *  3. EffectivePeriod from the Base Artifact is propagated to all owned
	 *     RelatedArtifacts which do not specify their own effectivePeriod
	 * 
	 * Links and references between Bundle resources are updated to point to
	 * the new versions.
	 */
	public Bundle createReleaseBundle(IdType idType, String version, CodeType versionBehavior, boolean latestFromTxServer, FhirDal fhirDal) throws UnprocessableEntityException, ResourceNotFoundException, PreconditionFailedException {
		// TODO: This check is to avoid partial releases and should be removed once the argument is supported.
		if (latestFromTxServer) {
			throw new NotImplementedOperationException("Support for 'latestFromTxServer' is not yet implemented.");
		}
		checkReleaseVersion(version,versionBehavior);
		MetadataResource rootArtifact = (MetadataResource) fhirDal.read(idType);
		KnowledgeArtifactAdapter<MetadataResource> rootArtifactAdapter = new KnowledgeArtifactAdapter<>(rootArtifact);
		Date currentApprovalDate = rootArtifactAdapter.getApprovalDate();
		checkReleasePreconditions(rootArtifact, currentApprovalDate);

		// Determine which version should be used.
		String existingVersion = rootArtifact.hasVersion() ? rootArtifact.getVersion().replace("-draft","") : null;
		String releaseVersion = getReleaseVersion(version, versionBehavior, existingVersion)
			.orElseThrow(() -> new UnprocessableEntityException("Could not resolve a version for the root artifact."));
		Period rootEffectivePeriod = rootArtifactAdapter.getEffectivePeriod();
		List<MetadataResource> releasedResources = internalRelease(rootArtifactAdapter, releaseVersion, rootEffectivePeriod, versionBehavior, latestFromTxServer, fhirDal);

		// once iteration is complete, delete all depends-on RAs in the root artifact
		rootArtifactAdapter.getRelatedArtifact().removeIf(ra -> ra.getType() == RelatedArtifact.RelatedArtifactType.DEPENDSON);

		Bundle transactionBundle = new Bundle()
			.setType(Bundle.BundleType.TRANSACTION);
		for (MetadataResource artifact: releasedResources) {
			transactionBundle.addEntry(createEntry(artifact));

			KnowledgeArtifactAdapter<MetadataResource> artifactAdapter = new KnowledgeArtifactAdapter<MetadataResource>(artifact);
			List<RelatedArtifact> components = artifactAdapter.getComponents();
			// add all root artifact components
			// and child artifact components recursively
			// as root artifact dependencies
			for (RelatedArtifact component : components) {
				MetadataResource resource;
				// if the relatedArtifact is Owned, need to update
				// the reference to the new Version
				if (KnowledgeArtifactAdapter.checkIfRelatedArtifactIsOwned(component)) {
					resource = checkIfReferenceInList(component, releasedResources)
					// should never happen since we check all references
					// as part of `internalRelease`
					.orElseThrow(() -> new InternalErrorException("Owned resource reference not found during release"));
					String reference = String.format("%s|%s", resource.getUrl(), resource.getVersion());
					component.setResource(reference);
				} else if (Canonicals.getVersion(component.getResourceElement()) == null || Canonicals.getVersion(component.getResourceElement()).isEmpty()) {
					// if the not Owned component doesn't have a version,
					// try to find the latest version
					String updatedReference = tryUpdateReferenceToLatestActiveVersion(component.getResource(), fhirDal, artifact.getUrl());
					component.setResource(updatedReference);
				}
				RelatedArtifact componentToDependency = new RelatedArtifact().setType(RelatedArtifact.RelatedArtifactType.DEPENDSON).setResource(component.getResourceElement().getValueAsString());
				rootArtifactAdapter.getRelatedArtifact().add(componentToDependency);
			}

			List<RelatedArtifact> dependencies = artifactAdapter.getDependencies();
			for (RelatedArtifact dependency : dependencies) {
				// if the dependency gets updated as part of $release
				// then update the reference as well
				checkIfReferenceInList(dependency, releasedResources)
					.ifPresentOrElse((resource) -> {
						String updatedReference = String.format("%s|%s", resource.getUrl(), resource.getVersion());
						dependency.setResource(updatedReference);
					},
					// not present implies that
					// the dependency wasn't updated as part of $release
					() -> {
						// if the dependency doesn't have a version,
						// try to find the latest version
						if (Canonicals.getVersion(dependency.getResourceElement()) == null || Canonicals.getVersion(dependency.getResourceElement()).isEmpty()) {
							// TODO: update when we support expansionParameters and requireVersionedDependencies
							String updatedReference = tryUpdateReferenceToLatestActiveVersion(dependency.getResource(), fhirDal, artifact.getUrl());
							dependency.setResource(updatedReference);
						}
					});
				// only add the dependency to the manifest if it is
				// from a leaf artifact
				if (!artifact.getUrl().equals(rootArtifact.getUrl())) {
					rootArtifactAdapter.getRelatedArtifact().add(dependency);
				}
			}
		}
		// removed duplicates and add
		List<RelatedArtifact> distinctResolvedRelatedArtifacts = new ArrayList<>();
		for (RelatedArtifact ra: rootArtifactAdapter.getRelatedArtifact()) {
			if (!distinctResolvedRelatedArtifacts.stream().anyMatch(r -> r.getResource().equals(ra.getResource()) && r.getType().equals(ra.getType()))) {
				distinctResolvedRelatedArtifacts.add(ra);
			}
		}
		rootArtifactAdapter.setRelatedArtifact(distinctResolvedRelatedArtifacts);
		return transactionBundle;
	}
	private void checkReleaseVersion(String version,CodeType versionBehavior) throws UnprocessableEntityException {
		if (versionBehavior == null || versionBehavior.getCode() == null || versionBehavior.getCode().isEmpty()) {
			throw new UnprocessableEntityException("'versionBehavior' must be provided as an argument to the $release operation. Valid values are 'default', 'check', 'force'.");
		}

		if (!versionBehavior.getCode().equals("default") && !versionBehavior.getCode().equals("check") && !versionBehavior.getCode().equals("force")) {
			throw new UnprocessableEntityException(String.format("'%s' is not a valid versionBehavior. Valid values are 'default', 'check', 'force'", versionBehavior.getCode()));
		}
		checkVersionValidSemver(version);
	}
	private void checkReleasePreconditions(MetadataResource artifact, Date approvalDate) throws PreconditionFailedException {
		if (artifact == null) {
			throw new ResourceNotFoundException("Resource not found.");
		}
		if(!artifact.getStatus().equals(Enumerations.PublicationStatus.DRAFT)){
			throw new PreconditionFailedException(String.format("Resource with ID: '%s' does not have a status of 'draft'.", artifact.getIdElement().getIdPart()));
		}
		if (approvalDate == null) {
			throw new PreconditionFailedException(String.format("The artifact must be approved (indicated by approvalDate) before it is eligible for release."));
		}
		if (approvalDate.before(artifact.getDate())) {
			throw new PreconditionFailedException(
				String.format("The artifact was approved on '%s', but was last modified on '%s'. An approval must be provided after the most-recent update.", approvalDate, artifact.getDate()));
		}
	}
	private List<MetadataResource> internalRelease(KnowledgeArtifactAdapter<MetadataResource> artifactAdapter, String version, Period rootEffectivePeriod,
																 CodeType versionBehavior, boolean latestFromTxServer, FhirDal fhirDal) throws NotImplementedOperationException, ResourceNotFoundException {
		List<MetadataResource> resourcesToUpdate = new ArrayList<MetadataResource>();

		// Step 1: Update the Date and the version
		// Need to update the Date element because we're changing the status
		artifactAdapter.resource.setDate(new Date());
		artifactAdapter.resource.setStatus(Enumerations.PublicationStatus.ACTIVE);
		artifactAdapter.resource.setVersion(version);

		// Step 2: propagate effectivePeriod if it doesn't exist
		Period effectivePeriod = artifactAdapter.getEffectivePeriod();
		// if the root artifact period is NOT null AND HAS a start or an end date
		if((rootEffectivePeriod != null && (rootEffectivePeriod.hasStart() || rootEffectivePeriod.hasEnd()))
		// and the current artifact period IS null OR does NOT HAVE a start or an end date
		&& (effectivePeriod == null || !(effectivePeriod.hasStart() || effectivePeriod.hasEnd()))){
			artifactAdapter.setEffectivePeriod(rootEffectivePeriod);
		}

		resourcesToUpdate.add(artifactAdapter.resource);

		// Step 3 : Get all the OWNED relatedArtifacts

		for (RelatedArtifact ownedRelatedArtifact : artifactAdapter.getOwnedRelatedArtifacts()) {
			if (ownedRelatedArtifact.hasResource()) {
				MetadataResource referencedResource;
				CanonicalType ownedResourceReference = ownedRelatedArtifact.getResourceElement();
				Boolean alreadyUpdated = resourcesToUpdate
					.stream()
					.filter(r -> r.getUrl().equals(Canonicals.getUrl(ownedResourceReference)))
					.findAny()
					.isPresent();
				if(!alreadyUpdated) {
					// For composition references, if a version is not specified in the reference then the latest version
					// of the referenced artifact should be used. If a version is specified then `searchResourceByUrl` will
					// return that version.
					referencedResource = KnowledgeArtifactAdapter.findLatestVersion(searchResourceByUrl(ownedResourceReference.getValueAsString(), fhirDal))
					.orElseThrow(()-> new ResourceNotFoundException(
							String.format("Resource with URL '%s' is Owned by this repository and referenced by resource '%s', but was not found on the server.",
								ownedResourceReference.getValueAsString(),
								artifactAdapter.resource.getUrl()))
					);
					KnowledgeArtifactAdapter<MetadataResource> searchResultAdapter = new KnowledgeArtifactAdapter<>(referencedResource);
					resourcesToUpdate.addAll(internalRelease(searchResultAdapter, version, rootEffectivePeriod, versionBehavior, latestFromTxServer, fhirDal));
				}
			}
		}

		return resourcesToUpdate;
	}
	private String tryUpdateReferenceToLatestActiveVersion(String inputReference, FhirDal fhirDal, String sourceArtifactUrl) throws ResourceNotFoundException {
		// List<MetadataResource> matchingResources = getResourcesFromBundle(searchResourceByUrlAndStatus(inputReference, "active", fhirDal));
		// using filtered list until APHL-601 (searchResourceByUrlAndStatus bug) resolved
		List<MetadataResource> matchingResources = getResourcesFromBundle(searchResourceByUrl(inputReference, fhirDal))
			.stream()
			.filter(r -> r.getStatus().equals(Enumerations.PublicationStatus.ACTIVE))
			.collect(Collectors.toList());

		if (matchingResources.isEmpty()) {
			return inputReference;
		} else {
			// TODO: Log which version was selected
			matchingResources.sort(comparing(r -> ((MetadataResource) r).getVersion()).reversed());
			MetadataResource latestActiveVersion = matchingResources.get(0);
			String latestActiveReference = String.format("%s|%s", latestActiveVersion.getUrl(), latestActiveVersion.getVersion());
			return latestActiveReference;
		}
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
	private Optional<MetadataResource> checkIfReferenceInList(RelatedArtifact artifactToUpdate, List<MetadataResource> resourceList){
		Optional<MetadataResource> updatedReference = Optional.ofNullable(null);
		for (MetadataResource resource : resourceList){
			String referenceURL = Canonicals.getUrl(artifactToUpdate.getResourceElement());
			String currentResourceURL = resource.getUrl();
			if (artifactToUpdate.hasResource() && referenceURL.equals(currentResourceURL)) {
				return Optional.of(resource);
			}
		}
		return updatedReference;
	}
	/* $package */
	public Bundle createPackageBundle(IdType id, FhirDal fhirDal, List<String> capability, List<String> include, List<CanonicalType> canonicalVersion, List<CanonicalType> checkCanonicalVersion, List<CanonicalType> forceCanonicalVersion, Integer count, Integer offset, Endpoint contentEndpoint, Endpoint terminologyEndpoint, Boolean packageOnly){
		if (contentEndpoint != null || terminologyEndpoint != null) {
			throw new NotImplementedOperationException("This repository is not implementing custom Content and Terminology endpoints at this time");
		}
		if (packageOnly != null) {
			throw new NotImplementedOperationException("This repository is not implementing packageOnly at this time");
		}
		MetadataResource resource = (MetadataResource) fhirDal.read(id);
		// TODO: In the case of a released (active) root Library we can depend on the relatedArtifacts as a comprehensive manifest
		Bundle packagedBundle = new Bundle()
			.setType(Bundle.BundleType.COLLECTION);
		if (include != null
			&& include.size() == 1
			&& include.stream().anyMatch((includedType) -> includedType.equals("artifact"))) {
			findUnsupportedCapability(resource, capability);
			processCanonicals(resource, canonicalVersion, checkCanonicalVersion, forceCanonicalVersion);
			BundleEntryComponent entry = createEntry(resource);
			// TODO: remove history from request.url
			entry.getRequest().setMethod(HTTPVerb.POST);
			entry.getRequest().setIfNoneExist("url="+resource.getUrl()+"&version="+resource.getVersion());
			packagedBundle.addEntry(entry);
		} else {
			recursivePackage(resource, packagedBundle, fhirDal, capability, include, canonicalVersion, checkCanonicalVersion, forceCanonicalVersion);
			List<BundleEntryComponent> included = findUnsupportedInclude(packagedBundle.getEntry(),include);
			packagedBundle.setEntry(included);
		}
		packagedBundle.setTotal(packagedBundle.getEntry().size());
		if (offset != null) {
			List<BundleEntryComponent> entries = packagedBundle.getEntry();
			Integer bundleSize = entries.size();
			if (offset < bundleSize) {
				packagedBundle.setEntry(entries.subList(offset, bundleSize));
			} else {
				packagedBundle.setEntry(Arrays.asList());
			}
		}
		if (count != null) {
			// repeat these two from earlier
			// because we might modify / replace the
			// entries list at any time
			List<BundleEntryComponent> entries = packagedBundle.getEntry();
			Integer bundleSize = entries.size();
			if (count < bundleSize){
				packagedBundle.setEntry(entries.subList(0, count));
			} else {
				// there are not enough entries in the bundle
				// to page so we return all of them
				// no change
			}
		}
		return packagedBundle;
	}
	void recursivePackage(
		MetadataResource resource,
		Bundle bundle,
		FhirDal fhirDal,
		List<String> capability,
		List<String> include,
		List<CanonicalType> canonicalVersion,
		List<CanonicalType> checkCanonicalVersion,
		List<CanonicalType> forceCanonicalVersion
		){
		if (resource != null) {
			KnowledgeArtifactAdapter<MetadataResource> adapter = new KnowledgeArtifactAdapter<MetadataResource>(resource);
			findUnsupportedCapability(resource, capability);
			processCanonicals(resource, canonicalVersion, checkCanonicalVersion, forceCanonicalVersion);
			boolean entryExists = bundle.getEntry().stream()
				.map(e -> (MetadataResource)e.getResource())
				.filter(mr -> mr.getUrl() != null && mr.getVersion() != null)
				.anyMatch(mr -> mr.getUrl().equals(resource.getUrl()) && mr.getVersion().equals(resource.getVersion()));
			if (!entryExists) {
				BundleEntryComponent entry = createEntry(resource);
				// TODO: remove history from request.url
				entry.getRequest().setMethod(HTTPVerb.POST);
				entry.getRequest().setIfNoneExist("url="+resource.getUrl()+"&version="+resource.getVersion());
				bundle.addEntry(entry);
			}
			List<RelatedArtifact> components = adapter.getComponents();
			List<RelatedArtifact> dependencies = adapter.getDependencies();
			Stream.concat(components.stream(), dependencies.stream())
				.map(ra -> searchResourceByUrl(ra.getResource(), fhirDal))
				.map(searchBundle -> searchBundle.getEntry().stream().findFirst().orElseGet(()-> new BundleEntryComponent()).getResource())
				.forEach(component -> recursivePackage((MetadataResource)component, bundle, fhirDal, capability, include, canonicalVersion, checkCanonicalVersion, forceCanonicalVersion));
		}
	}
	private Optional<String> findVersionInListMatchingResource(List<CanonicalType> list, MetadataResource resource){
		return list.stream()
					.filter((canonical) -> Canonicals.getUrl(canonical).equals(resource.getUrl()))
					.map((canonical) -> Canonicals.getVersion(canonical))
					.findAny();
	}
	private void findUnsupportedCapability(MetadataResource resource, List<String> capability) throws PreconditionFailedException{
		if (capability != null) {
			List<Extension> knowledgeCapabilityExtension = resource.getExtension().stream()
			.filter(ext -> ext.getUrl().contains("cqf-knowledgeCapability"))
			.collect(Collectors.toList());
			if (knowledgeCapabilityExtension.isEmpty()) {
				// consider resource unsupported
				// if it's knowledgeCapability is
				// undefined
				throw new PreconditionFailedException(String.format("Resource with url: '%s' does not specify capability.", resource.getUrl()));
			}
			knowledgeCapabilityExtension.stream()
				.filter(ext -> !capability.contains(((CodeType) ext.getValue()).getValue()))
				.findAny()
				.ifPresent((ext) -> {
					throw new PreconditionFailedException(String.format("Resourc		e with url: '%s' is not one of '%s'.",
					resource.getUrl(),
					String.join(", ", capability)));
				});
		}
	}
	private void processCanonicals(MetadataResource resource, List<CanonicalType> canonicalVersion,  List<CanonicalType> checkCanonicalVersion,  List<CanonicalType> forceCanonicalVersion) throws UnprocessableEntityException {
		if (checkCanonicalVersion != null) {
			// check throws an error
			findVersionInListMatchingResource(checkCanonicalVersion, resource)
				.ifPresent((version) -> {
					if (!resource.getVersion().equals(version)) {
						throw new UnprocessableEntityException("The versions don't match!");
					}
				});
		} else if (forceCanonicalVersion != null) {
			// force just does a silent override
			findVersionInListMatchingResource(forceCanonicalVersion, resource)
				.ifPresent((version) -> resource.setVersion(version));
		} else if (canonicalVersion != null && !resource.hasVersion()) {
			// canonicalVersion adds a version if it's missing
			findVersionInListMatchingResource(canonicalVersion, resource)
				.ifPresent((version) -> resource.setVersion(version));
		}
	}
	private List<BundleEntryComponent> findUnsupportedInclude(List<BundleEntryComponent> entries, List<String> include){
		if (include == null
			|| include.stream().anyMatch((includedType) -> includedType.equals("all"))) {
			return entries;
		}
		List<BundleEntryComponent> filteredList = new ArrayList<>();
		entries.stream().forEach(entry -> {
			if (include.stream().anyMatch((type) -> type.equals("knowledge"))) {
				Boolean resourceIsKnowledgeType = entry.getResource().getResourceType().equals(ResourceType.Library)
				|| entry.getResource().getResourceType().equals(ResourceType.Measure)
				|| entry.getResource().getResourceType().equals(ResourceType.ActivityDefinition)
				|| entry.getResource().getResourceType().equals(ResourceType.PlanDefinition);
				if (resourceIsKnowledgeType) {
					filteredList.add(entry);
				}
			}
			// as per http://hl7.org/fhir/R4/resource.html#canonical
			if (include.stream().anyMatch((type) -> type.equals("canonical"))) {
				Boolean resourceIsCanonicalType = entry.getResource().getResourceType().equals(ResourceType.ActivityDefinition)
				|| entry.getResource().getResourceType().equals(ResourceType.CapabilityStatement)
				|| entry.getResource().getResourceType().equals(ResourceType.ChargeItemDefinition)
				|| entry.getResource().getResourceType().equals(ResourceType.CompartmentDefinition)
				|| entry.getResource().getResourceType().equals(ResourceType.ConceptMap)
				|| entry.getResource().getResourceType().equals(ResourceType.EffectEvidenceSynthesis)
				|| entry.getResource().getResourceType().equals(ResourceType.EventDefinition)
				|| entry.getResource().getResourceType().equals(ResourceType.Evidence)
				|| entry.getResource().getResourceType().equals(ResourceType.EvidenceVariable)
				|| entry.getResource().getResourceType().equals(ResourceType.ExampleScenario)
				|| entry.getResource().getResourceType().equals(ResourceType.GraphDefinition)
				|| entry.getResource().getResourceType().equals(ResourceType.ImplementationGuide)
				|| entry.getResource().getResourceType().equals(ResourceType.Library)
				|| entry.getResource().getResourceType().equals(ResourceType.Measure)
				|| entry.getResource().getResourceType().equals(ResourceType.MessageDefinition)
				|| entry.getResource().getResourceType().equals(ResourceType.NamingSystem)
				|| entry.getResource().getResourceType().equals(ResourceType.OperationDefinition)
				|| entry.getResource().getResourceType().equals(ResourceType.PlanDefinition)
				|| entry.getResource().getResourceType().equals(ResourceType.Questionnaire)
				|| entry.getResource().getResourceType().equals(ResourceType.ResearchDefinition)
				|| entry.getResource().getResourceType().equals(ResourceType.ResearchElementDefinition)
				|| entry.getResource().getResourceType().equals(ResourceType.RiskEvidenceSynthesis)
				|| entry.getResource().getResourceType().equals(ResourceType.SearchParameter)
				|| entry.getResource().getResourceType().equals(ResourceType.StructureDefinition)
				|| entry.getResource().getResourceType().equals(ResourceType.StructureMap)
				|| entry.getResource().getResourceType().equals(ResourceType.TerminologyCapabilities)
				|| entry.getResource().getResourceType().equals(ResourceType.TestScript)
				|| entry.getResource().getResourceType().equals(ResourceType.ValueSet);
				if (resourceIsCanonicalType) {
					filteredList.add(entry);
				}
			}
			if (include.stream().anyMatch((type) -> type.equals("terminology"))) {
				Boolean resourceIsTerminologyType = entry.getResource().getResourceType().equals(ResourceType.CodeSystem)
				|| entry.getResource().getResourceType().equals(ResourceType.ValueSet)
				|| entry.getResource().getResourceType().equals(ResourceType.ConceptMap)
				|| entry.getResource().getResourceType().equals(ResourceType.NamingSystem)
				|| entry.getResource().getResourceType().equals(ResourceType.TerminologyCapabilities);
				if (resourceIsTerminologyType) {
					filteredList.add(entry);
				}
			}
			if (include.stream().anyMatch((type) -> type.equals("conformance"))) {
				Boolean resourceIsConformanceType = entry.getResource().getResourceType().equals(ResourceType.CapabilityStatement)
				|| entry.getResource().getResourceType().equals(ResourceType.StructureDefinition)
				|| entry.getResource().getResourceType().equals(ResourceType.ImplementationGuide)
				|| entry.getResource().getResourceType().equals(ResourceType.SearchParameter)
				|| entry.getResource().getResourceType().equals(ResourceType.MessageDefinition)
				|| entry.getResource().getResourceType().equals(ResourceType.OperationDefinition)
				|| entry.getResource().getResourceType().equals(ResourceType.CompartmentDefinition)
				|| entry.getResource().getResourceType().equals(ResourceType.StructureMap)
				|| entry.getResource().getResourceType().equals(ResourceType.GraphDefinition)
				|| entry.getResource().getResourceType().equals(ResourceType.ExampleScenario);
				if (resourceIsConformanceType) {
					filteredList.add(entry);
				}
			}
			if (include.stream().anyMatch((type) -> type.equals("extensions"))
				&& entry.getResource().getResourceType().equals(ResourceType.StructureDefinition)
				&& ((StructureDefinition) entry.getResource()).getType().equals("Extension")) {
						filteredList.add(entry);
			}
			if (include.stream().anyMatch((type) -> type.equals("profiles"))
				&& entry.getResource().getResourceType().equals(ResourceType.StructureDefinition)
				&& !((StructureDefinition) entry.getResource()).getType().equals("Extension")) {
				filteredList.add(entry);
			}
			if (include.stream().anyMatch((type) -> type.equals("tests"))){
				if (entry.getResource().getResourceType().equals(ResourceType.Library)
					&& ((Library) entry.getResource()).getType().getCoding().stream().anyMatch(coding -> coding.getCode().equals("test-case"))) {
					filteredList.add(entry);
				} else if (((MetadataResource) entry.getResource()).getExtension().stream().anyMatch(ext -> ext.getUrl().contains("isTestCase")
					&& ((BooleanType) ext.getValue()).getValue())) {
					filteredList.add(entry);
				}
			}
			if (include.stream().anyMatch((type) -> type.equals("examples"))){
				// TODO: idk if this is legit just a placeholder for now
				if (((MetadataResource) entry.getResource()).getExtension().stream().anyMatch(ext -> ext.getUrl().contains("isExample")
					&& ((BooleanType) ext.getValue()).getValue())) {
					filteredList.add(entry);
				}
			}
		});
		List<BundleEntryComponent> distinctFilteredEntries = new ArrayList<>();
		// remove duplicates
		for (BundleEntryComponent entry: filteredList) {
			if (!distinctFilteredEntries.stream()
				.map((e) -> ((MetadataResource) e.getResource()))
				.anyMatch(existingEntry -> existingEntry.getUrl().equals(((MetadataResource) entry.getResource()).getUrl()) && existingEntry.getVersion().equals(((MetadataResource) entry.getResource()).getVersion()))
			) {
				distinctFilteredEntries.add(entry);
			}
		}
		return distinctFilteredEntries;
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
