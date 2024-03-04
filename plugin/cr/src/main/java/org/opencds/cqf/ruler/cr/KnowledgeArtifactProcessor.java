package org.opencds.cqf.ruler.cr;
import static java.util.Comparator.comparing;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Basic;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MarkdownType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.UsageContext;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetComposeComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.ruler.cr.r4.ArtifactAssessment;
import org.opencds.cqf.ruler.cr.r4.ArtifactAssessment.ArtifactAssessmentContentInformationType;
import org.opencds.cqf.ruler.cr.r4.CRMIReleaseExperimentalBehavior.CRMIReleaseExperimentalBehaviorCodes;
import org.opencds.cqf.ruler.cr.r4.CRMIReleaseVersionBehavior.CRMIReleaseVersionBehaviorCodes;
import org.opencds.cqf.ruler.cr.r4.helper.ResourceClassMapHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.ValueSetExpansionOptions;
import ca.uhn.fhir.cr.repo.HapiFhirRepository;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDaoValueSet;
import ca.uhn.fhir.jpa.patch.FhirPatch;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.parser.path.EncodeContextPath;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

@Configurable
// TODO: This belongs in the Evaluator. Only included in Ruler at dev time for shorter cycle.
public class KnowledgeArtifactProcessor {
	@Autowired
	private TerminologyServerClient terminologyServerClient;
	private Logger myLog = LoggerFactory.getLogger(KnowledgeArtifactProcessor.class);
	public static final String CPG_INFERENCEEXPRESSION = "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-inferenceExpression";
	public static final String CPG_ASSERTIONEXPRESSION = "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-assertionExpression";
	public static final String CPG_FEATUREEXPRESSION = "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-featureExpression";
	public static final String releaseLabelUrl = "http://hl7.org/fhir/StructureDefinition/artifact-releaseLabel";
	public static final String releaseDescriptionUrl = "http://hl7.org/fhir/StructureDefinition/artifact-releaseDescription";
	public static final String valueSetPriorityUrl = "http://aphl.org/fhir/vsm/StructureDefinition/vsm-valueset-priority";
	public static final String valueSetConditionUrl = "http://aphl.org/fhir/vsm/StructureDefinition/vsm-valueset-condition";
	public static final String valueSetPriorityCode = "priority";
	public static final String valueSetConditionCode = "focus";
	public final List<String> preservedExtensionUrls = List.of(
			valueSetPriorityUrl,
			valueSetConditionUrl
		);
	public static final String usPhContextTypeUrl = "http://hl7.org/fhir/us/ecr/CodeSystem/us-ph-usage-context-type";
	public static final String contextTypeUrl = "http://terminology.hl7.org/CodeSystem/usage-context-type";
	public static final String contextUrl = "http://hl7.org/fhir/us/ecr/CodeSystem/us-ph-usage-context";

	// as per http://hl7.org/fhir/R4/resource.html#canonical
	public static final List<ResourceType> canonicalResourceTypes =
		new ArrayList<>(
			List.of(
				ResourceType.ActivityDefinition,
				ResourceType.CapabilityStatement,
				ResourceType.ChargeItemDefinition,
				ResourceType.CompartmentDefinition,
				ResourceType.ConceptMap,
				ResourceType.EffectEvidenceSynthesis,
				ResourceType.EventDefinition,
				ResourceType.Evidence,
				ResourceType.EvidenceVariable,
				ResourceType.ExampleScenario,
				ResourceType.GraphDefinition,
				ResourceType.ImplementationGuide,
				ResourceType.Library,
				ResourceType.Measure,
				ResourceType.MessageDefinition,
				ResourceType.NamingSystem,
				ResourceType.OperationDefinition,
				ResourceType.PlanDefinition,
				ResourceType.Questionnaire,
				ResourceType.ResearchDefinition,
				ResourceType.ResearchElementDefinition,
				ResourceType.RiskEvidenceSynthesis,
				ResourceType.SearchParameter,
				ResourceType.StructureDefinition,
				ResourceType.StructureMap,
				ResourceType.TerminologyCapabilities,
				ResourceType.TestScript,
				ResourceType.ValueSet
			)
		);

	public static final List<ResourceType> conformanceResourceTypes =
		new ArrayList<>(
			List.of(
				ResourceType.CapabilityStatement,
				ResourceType.StructureDefinition,
				ResourceType.ImplementationGuide,
				ResourceType.SearchParameter,
				ResourceType.MessageDefinition,
				ResourceType.OperationDefinition,
				ResourceType.CompartmentDefinition,
				ResourceType.StructureMap,
				ResourceType.GraphDefinition,
				ResourceType.ExampleScenario
			)
		);

	public static final List<ResourceType> knowledgeArtifactResourceTypes =
		new ArrayList<>(
			List.of(
				ResourceType.Library,
				ResourceType.Measure,
				ResourceType.ActivityDefinition,
				ResourceType.PlanDefinition
			)
		);

	public static final List<ResourceType> terminologyResourceTypes =
		new ArrayList<>(
			List.of(
				ResourceType.CodeSystem,
				ResourceType.ValueSet,
				ResourceType.ConceptMap,
				ResourceType.NamingSystem,
				ResourceType.TerminologyCapabilities
			)
		);
	private BundleEntryComponent createEntry(IBaseResource theResource) {
		BundleEntryComponent entry = new Bundle.BundleEntryComponent()
				.setResource((Resource) theResource)
				.setRequest(createRequest(theResource));
		String fullUrl = entry.getRequest().getUrl();
		if (theResource instanceof MetadataResource) {
			MetadataResource resource = (MetadataResource) theResource;
			if (resource.hasUrl()) {
				fullUrl = resource.getUrl();
				if (resource.hasVersion()) {
					fullUrl += "|" + resource.getVersion();
				}
			}
		}
		entry.setFullUrl(fullUrl);
		return entry;
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
/**
 * search by versioned Canonical URL
 * @param url canonical URL of the form www.example.com/Patient/123|0.1
 * @param hapiFhirRepository to do the searching
 * @return a bundle of results
 */
	private Bundle searchResourceByUrl(String url, HapiFhirRepository hapiFhirRepository) {
		Map<String, List<IQueryParameterType>> searchParams = new HashMap<>();

		List<IQueryParameterType> urlList = new ArrayList<>();
		urlList.add(new UriParam(Canonicals.getUrl(url)));
		searchParams.put("url", urlList);

		List<IQueryParameterType> versionList = new ArrayList<>();
		String version = Canonicals.getVersion(url);
		if (version != null && !version.isEmpty()) {
			versionList.add(new TokenParam(Canonicals.getVersion((url))));
			searchParams.put("version", versionList);
		}

		Bundle searchResultsBundle = (Bundle)hapiFhirRepository.search(Bundle.class, ResourceClassMapHelper.getClass(Canonicals.getResourceType(url)), searchParams);
		return searchResultsBundle;
	}

	private Bundle searchArtifactAssessmentForArtifact(IdType reference, HapiFhirRepository hapiFhirRepository) {
		Map<String, List<IQueryParameterType>> searchParams = new HashMap<>();
		List<IQueryParameterType> urlList = new ArrayList<>();
		urlList.add(new ReferenceParam(reference));
		searchParams.put("artifact", urlList);
		Bundle searchResultsBundle = (Bundle)hapiFhirRepository.search(Bundle.class,Basic.class, searchParams);
		return searchResultsBundle;
	}

	private Bundle searchResourceByUrlAndStatus(String url, String status, HapiFhirRepository hapiFhirRepository) {
		Map<String, List<IQueryParameterType>> searchParams = new HashMap<>();

		List<IQueryParameterType> urlList = new ArrayList<>();
		urlList.add(new UriParam(Canonicals.getUrl(url)));
		searchParams.put("url", urlList);

		List<IQueryParameterType> versionList = new ArrayList<>();
		String version = Canonicals.getVersion(url);
		if (version != null && !version.isEmpty()) {
			versionList.add(new TokenParam(Canonicals.getVersion((url))));
			searchParams.put("version", versionList);
		}

		List<IQueryParameterType> statusList = new ArrayList<>();
		statusList.add(new TokenParam(status));
		searchParams.put("status", statusList);

		Bundle searchResultsBundle = hapiFhirRepository.search(Bundle.class, ResourceClassMapHelper.getClass(Canonicals.getResourceType(url)), searchParams);
		return searchResultsBundle;
	}

	private MetadataResource retrieveResourcesByCanonical(String reference, HapiFhirRepository hapiFhirRepository) throws ResourceNotFoundException {
		Bundle referencedResourceBundle = searchResourceByUrl(reference, hapiFhirRepository);
		Optional<MetadataResource> referencedResource = KnowledgeArtifactAdapter.findLatestVersion(referencedResourceBundle);
		if (referencedResource.isEmpty()) {
			throw new ResourceNotFoundException(String.format("Resource for Canonical '%s' not found.", reference));
		}
		return referencedResource.get();
	}

	/* approve */
	/*
	 * The operation sets the date and approvalDate elements of the approved artifact, and is otherwise only
	 * allowed to add artifactComment elements to the artifact and to add or update an endorser.
	 */
	public MetadataResource approve(MetadataResource resource, IPrimitiveType<Date> approvalDate, ArtifactAssessment assessment) {
		KnowledgeArtifactAdapter<MetadataResource> targetResourceAdapter = new KnowledgeArtifactAdapter<MetadataResource>(resource);
		Date currentDate = new Date();

		if (approvalDate == null){
			targetResourceAdapter.setApprovalDate(currentDate);
		} else {
			targetResourceAdapter.setApprovalDate(approvalDate.getValue());
		}

		DateTimeType theDate = new DateTimeType(currentDate, TemporalPrecisionEnum.DAY);
		resource.setDateElement(theDate);
		return resource;
	}

	ArtifactAssessment 	createApprovalAssessment(IdType id, String artifactCommentType,
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
	public Bundle createDraftBundle(IdType baseArtifactId, HapiFhirRepository hapiFhirRepository, String version) throws ResourceNotFoundException, UnprocessableEntityException {
		checkVersionValidSemver(version);
		MetadataResource baseArtifact = (MetadataResource)hapiFhirRepository.read(ResourceClassMapHelper.getClass(baseArtifactId.getResourceType()), baseArtifactId);
		if (baseArtifact == null) {
			throw new ResourceNotFoundException(baseArtifactId);
		}
		KnowledgeArtifactAdapter<MetadataResource> baseArtifactAdapter = new KnowledgeArtifactAdapter<MetadataResource>(baseArtifact);
		List<Extension> removeReleaseLabelAndDescription = baseArtifact.getExtension()
			.stream()
			.filter(ext -> !ext.getUrl().equals(releaseLabelUrl) && !ext.getUrl().equals(releaseDescriptionUrl))
			.collect(Collectors.toList());
		baseArtifact.setExtension(removeReleaseLabelAndDescription);
		baseArtifactAdapter.setApprovalDate(null);
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
		Bundle existingArtifactsForUrl = searchResourceByUrl(draftVersionUrl, hapiFhirRepository);
		if(existingArtifactsForUrl.getEntry().size() != 0){
			throw new PreconditionFailedException(
				String.format("A draft of Program '%s' already exists with version: '%s'. Only one draft of a program version can exist at a time.", baseArtifact.getUrl(), draftVersionUrl));
		}
		List<MetadataResource> resourcesToCreate = createDraftsOfArtifactAndRelated(baseArtifact, hapiFhirRepository, version, new ArrayList<MetadataResource>());
		Bundle transactionBundle = new Bundle()
			.setType(Bundle.BundleType.TRANSACTION);
		List<IdType> urnList = resourcesToCreate.stream().map(res -> new IdType("urn:uuid:" + UUID.randomUUID().toString())).collect(Collectors.toList());
		TreeSet<String> ownedResourceUrls = createOwnedResourceUrlCache(resourcesToCreate);
		for (int i = 0; i < resourcesToCreate.size(); i++) {
			KnowledgeArtifactAdapter<MetadataResource> newResourceAdapter = new KnowledgeArtifactAdapter<MetadataResource>(resourcesToCreate.get(i));
			updateUsageContextReferencesWithUrns(resourcesToCreate.get(i), resourcesToCreate, urnList);
			updateRelatedArtifactUrlsWithNewVersions(combineComponentsAndDependencies(newResourceAdapter), draftVersion, ownedResourceUrls);
			MetadataResource updateIdForBundle = newResourceAdapter.copy();
			updateIdForBundle.setId(urnList.get(i));
			transactionBundle.addEntry(createEntry(updateIdForBundle));
		}
		return transactionBundle;
	}
	private TreeSet<String> createOwnedResourceUrlCache(List<MetadataResource> resources) {
		TreeSet<String> retval = new TreeSet<String>();
		resources.stream()
			.map(KnowledgeArtifactAdapter::new)
			.map(KnowledgeArtifactAdapter::getOwnedRelatedArtifacts).flatMap(List::stream)
			.map(RelatedArtifact::getResource)
			.map(Canonicals::getUrl)
			.forEach(retval::add);
		return retval;
	}
	private void updateUsageContextReferencesWithUrns(MetadataResource newResource, List<MetadataResource> resourceListWithOriginalIds, List<IdType> idListForTransactionBundle) {
		List<UsageContext> useContexts = newResource.getUseContext();
		for (UsageContext useContext : useContexts) {
			// TODO: will we ever need to resolve these references?
			if (useContext.hasValueReference()) {
				Reference useContextRef = useContext.getValueReference();
				if (useContextRef != null) {
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

	private void updateRelatedArtifactUrlsWithNewVersions(List<RelatedArtifact> relatedArtifactList, String updatedVersion, TreeSet<String> ownedUrlCache){
		// For each  relatedArtifact, update the version of the reference.
		relatedArtifactList.stream()
			.filter(RelatedArtifact::hasResource)
			// only update the references to owned resources (including dependencies)
			.filter(ra -> ownedUrlCache.contains(Canonicals.getUrl(ra.getResource())))
			.collect(Collectors.toList())
			.replaceAll(ra -> ra.setResource(Canonicals.getUrl(ra.getResource()) + "|" + updatedVersion));
	}

	private void checkVersionValidSemver(String version) throws UnprocessableEntityException {
		if (version == null || version.isEmpty()) {
			throw new UnprocessableEntityException("The version argument is required");
		}
		if (version.contains("draft")) {
			throw new UnprocessableEntityException("The version cannot contain 'draft'");
		}
		if (version.contains("/") || version.contains("\\") || version.contains("|")) {
			throw new UnprocessableEntityException("The version contains illegal characters");
		}
		Pattern pattern = Pattern.compile("^(\\d+\\.)(\\d+\\.)(\\d+\\.)?(\\*|\\d+)$", Pattern.CASE_INSENSITIVE);
    	Matcher matcher = pattern.matcher(version);
    	boolean matchFound = matcher.find();
		if (!matchFound) {
			throw new UnprocessableEntityException("The version must be in the format MAJOR.MINOR.PATCH or MAJOR.MINOR.PATCH.REVISION");
		}
	}
	
	private List<MetadataResource> createDraftsOfArtifactAndRelated(MetadataResource resourceToDraft, HapiFhirRepository hapiFhirRepository, String version, List<MetadataResource> resourcesToCreate) {
		String draftVersion = version + "-draft";
		String draftVersionUrl = Canonicals.getUrl(resourceToDraft.getUrl()) + "|" + draftVersion;

		// TODO: Decide if we need both of these checks
		Optional<MetadataResource> existingArtifactsWithMatchingUrl = KnowledgeArtifactAdapter.findLatestVersion(searchResourceByUrl(draftVersionUrl, hapiFhirRepository));
		Optional<MetadataResource> draftVersionAlreadyInBundle = resourcesToCreate.stream().filter(res -> res.getUrl().equals(Canonicals.getUrl(draftVersionUrl)) && res.getVersion().equals(draftVersion)).findAny();
		MetadataResource newResource = null;
		if (existingArtifactsWithMatchingUrl.isPresent()) {
			newResource = existingArtifactsWithMatchingUrl.get();
		} else if(draftVersionAlreadyInBundle.isPresent()) {
			newResource = draftVersionAlreadyInBundle.get();
		}

		if (newResource == null) {
			KnowledgeArtifactAdapter<MetadataResource> sourceResourceAdapter = new KnowledgeArtifactAdapter<>(resourceToDraft);
			sourceResourceAdapter.setEffectivePeriod(null);
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
					Bundle referencedResourceBundle = searchResourceByUrl(ra.getUrl(), hapiFhirRepository);
					processReferencedResourceForDraft(hapiFhirRepository, referencedResourceBundle, ra, version, resourcesToCreate);
				} else if (ra.hasResource()) {
					Bundle referencedResourceBundle = searchResourceByUrl(ra.getResourceElement().getValueAsString(), hapiFhirRepository);
					processReferencedResourceForDraft(hapiFhirRepository, referencedResourceBundle, ra, version, resourcesToCreate);
				}
			}
		}

		return resourcesToCreate;
	}
	
	private void processReferencedResourceForDraft(HapiFhirRepository hapiFhirRepository, Bundle referencedResourceBundle, RelatedArtifact ra, String version, List<MetadataResource> transactionBundle) {
		if (!referencedResourceBundle.getEntryFirstRep().isEmpty()) {
			Bundle.BundleEntryComponent referencedResourceEntry = referencedResourceBundle.getEntry().get(0);
			if (referencedResourceEntry.hasResource() && referencedResourceEntry.getResource() instanceof MetadataResource) {
				MetadataResource referencedResource = (MetadataResource) referencedResourceEntry.getResource();

				createDraftsOfArtifactAndRelated(referencedResource, hapiFhirRepository, version, transactionBundle);
			}
		}
	}

	private Optional<String> getReleaseVersion(String version, CRMIReleaseVersionBehaviorCodes versionBehavior, String existingVersion) throws UnprocessableEntityException {
		Optional<String> releaseVersion = Optional.ofNullable(null);
		// If no version exists use the version argument provided
		if (existingVersion == null || existingVersion.isEmpty() || existingVersion.isBlank()) {
			return Optional.ofNullable(version);
		}
		String replaceDraftInExisting = existingVersion.replace("-draft","");

		if (CRMIReleaseVersionBehaviorCodes.DEFAULT == versionBehavior) {
			if(replaceDraftInExisting != null && !replaceDraftInExisting.isEmpty()) {
				releaseVersion = Optional.of(replaceDraftInExisting);
			} else {
				releaseVersion = Optional.ofNullable(version);
			}
		} else if (CRMIReleaseVersionBehaviorCodes.FORCE == versionBehavior) {
			releaseVersion = Optional.ofNullable(version);
		} else if (CRMIReleaseVersionBehaviorCodes.CHECK == versionBehavior) {
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
	public Bundle createReleaseBundle(IdType idType, String releaseLabel, String version, CRMIReleaseVersionBehaviorCodes versionBehavior, boolean latestFromTxServer, CRMIReleaseExperimentalBehaviorCodes experimentalBehavior, HapiFhirRepository hapiFhirRepository) throws UnprocessableEntityException, ResourceNotFoundException, PreconditionFailedException {
		// TODO: This check is to avoid partial releases and should be removed once the argument is supported.
		if (latestFromTxServer) {
			throw new NotImplementedOperationException("Support for 'latestFromTxServer' is not yet implemented.");
		}
		checkReleaseVersion(version, versionBehavior);
		MetadataResource rootArtifact = (MetadataResource)hapiFhirRepository.read(ResourceClassMapHelper.getClass(idType.getResourceType()),idType);
		KnowledgeArtifactAdapter<MetadataResource> rootArtifactAdapter = new KnowledgeArtifactAdapter<>(rootArtifact);
		Date currentApprovalDate = rootArtifactAdapter.getApprovalDate();
		checkReleasePreconditions(rootArtifact, currentApprovalDate);

		// Determine which version should be used.
		String existingVersion = rootArtifact.hasVersion() ? rootArtifact.getVersion().replace("-draft","") : null;
		String releaseVersion = getReleaseVersion(version, versionBehavior, existingVersion)
			.orElseThrow(() -> new UnprocessableEntityException("Could not resolve a version for the root artifact."));
		Period rootEffectivePeriod = rootArtifactAdapter.getEffectivePeriod();
		// if the root artifact is experimental then we don't need to check for experimental children
		if (rootArtifact.getExperimental()) {
			experimentalBehavior = CRMIReleaseExperimentalBehaviorCodes.NONE;
		}
		List<MetadataResource> releasedResources = internalRelease(rootArtifactAdapter, releaseVersion, rootEffectivePeriod, versionBehavior, latestFromTxServer, experimentalBehavior, hapiFhirRepository);
		updateReleaseLabel(rootArtifact, releaseLabel);
		List<RelatedArtifact> rootArtifactOriginalDependencies = new ArrayList<RelatedArtifact>(rootArtifactAdapter.getDependencies());
		// Get list of extensions which need to be preserved
		List<RelatedArtifact> originalDependenciesWithExtensions = rootArtifactOriginalDependencies.stream().filter(dep -> dep.getExtension() != null && dep.getExtension().size() > 0).collect(Collectors.toList());
		// once iteration is complete, delete all depends-on RAs in the root artifact
		rootArtifactAdapter.getRelatedArtifact().removeIf(ra -> ra.getType() == RelatedArtifact.RelatedArtifactType.DEPENDSON);

		Bundle transactionBundle = new Bundle()
			.setType(Bundle.BundleType.TRANSACTION);
		for (MetadataResource artifact: releasedResources) {
			transactionBundle.addEntry(createEntry(artifact));

			KnowledgeArtifactAdapter<MetadataResource> artifactAdapter = new KnowledgeArtifactAdapter<MetadataResource>(artifact);
			List<RelatedArtifact> components = artifactAdapter.getComponents();
			// add all root artifact components and child artifact components recursively as root artifact dependencies
			for (RelatedArtifact component : components) {
				MetadataResource resource;
				// if the relatedArtifact is Owned, need to update the reference to the new Version
				if (KnowledgeArtifactAdapter.checkIfRelatedArtifactIsOwned(component)) {
					resource = checkIfReferenceInList(component, releasedResources)
					// should never happen since we check all references as part of `internalRelease`
					.orElseThrow(() -> new InternalErrorException("Owned resource reference not found during release"));
					String reference = String.format("%s|%s", resource.getUrl(), resource.getVersion());
					component.setResource(reference);
				} else if (Canonicals.getVersion(component.getResourceElement()) == null || Canonicals.getVersion(component.getResourceElement()).isEmpty()) {
					// if the not Owned component doesn't have a version, try to find the latest version
					String updatedReference = tryUpdateReferenceToLatestActiveVersion(component.getResource(), hapiFhirRepository, artifact.getUrl());
					component.setResource(updatedReference);
				}
				RelatedArtifact componentToDependency = new RelatedArtifact().setType(RelatedArtifact.RelatedArtifactType.DEPENDSON).setResource(component.getResourceElement().getValueAsString());
				rootArtifactAdapter.getRelatedArtifact().add(componentToDependency);
			}

			List<RelatedArtifact> dependencies = artifactAdapter.getDependencies();
			for (RelatedArtifact dependency : dependencies) {
				// if the dependency gets updated as part of $release then update the reference as well
				checkIfReferenceInList(dependency, releasedResources)
					.ifPresentOrElse((resource) -> {
						String updatedReference = String.format("%s|%s", resource.getUrl(), resource.getVersion());
						dependency.setResource(updatedReference);
					},
					// not present implies that the dependency wasn't updated as part of $release
					() -> {
						// if the dependency doesn't have a version, try to find the latest version
						if (Canonicals.getVersion(dependency.getResourceElement()) == null || Canonicals.getVersion(dependency.getResourceElement()).isEmpty()) {
							// TODO: update when we support expansionParameters and requireVersionedDependencies
							String updatedReference = tryUpdateReferenceToLatestActiveVersion(dependency.getResource(), hapiFhirRepository, artifact.getUrl());
							dependency.setResource(updatedReference);
						}
					});
				// only add the dependency to the manifest if it is from a leaf artifact
				if (!artifact.getUrl().equals(rootArtifact.getUrl())) {
					rootArtifactAdapter.getRelatedArtifact().add(dependency);
				}
			}
		}
		// removed duplicates and add
		List<RelatedArtifact> distinctResolvedRelatedArtifacts = new ArrayList<>();
		for (RelatedArtifact resolvedRelatedArtifact: rootArtifactAdapter.getRelatedArtifact()) {
			boolean isDistinct = !distinctResolvedRelatedArtifacts.stream().anyMatch(distinctRelatedArtifact -> {
				boolean referenceNotInArray = distinctRelatedArtifact.getResource().equals(resolvedRelatedArtifact.getResource());
				boolean typeMatches = distinctRelatedArtifact.getType().equals(resolvedRelatedArtifact.getType());
				return referenceNotInArray && typeMatches;
			});
			if (isDistinct) {
				distinctResolvedRelatedArtifacts.add(resolvedRelatedArtifact);
				// preserve Extensions if found
				originalDependenciesWithExtensions
				.stream()
					.filter(originalDep -> originalDep.getResource().equals(resolvedRelatedArtifact.getResource()))
					.findFirst()
					.ifPresent(dep -> {
						checkIfValueSetNeedsCondition(null, dep, hapiFhirRepository);
						resolvedRelatedArtifact.getExtension().addAll(dep.getExtension());
						originalDependenciesWithExtensions.removeIf(ra -> ra.getResource().equals(resolvedRelatedArtifact.getResource()));
					});
			}
		}
		// update ArtifactComments referencing the old Canonical Reference
		transactionBundle.getEntry().addAll(findArtifactCommentsToUpdate(rootArtifact, releaseVersion, hapiFhirRepository));
		rootArtifactAdapter.setRelatedArtifact(distinctResolvedRelatedArtifacts);

		return transactionBundle;
	}
	private List<RelatedArtifact> getRelatedArtifactsWithPreservedExtensions(List<RelatedArtifact> deps) {
		return deps.stream()
			.filter(ra -> preservedExtensionUrls
				.stream().anyMatch(url -> ra.getExtension()
					.stream().anyMatch(ext -> ext.getUrl().equalsIgnoreCase(url))))
			.collect(Collectors.toList());
	}
	private List<BundleEntryComponent> findArtifactCommentsToUpdate(MetadataResource rootArtifact,String releaseVersion, HapiFhirRepository hapiFhirRepository){
		List<BundleEntryComponent> returnEntries = new ArrayList<BundleEntryComponent>();
		// find any artifact assessments and update those as part of the bundle
		this.searchArtifactAssessmentForArtifact(rootArtifact.getIdElement(), hapiFhirRepository)
			.getEntry()
			.stream()
			// The search is on Basic resources only unless we can register the ArtifactAssessment class
			.map(entry -> {
				try {
					return (Basic) entry.getResource();
				} catch (Exception e) {
					return null;
				}
			})
			.filter(entry -> entry != null)
			// convert Basic to ArtifactAssessment by transferring the extensions
			.map(basic -> {
				ArtifactAssessment extensionsTransferred = new ArtifactAssessment();
				extensionsTransferred.setExtension(basic.getExtension());
				extensionsTransferred.setId(basic.getClass().getSimpleName() + "/" + basic.getIdPart());
				return extensionsTransferred;
			})
			.forEach(artifactComment -> {
				artifactComment.setDerivedFromContentRelatedArtifact(new CanonicalType(String.format("%s|%s", rootArtifact.getUrl(), releaseVersion)));
				returnEntries.add(createEntry(artifactComment));
			});
			return returnEntries;
	}
	private void updateReleaseLabel(MetadataResource artifact,String releaseLabel) throws IllegalArgumentException {
		if (releaseLabel != null) {
			Extension releaseLabelExtension = artifact.getExtensionByUrl(releaseLabel);
			if (releaseLabelExtension == null) {
				// create the Extension and add it to the artifact if it doesn't exist
				releaseLabelExtension = new Extension(releaseLabelUrl);
				artifact.addExtension(releaseLabelExtension);
			}
			releaseLabelExtension.setValue(new StringType(releaseLabel));
		}
	}
	private void checkReleaseVersion(String version,CRMIReleaseVersionBehaviorCodes versionBehavior) throws UnprocessableEntityException {
		if (CRMIReleaseVersionBehaviorCodes.NULL == versionBehavior) {
			throw new UnprocessableEntityException("'versionBehavior' must be provided as an argument to the $release operation. Valid values are 'default', 'check', 'force'.");
		}
		checkVersionValidSemver(version);
	}
	private void checkReleasePreconditions(MetadataResource artifact, Date approvalDate) throws PreconditionFailedException {
		if (artifact == null) {
			throw new ResourceNotFoundException("Resource not found.");
		}

		if (Enumerations.PublicationStatus.DRAFT != artifact.getStatus()) {
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
																 CRMIReleaseVersionBehaviorCodes versionBehavior, boolean latestFromTxServer, CRMIReleaseExperimentalBehaviorCodes experimentalBehavior, HapiFhirRepository hapiFhirRepository) throws NotImplementedOperationException, ResourceNotFoundException {
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
					referencedResource = KnowledgeArtifactAdapter.findLatestVersion(searchResourceByUrl(ownedResourceReference.getValueAsString(), hapiFhirRepository))
					.orElseThrow(()-> new ResourceNotFoundException(
							String.format("Resource with URL '%s' is Owned by this repository and referenced by resource '%s', but was not found on the server.",
								ownedResourceReference.getValueAsString(),
								artifactAdapter.resource.getUrl()))
					);
					KnowledgeArtifactAdapter<MetadataResource> searchResultAdapter = new KnowledgeArtifactAdapter<>(referencedResource);
					if (CRMIReleaseExperimentalBehaviorCodes.NULL != experimentalBehavior && CRMIReleaseExperimentalBehaviorCodes.NONE != experimentalBehavior) {
						checkNonExperimental(referencedResource, experimentalBehavior, hapiFhirRepository);
					}
					resourcesToUpdate.addAll(internalRelease(searchResultAdapter, version, rootEffectivePeriod, versionBehavior, latestFromTxServer, experimentalBehavior, hapiFhirRepository));
				}
			}
		}

		return resourcesToUpdate;
	}
	private String tryUpdateReferenceToLatestActiveVersion(String inputReference, HapiFhirRepository hapiFhirRepository, String sourceArtifactUrl) throws ResourceNotFoundException {
		// List<MetadataResource> matchingResources = getResourcesFromBundle(searchResourceByUrlAndStatus(inputReference, "active", hapiFhirRepository));
		// using filtered list until APHL-601 (searchResourceByUrlAndStatus bug) resolved
		List<MetadataResource> matchingResources = getResourcesFromBundle(searchResourceByUrl(inputReference, hapiFhirRepository))
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
		for (MetadataResource resource : resourceList) {
			String referenceURL = Canonicals.getUrl(artifactToUpdate.getResourceElement());
			String currentResourceURL = resource.getUrl();
			if (artifactToUpdate.hasResource() && referenceURL.equals(currentResourceURL)) {
				return Optional.of(resource);
			}
		}
		return updatedReference;
	}

	private void checkNonExperimental(MetadataResource resource, CRMIReleaseExperimentalBehaviorCodes experimentalBehavior, HapiFhirRepository hapiFhirRepository) throws UnprocessableEntityException {
		String nonExperimentalError = String.format("Root artifact is not Experimental, but references an Experimental resource with URL '%s'.",
								resource.getUrl());
		if (CRMIReleaseExperimentalBehaviorCodes.WARN == experimentalBehavior && resource.getExperimental()) {
			myLog.warn(nonExperimentalError);
		} else if (CRMIReleaseExperimentalBehaviorCodes.ERROR == experimentalBehavior && resource.getExperimental()) {
			throw new UnprocessableEntityException(nonExperimentalError);
		}
		// for ValueSets need to check recursively if any chldren are experimental since we don't own these
		if (resource.getResourceType().equals(ResourceType.ValueSet)) {
			ValueSet valueSet = (ValueSet) resource;
			List<CanonicalType> valueSets = valueSet
				.getCompose()
				.getInclude()
				.stream().flatMap(include -> include.getValueSet().stream())
				.collect(Collectors.toList());
			for (CanonicalType value: valueSets) {
				KnowledgeArtifactAdapter.findLatestVersion(searchResourceByUrl(value.getValueAsString(), hapiFhirRepository))
				.ifPresent(childVs -> checkNonExperimental(childVs, experimentalBehavior, hapiFhirRepository));
			}
		}
	}

	/* $package */
	public Bundle createPackageBundle(IdType id, HapiFhirRepository hapiFhirRepository, List<String> capability,
												 List<String> include, List<CanonicalType> artifactVersion,
												 List<CanonicalType> checkArtifactVersion, List<CanonicalType> forceArtifactVersion,
												 Integer count, Integer offset, String artifactRoute, String endpointUri,
												 Endpoint contentEndpoint, Endpoint terminologyEndpoint, Boolean packageOnly)
			throws NotImplementedOperationException, UnprocessableEntityException, IllegalArgumentException {
		if (
				(artifactRoute != null && !artifactRoute.isBlank() && !artifactRoute.isEmpty())
					|| (endpointUri != null && !endpointUri.isBlank() && !endpointUri.isEmpty())
					|| contentEndpoint != null
					|| terminologyEndpoint != null
			) {
			throw new NotImplementedOperationException("This repository is not implementing custom Content and Terminology endpoints at this time");
		}
		if (packageOnly != null) {
			throw new NotImplementedOperationException("This repository is not implementing packageOnly at this time");
		}
		if (count != null && count < 0) {
			throw new UnprocessableEntityException("'count' must be non-negative");
		}
		MetadataResource resource = (MetadataResource)hapiFhirRepository.read(ResourceClassMapHelper.getClass(id.getResourceType()), id);
		// TODO: In the case of a released (active) root Library we can depend on the relatedArtifacts as a comprehensive manifest
		Bundle packagedBundle = new Bundle();
		if (include != null
			&& include.size() == 1
			&& include.stream().anyMatch((includedType) -> includedType.equals("artifact"))) {
			findUnsupportedCapability(resource, capability);
			processCanonicals(resource, artifactVersion, checkArtifactVersion, forceArtifactVersion);
			BundleEntryComponent entry = createEntry(resource);
			entry.getRequest().setUrl(resource.getResourceType() + "/" + resource.getIdElement().getIdPart());
			entry.getRequest().setMethod(HTTPVerb.POST);
			entry.getRequest().setIfNoneExist("url="+resource.getUrl()+"&version="+resource.getVersion());
			packagedBundle.addEntry(entry);
		} else {
			recursivePackage(resource, packagedBundle, hapiFhirRepository, capability, include, artifactVersion, checkArtifactVersion, forceArtifactVersion);
			List<BundleEntryComponent> included = findUnsupportedInclude(packagedBundle.getEntry(),include);
			packagedBundle.setEntry(included);
		}
		setCorrectBundleType(count,offset,packagedBundle);
		pageBundleBasedOnCountAndOffset(count, offset, packagedBundle);
		handleValueSetReferenceExtensions(resource, packagedBundle.getEntry(), hapiFhirRepository);
		return packagedBundle;
	}
/**
 * $package allows for a bundle to be paged
 * @param count the maximum number of resources to be returned
 * @param offset the number of resources to skip beginning from the start of the bundle (starts from 1)
 * @param bundle the bundle to page
 */
	private void pageBundleBasedOnCountAndOffset(Integer count, Integer offset, Bundle bundle) {
		if (offset != null) {
			List<BundleEntryComponent> entries = bundle.getEntry();
			Integer bundleSize = entries.size();
			if (offset < bundleSize) {
				bundle.setEntry(entries.subList(offset, bundleSize));
			} else {
				bundle.setEntry(Arrays.asList());
			}
		}
		if (count != null) {
			// repeat these two from earlier because we might modify / replace the entries list at any time
			List<BundleEntryComponent> entries = bundle.getEntry();
			Integer bundleSize = entries.size();
			if (count < bundleSize){
				bundle.setEntry(entries.subList(0, count));
			} else {
				// there are not enough entries in the bundle to page, so we return all of them no change
			}
		}
	}
	
	private void setCorrectBundleType(Integer count, Integer offset, Bundle bundle) {
		// if the bundle is paged then it must be of type = collection and modified to follow bundle.type constraints
		// if not, set type = transaction
		// special case of count = 0 -> set type = searchset so we can display bundle.total
		if (count != null && count == 0) {
			bundle.setType(BundleType.SEARCHSET);
			bundle.setTotal(bundle.getEntry().size());
		} else if (
			(offset != null && offset > 0) || 
			(count != null && count < bundle.getEntry().size())
		) {
			bundle.setType(BundleType.COLLECTION);
			List<BundleEntryComponent> removedRequest = bundle.getEntry().stream()
				.map(entry -> {
					entry.setRequest(null);
					return entry;
				}).collect(Collectors.toList());
			bundle.setEntry(removedRequest);
		} else {
			bundle.setType(BundleType.TRANSACTION);
		}
	}
	private void checkIfValueSetNeedsCondition(MetadataResource resource, RelatedArtifact relatedArtifact, HapiFhirRepository hapiFhirRepository) throws UnprocessableEntityException {
		if (resource == null 
		&& relatedArtifact != null 
		&& relatedArtifact.hasResource() 
		&& Canonicals.getResourceType(relatedArtifact.getResource()).equals("ValueSet")) {
			List<MetadataResource> searchResults = getResourcesFromBundle(searchResourceByUrl(relatedArtifact.getResource(), hapiFhirRepository));
			if (searchResults.size() > 0) {
				resource = searchResults.get(0);
			}
		}
		if (resource != null && resource.getResourceType() == ResourceType.ValueSet) {
			ValueSet valueSet = (ValueSet)resource;
			boolean isLeaf = !valueSet.hasCompose() || (valueSet.hasCompose() && valueSet.getCompose().getIncludeFirstRep().getValueSet().size() == 0);
			Optional<Extension> maybeConditionExtension = Optional.ofNullable(relatedArtifact)
				.map(RelatedArtifact::getExtension)
				.map(list -> {
					return list.stream().filter(ext -> ext.getUrl().equalsIgnoreCase(valueSetConditionUrl)).findFirst().orElse(null);
				});
			if (isLeaf && !maybeConditionExtension.isPresent()) {
				throw new UnprocessableEntityException("Missing condition on ValueSet : " + valueSet.getUrl());
			}
		}
	}
	/**
	 * ValueSets can be part of multiple artifacts at the same time. Certain properties are tracked/managed in the manifest to avoid conflicts with other artifacts. This function sets those properties on the ValueSets themselves at export / $package time
	 * @param manifest the resource containing all RelatedArtifact references
	 * @param bundleEntries the list of packaged resources to modify according to the extensions on the manifest relatedArtifact references
	 */
	private void handleValueSetReferenceExtensions(MetadataResource manifest, List<BundleEntryComponent> bundleEntries, HapiFhirRepository hapiFhirRepository) throws UnprocessableEntityException, IllegalArgumentException {
		KnowledgeArtifactAdapter<MetadataResource> adapter = new KnowledgeArtifactAdapter<MetadataResource>(manifest);
		List<RelatedArtifact> relatedArtifactsWithPreservedExtension = getRelatedArtifactsWithPreservedExtensions(adapter.getDependencies());
		bundleEntries.stream()
			.forEach(entry -> {
				if (entry.getResource().getResourceType().equals(ResourceType.ValueSet)) {
					ValueSet valueSet = (ValueSet) entry.getResource();
					// remove any existing Priority and Conditions
					List<UsageContext> usageContexts = removeExistingReferenceExtensionData(valueSet.getUseContext());
					valueSet.setUseContext(usageContexts);
					Optional<RelatedArtifact> maybeVSRelatedArtifact = relatedArtifactsWithPreservedExtension.stream().filter(ra -> Canonicals.getUrl(ra.getResource()).equals(valueSet.getUrl())).findFirst();
					checkIfValueSetNeedsCondition(valueSet, maybeVSRelatedArtifact.orElse(null), hapiFhirRepository);
					// If leaf valueset
					if (!valueSet.hasCompose()
					 || (valueSet.hasCompose() && valueSet.getCompose().getIncludeFirstRep().getValueSet().size() == 0)) {
						// If Condition extension is present
						maybeVSRelatedArtifact
							.map(ra -> ra.getExtension())
							.ifPresent(
								// add Conditions
								exts -> {
									exts.stream()
										.filter(ext -> ext.getUrl().equalsIgnoreCase(valueSetConditionUrl))
										.forEach(ext -> tryAddCondition(usageContexts, (CodeableConcept) ext.getValue()));
								});		
					}
					// update Priority
					UsageContext priority = getOrCreateUsageContext(usageContexts, usPhContextTypeUrl, valueSetPriorityCode);
					maybeVSRelatedArtifact
						.map(ra -> ra.getExtensionByUrl(valueSetPriorityUrl))
						.ifPresentOrElse(
							// set value as per extension
							ext -> priority.setValue(ext.getValue()),
							// set to "routine" if missing
							() -> {
								CodeableConcept routine = new CodeableConcept(new Coding(contextUrl, "routine", null)).setText("Routine");
								priority.setValue(routine);
						});
				}
			});
	}
	/**
	 * Removes any existing UsageContexts corresponding the the VSM specific extensions
	 * @param usageContexts the list of usage contexts to modify
	 */
	private List<UsageContext> removeExistingReferenceExtensionData(List<UsageContext> usageContexts) {
		List<String> useContextCodesToReplace = List.of(valueSetConditionCode,valueSetPriorityCode);
		return usageContexts.stream()
		// remove any useContexts which need to be replaced
			.filter(useContext -> !useContextCodesToReplace.stream()
				.anyMatch(code -> useContext.getCode().getCode().equals(code)))
			.collect(Collectors.toList());
	}

	private void tryAddCondition(List<UsageContext> usageContexts, CodeableConcept condition) {
		boolean focusAlreadyExists = usageContexts.stream().anyMatch(u -> 
			u.getCode().getSystem().equals(contextTypeUrl) 
			&& u.getCode().getCode().equals(valueSetConditionCode) 
			&& u.getValueCodeableConcept().hasCoding(condition.getCoding().get(0).getSystem(), condition.getCoding().get(0).getCode())
		);
		if (!focusAlreadyExists) {
			UsageContext newFocus = new UsageContext(new Coding(contextTypeUrl,valueSetConditionCode,null),condition);
			newFocus.setValue(condition);
			usageContexts.add(newFocus);
		}
	}
	/**
	 * 
	 * Either finds a usageContext with the same system and code or creates an empty one
	 * and appends it
	 * 
	 * @param usageContexts the list of usageContexts to search and/or append to
	 * @param system the usageContext.code.system to find / create
	 * @param code the usage.code.code to find / create
	 * @return the found / created usageContext
	 */
	private UsageContext getOrCreateUsageContext(List<UsageContext> usageContexts, String system, String code) {
		return usageContexts.stream()
			.filter(useContext -> useContext.getCode().getSystem().equals(system) && useContext.getCode().getCode().equals(code))
			.findFirst().orElseGet(()-> {
				// create the UseContext if it doesn't exist
				Coding c = new Coding(system, code, null);
				UsageContext n = new UsageContext(c, null);
				// add it to the ValueSet before returning
				usageContexts.add(n);
				return n;
			});
	}
	void recursivePackage(
		MetadataResource resource,
		Bundle bundle,
		HapiFhirRepository hapiFhirRepository,
		List<String> capability,
		List<String> include,
		List<CanonicalType> artifactVersion,
		List<CanonicalType> checkArtifactVersion,
		List<CanonicalType> forceArtifactVersion
		) throws PreconditionFailedException{
		if (resource != null) {
			KnowledgeArtifactAdapter<MetadataResource> adapter = new KnowledgeArtifactAdapter<MetadataResource>(resource);
			findUnsupportedCapability(resource, capability);
			processCanonicals(resource, artifactVersion, checkArtifactVersion, forceArtifactVersion);
			boolean entryExists = bundle.getEntry().stream()
				.map(e -> (MetadataResource)e.getResource())
				.filter(mr -> mr.getUrl() != null && mr.getVersion() != null)
				.anyMatch(mr -> mr.getUrl().equals(resource.getUrl()) && mr.getVersion().equals(resource.getVersion()));
			if (!entryExists) {
				BundleEntryComponent entry = createEntry(resource);
				entry.getRequest().setUrl(resource.getResourceType() + "/" + resource.getIdElement().getIdPart());
				entry.getRequest().setMethod(HTTPVerb.POST);
				entry.getRequest().setIfNoneExist("url="+resource.getUrl()+"&version="+resource.getVersion());
				bundle.addEntry(entry);
			}

			combineComponentsAndDependencies(adapter).stream()
				.map(ra -> searchResourceByUrl(ra.getResource(), hapiFhirRepository))
				.map(searchBundle -> searchBundle.getEntry().stream().findFirst().orElseGet(()-> new BundleEntryComponent()).getResource())
				.forEach(component -> recursivePackage((MetadataResource)component, bundle, hapiFhirRepository, capability, include, artifactVersion, checkArtifactVersion, forceArtifactVersion));
		}
	}
	private List<RelatedArtifact> combineComponentsAndDependencies(KnowledgeArtifactAdapter<MetadataResource> adapter) {
		return Stream.concat(adapter.getComponents().stream(), adapter.getDependencies().stream()).collect(Collectors.toList());
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
				// consider resource unsupported if it's knowledgeCapability is undefined
				throw new PreconditionFailedException(String.format("Resource with url: '%s' does not specify capability.", resource.getUrl()));
			}
			knowledgeCapabilityExtension.stream()
				.filter(ext -> !capability.contains(((CodeType) ext.getValue()).getValue()))
				.findAny()
				.ifPresent((ext) -> {
					throw new PreconditionFailedException(String.format("Resource with url: '%s' is not one of '%s'.",
					resource.getUrl(),
					String.join(", ", capability)));
				});
		}
	}

	private void processCanonicals(MetadataResource resource, List<CanonicalType> canonicalVersion,  List<CanonicalType> checkArtifactVersion,  List<CanonicalType> forceArtifactVersion) throws PreconditionFailedException {
		if (checkArtifactVersion != null) {
			// check throws an error
			findVersionInListMatchingResource(checkArtifactVersion, resource)
				.ifPresent((version) -> {
					if (!resource.getVersion().equals(version)) {
						throw new PreconditionFailedException(String.format("Resource with url '%s' has version '%s' but checkVersion specifies '%s'",
						resource.getUrl(),
						resource.getVersion(),
						version
						));
					}
				});
		} else if (forceArtifactVersion != null) {
			// force just does a silent override
			findVersionInListMatchingResource(forceArtifactVersion, resource)
				.ifPresent((version) -> resource.setVersion(version));
		} else if (canonicalVersion != null && !resource.hasVersion()) {
			// canonicalVersion adds a version if it's missing
			findVersionInListMatchingResource(canonicalVersion, resource)
				.ifPresent((version) -> resource.setVersion(version));
		}
	}

	private List<BundleEntryComponent> findUnsupportedInclude(List<BundleEntryComponent> entries, List<String> include) {
		if (include == null || include.stream().anyMatch((includedType) -> includedType.equals("all"))) {
			return entries;
		}
		List<BundleEntryComponent> filteredList = new ArrayList<>();
		entries.stream().forEach(entry -> {
			if (include.stream().anyMatch((type) -> type.equals("knowledge"))) {
				Boolean resourceIsKnowledgeType = knowledgeArtifactResourceTypes.contains(entry.getResource().getResourceType());
				if (resourceIsKnowledgeType) {
					filteredList.add(entry);
				}
			}
			if (include.stream().anyMatch((type) -> type.equals("canonical"))) {
				Boolean resourceIsCanonicalType = canonicalResourceTypes.contains(entry.getResource().getResourceType());
				if (resourceIsCanonicalType) {
					filteredList.add(entry);
				}
			}
			if (include.stream().anyMatch((type) -> type.equals("terminology"))) {
				Boolean resourceIsTerminologyType = terminologyResourceTypes.contains(entry.getResource().getResourceType());
				if (resourceIsTerminologyType) {
					filteredList.add(entry);
				}
			}
			if (include.stream().anyMatch((type) -> type.equals("conformance"))) {
				Boolean resourceIsConformanceType = conformanceResourceTypes.contains(entry.getResource().getResourceType());
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
	public Parameters artifactDiff(MetadataResource theSourceLibrary, MetadataResource theTargetLibrary, FhirContext theContext, HapiFhirRepository hapiFhirRepository, boolean compareComputable, boolean compareExecutable,IFhirResourceDaoValueSet<ValueSet> dao) throws UnprocessableEntityException {
		// setup
		FhirPatch patch = new FhirPatch(theContext);
		patch.setIncludePreviousValueInDiff(true);
		// ignore meta changes
		patch.addIgnorePath("*.meta");
		Parameters libraryDiff = handleRelatedArtifactArrayElementsDiff(theSourceLibrary,theTargetLibrary,patch);

		// then check for references and add those to the base Parameters object
		diffCache cache = new diffCache();
		cache.addDiff(theSourceLibrary.getUrl()+"|"+theSourceLibrary.getVersion(), theTargetLibrary.getUrl()+"|"+theTargetLibrary.getVersion(), libraryDiff);
		checkForChangesInChildren(libraryDiff, theSourceLibrary, theTargetLibrary, hapiFhirRepository, patch, cache, theContext, compareComputable, compareExecutable,dao);
		return libraryDiff;
	}
	private Parameters handleRelatedArtifactArrayElementsDiff(MetadataResource theSourceLibrary,MetadataResource theTargetLibrary, FhirPatch patch) {
		KnowledgeArtifactAdapter<MetadataResource> updateSource = new KnowledgeArtifactAdapter<MetadataResource>(theSourceLibrary.copy());
		KnowledgeArtifactAdapter<MetadataResource> updateTarget = new KnowledgeArtifactAdapter<MetadataResource>(theTargetLibrary.copy());
		additionsAndDeletions<RelatedArtifact> processedRelatedArtifacts = extractAdditionsAndDeletions(updateSource.getRelatedArtifact(), updateTarget.getRelatedArtifact(), RelatedArtifact.class);
		updateSource.setRelatedArtifact(processedRelatedArtifacts.getSourceMatches());
		updateTarget.setRelatedArtifact(processedRelatedArtifacts.getTargetMatches());
		Parameters updateOperations = (Parameters) patch.diff(updateSource.copy(),updateTarget.copy());
		processedRelatedArtifacts.appendInsertOperations(updateOperations, patch, updateSource.getRelatedArtifact().size());
		processedRelatedArtifacts.appendDeleteOperations(updateOperations, patch, updateSource.getRelatedArtifact().size());
		return updateOperations;
	}
	private Parameters advancedValueSetDiff(MetadataResource theSourceValueSet,MetadataResource theTargetValueSet, FhirPatch patch, boolean compareComputable, boolean compareExecutable) {
		ValueSet updateSource = (ValueSet)theSourceValueSet.copy();
		ValueSet updateTarget = (ValueSet)theTargetValueSet.copy();
		additionsAndDeletions<ConceptSetComponent> composeIncludeProcessed = extractAdditionsAndDeletions(updateSource.getCompose().getInclude(), updateTarget.getCompose().getInclude(), ConceptSetComponent.class);
		additionsAndDeletions<ValueSetExpansionContainsComponent> expansionContainsProcessed = extractAdditionsAndDeletions(updateSource.getExpansion().getContains(), updateTarget.getExpansion().getContains(), ValueSetExpansionContainsComponent.class);
		if (compareComputable) {
			updateSource.getCompose().setInclude(composeIncludeProcessed.getSourceMatches());
			updateTarget.getCompose().setInclude(composeIncludeProcessed.getTargetMatches());
		} else {
			// don't generate any Parameters
			updateSource.getCompose().setInclude(new ArrayList<>());
			updateTarget.getCompose().setInclude(new ArrayList<>());
		}
		if (compareExecutable) {
			updateSource.getExpansion().setContains(expansionContainsProcessed.getSourceMatches());
			updateTarget.getExpansion().setContains(expansionContainsProcessed.getTargetMatches());
		} else {
			// don't generate any Parameters
			updateSource.getExpansion().setContains(new ArrayList<>());
			updateTarget.getExpansion().setContains(new ArrayList<>());
		}
		// first match the ones which are just updated
		Parameters vsDiff = (Parameters) patch.diff(updateSource,updateTarget);
		// then get all the delete entries
		if (compareComputable) {
			composeIncludeProcessed.appendInsertOperations(vsDiff, patch, updateTarget.getCompose().getInclude().size());
			composeIncludeProcessed.appendDeleteOperations(vsDiff, patch, updateTarget.getCompose().getInclude().size());
		}
		if (compareExecutable) {
			expansionContainsProcessed.appendInsertOperations(vsDiff, patch, updateTarget.getExpansion().getContains().size());	
			expansionContainsProcessed.appendDeleteOperations(vsDiff, patch, updateTarget.getExpansion().getContains().size());	
		}
		return vsDiff;
	}
	private void doesValueSetNeedExpansion(ValueSet vset, IFhirResourceDaoValueSet<ValueSet> dao) {
		Optional<Date> lastExpanded = Optional.ofNullable(vset.getExpansion()).map(e -> e.getTimestamp());
		Optional<Date> lastUpdated = Optional.ofNullable(vset.getMeta()).map(m -> m.getLastUpdated());
		if (lastExpanded.isPresent() && lastUpdated.isPresent() && lastExpanded.get().equals(lastUpdated.get())) {
			// ValueSet was not changed after last expansion, don't need to update
			return;
		} else {
			// clear obsolete expansion
			vset.setExpansion(null);
			ValueSetExpansionOptions options = new ValueSetExpansionOptions();
			options.setIncludeHierarchy(true);

			ValueSet e = dao.expand(vset,options);
			// we need to do this because dao.expand sets the expansion to a subclass and then that breaks the FhirPatch
			// `copy` creates the superclass again
			vset.setExpansion(e.getExpansion().copy());
			return;
		}
	}

	public void getExpansion(ValueSet valueSet) {
		Extension authoritativeSource = valueSet
			.getExtensionByUrl("http://hl7.org/fhir/StructureDefinition/valueset-authoritativeSource");
		String urlToUse = authoritativeSource != null && authoritativeSource.hasValue() ? authoritativeSource.getValue().primitiveValue() : valueSet.getUrl();

		ValueSet.ValueSetExpansionComponent expansion = new ValueSet.ValueSetExpansionComponent();
		expansion.setTimestamp(Date.from(Instant.now()));

		ValueSet expandedValueSet;
		try {
			expandedValueSet = terminologyServerClient.expand(valueSet, urlToUse);
			valueSet.setExpansion(expandedValueSet.getExpansion());
		} catch (Exception e) {
			myLog.warn("Terminology Server expansion failed: {}", valueSet.getIdElement().getValue(), e);

			if (hasSimpleCompose(valueSet)) {
				//Expansion run independent of terminology servers and should be flagged as such
				ArrayList<ValueSet.ValueSetExpansionParameterComponent> expansionParameters = new ArrayList<>();
				ValueSet.ValueSetExpansionParameterComponent parameterNaive = new ValueSet.ValueSetExpansionParameterComponent();
				parameterNaive.setName("naive");
				parameterNaive.setValue(new BooleanType(true));
				expansionParameters.add(parameterNaive);
				expansion.setParameter(expansionParameters);

				for (ValueSet.ConceptSetComponent csc : valueSet.getCompose().getInclude()) {
					for (ValueSet.ConceptReferenceComponent crc : csc.getConcept()) {
						expansion.addContains()
							.setCode(crc.getCode())
							.setSystem(csc.getSystem())
							.setVersion(csc.getVersion())
							.setDisplay(crc.getDisplay());
					}
				}
				valueSet.setExpansion(expansion);
			}
		}
	}

	public boolean hasSimpleCompose(ValueSet valueSet) {
		if (valueSet.hasCompose()) {
			if (valueSet.getCompose().hasExclude()) {
				return false;
			}
			for (ValueSet.ConceptSetComponent csc : valueSet.getCompose().getInclude()) {
				if (csc.hasValueSet()) {
					// Cannot expand a compose that references a value set
					return false;
				}

				if (!csc.hasSystem()) {
					// Cannot expand a compose that does not have a system
					return false;
				}

				if (csc.hasFilter()) {
					// Cannot expand a compose that has a filter
					return false;
				}

				if (!csc.hasConcept()) {
					// Cannot expand a compose that does not enumerate concepts
					return false;
				}
			}

			// If all includes are simple, the compose can be expanded
			return true;
		}

		return false;
	}

	private class diffCache {
		private final Map<String,Parameters> diffs = new HashMap<String,Parameters>();
		private final Map<String,MetadataResource> resources = new HashMap<String,MetadataResource>();
		public diffCache() {
			super();
		}
		public void addDiff(String sourceUrl, String targetUrl, Parameters diff) {
			this.diffs.put(sourceUrl+"-"+targetUrl, diff);
		}
		public Parameters getDiff(String sourceUrl, String targetUrl) {
			return this.diffs.get(sourceUrl+"-"+targetUrl);
		}
		public void addResource(String url, MetadataResource resource) {
			this.resources.put(url, resource);
		}
		public MetadataResource getResource(String url) {
			return this.resources.get(url);
		}
	}
	private void fixDeletePathIndexes(List<ParametersParameterComponent> parameters, int newStart) {
		for (int i = 0; i < parameters.size(); i++) {
			ParametersParameterComponent parameter = parameters.get(i);
			Optional<ParametersParameterComponent> path = parameter.getPart().stream()
				.filter(part -> part.getName().equals("path"))
				.findFirst();
			if (path.isPresent()) {
				String pathString = ((StringType)path.get().getValue()).getValue();
				EncodeContextPath e = new EncodeContextPath(pathString);
        String newIndex = "[" + String.valueOf(i + newStart) + "]"; // Replace with your desired string
				String result = pathString.replaceAll("\\[([^\\]]+)\\]", newIndex);
				path.get().setValue(new StringType(result));
			}
		};
	}
	private void fixInsertPathIndexes (List<ParametersParameterComponent> parameters, int newStart) {
		int opCounter = 0;
		for (ParametersParameterComponent parameter:parameters) {
			// ParametersParameterComponent parameter = parameters.get(i);
			// need to check for more than index here
			/**
			 * {
							"name": "operation",
							"part": [
									{
											"name": "type",
											"valueCode": "insert"
									},
									{
											"name": "path",
											"valueString": "ValueSet.expansion"
									},
									{
											"name": "index",
											"valueInteger": 64
									}
							]
					},
					{
							"name": "operation",
							"part": [
									{
											"name": "type",
											"valueCode": "insert"
									},
									{
											"name": "path",
											"valueString": "ValueSet.expansion.contains"
									},
									{
											"name": "index",
											"valueInteger": 65
									}
							]
					},
					{
							"name": "operation",
							"part": [
									{
											"name": "type",
											"valueCode": "insert"
									},
									{
											"name": "path",
											"valueString": "ValueSet.expansion.contains[0].system"
									},
									{
											"name": "index",
											"valueInteger": 66
									},
									{
											"name": "value",
											"valueUri": "http://loinc.org"
									}
							]
					},
					{
							"name": "operation",
							"part": [
									{
											"name": "type",
											"valueCode": "insert"
									},
									{
											"name": "path",
											"valueString": "ValueSet.expansion.contains[0].code"
									},
									{
											"name": "index",
											"valueInteger": 67
									},
									{
											"name": "value",
											"valueCode": "39297-7"
									}
							]
					},
					{
							"name": "operation",
							"part": [
									{
											"name": "type",
											"valueCode": "insert"
									},
									{
											"name": "path",
											"valueString": "ValueSet.expansion.contains[0].display"
									},
									{
											"name": "index",
											"valueInteger": 68
									},
									{
											"name": "value",
											"valueString": "Influenza virus A H10 Ab [Titer] in Serum by Hemagglutination inhibition"
									}
							]
					}
			 */
			
			Optional<ParametersParameterComponent> index = parameter.getPart().stream()
				.filter(part -> part.getName().equals("index"))
				.findFirst();
			Optional<ParametersParameterComponent> value = parameter.getPart().stream()
				.filter(part -> part.getName().equals("value"))
				.findFirst();
			Optional<ParametersParameterComponent> path = parameter.getPart().stream()
				.filter(part -> part.getName().equals("path"))
				.findFirst();
			if (path.isPresent()) {
				String pathString = ((StringType)path.get().getValue()).getValue();
				EncodeContextPath e = new EncodeContextPath(pathString);
				String elementName = e.getLeafElementName();
				// for contains / include, we want to update the second last index and the 
				if (elementName.equals("contains") 
				|| elementName.equals("include") 
				|| elementName.equals("relatedArtifact")) {
					if ((index.isPresent() && !value.isPresent())
					|| (elementName.equals("relatedArtifact") && index.isPresent())) {
						index.get().setValue(new IntegerType(opCounter + newStart));
						opCounter+=1;
					}
				}
				if (pathString.contains("expansion.contains") || pathString.contains("compose.include")) {
					if(value.isPresent()) {
						// subtract 1 here because the opcounter has already been incremented
						// maybe separate out the contains / include / relatedartifact rules a little more?
						// refactor into specific methods linked to specific signatures?
						String newIndex = "[" + String.valueOf(opCounter - 1 + newStart) + "]"; // Replace with your desired string
						String result = pathString.replaceAll("\\[([^\\]]+)\\]", newIndex);
						path.get().setValue(new StringType(result));
					}
				}
			}
		};
	}
	private void checkForChangesInChildren(Parameters baseDiff, MetadataResource theSourceBase, MetadataResource theTargetBase, HapiFhirRepository hapiFhirRepository, FhirPatch patch, diffCache cache, FhirContext ctx, boolean compareComputable, boolean compareExecutable,IFhirResourceDaoValueSet<ValueSet> dao) throws UnprocessableEntityException {
		// get the references in both the source and target
		List<RelatedArtifact> targetRefs = combineComponentsAndDependencies(new KnowledgeArtifactAdapter<MetadataResource>(theTargetBase));
		List<RelatedArtifact> sourceRefs = combineComponentsAndDependencies(new KnowledgeArtifactAdapter<MetadataResource>(theSourceBase));
		additionsAndDeletions<RelatedArtifact> fixed = extractAdditionsAndDeletions(sourceRefs, targetRefs, RelatedArtifact.class);
		if (fixed.getSourceMatches().size() > 0) {
			for(int i = 0; i < fixed.getSourceMatches().size(); i++) {
				String sourceCanonical = fixed.getSourceMatches().get(i).getResource();
				String targetCanonical = fixed.getTargetMatches().get(i).getResource();
				boolean diffNotAlreadyComputedAndPresent = baseDiff.getParameter(Canonicals.getUrl(targetCanonical)) == null;
				if (diffNotAlreadyComputedAndPresent) {
					MetadataResource source = checkOrUpdateResourceCache(sourceCanonical, cache, hapiFhirRepository, dao);
					MetadataResource target = checkOrUpdateResourceCache(targetCanonical, cache, hapiFhirRepository, dao);
					// need to do something smart here to expand the executable or computable resources
					checkOrUpdateDiffCache(sourceCanonical, targetCanonical, source, target, patch, cache, ctx, compareComputable, compareExecutable, dao)
						.ifPresentOrElse(diffToAppend -> {
							ParametersParameterComponent component = baseDiff.addParameter();
							component.setName(Canonicals.getUrl(sourceCanonical));
							component.setResource(diffToAppend);
							// check for changes in the children of those as well
							checkForChangesInChildren(diffToAppend, source, target, hapiFhirRepository, patch, cache, ctx, compareComputable, compareExecutable, dao);
						},
						() -> {
							if (target == null) {
								ParametersParameterComponent component = baseDiff.addParameter();
								component.setName(Canonicals.getUrl(sourceCanonical));
								component.setValue(new StringType("Target could not be retrieved"));
							} else if (source == null) { 
								ParametersParameterComponent component = baseDiff.addParameter();
								component.setName(Canonicals.getUrl(targetCanonical));
								component.setValue(new StringType("Source could not be retrieved"));
							}
						});
				}
			}
		}
		for (RelatedArtifact addition : fixed.getInsertions() ) {
			if (addition.hasResource()) {
				boolean diffNotAlreadyComputedAndPresent = baseDiff.getParameter(Canonicals.getUrl(addition.getResource())) == null;
				if (diffNotAlreadyComputedAndPresent) {
					ParametersParameterComponent component = baseDiff.addParameter();
					component.setName(Canonicals.getUrl(addition.getResource()));
					component.setValue(new StringType("Related artifact was inserted"));
				}
			}
		}
		for (RelatedArtifact deletion : fixed.getDeletions() ) {
			if (deletion.hasResource()) {
				boolean diffNotAlreadyComputedAndPresent = baseDiff.getParameter(Canonicals.getUrl(deletion.getResource())) == null;
				if (diffNotAlreadyComputedAndPresent) {
					ParametersParameterComponent component = baseDiff.addParameter();
					component.setName(Canonicals.getUrl(deletion.getResource()));
					component.setValue(new StringType("Related artifact was deleted"));
				}
			}
		}
	}
	private <T> additionsAndDeletions<T> extractAdditionsAndDeletions(List<T> source, List<T> target, Class<T> t) {
		List<T> sourceCopy = new ArrayList<T>(source);
		List<T> targetCopy = new ArrayList<T>(target);
		// this is n^2 with Lists but can be nlog(n) if we use TreeSets
		// check for matches and additions
		List<T> insertions = new ArrayList<T>();
		List<T> deletions = new ArrayList<T>();
		List<T> sourceMatches = new ArrayList<T>();
		List<T> targetMatches = new ArrayList<T>();
		targetCopy.forEach(targetObj -> {
			Optional<T> isInSource = sourceCopy.stream().filter(sourceObj -> {
				if (sourceObj instanceof RelatedArtifact && targetObj instanceof RelatedArtifact) {
					return relatedArtifactEquals((RelatedArtifact) sourceObj, (RelatedArtifact) targetObj);
				} else if (sourceObj instanceof ConceptSetComponent && targetObj instanceof ConceptSetComponent) {
					return conceptSetEquals((ConceptSetComponent)sourceObj, (ConceptSetComponent)targetObj);
				} else if (sourceObj instanceof ValueSetExpansionContainsComponent && targetObj instanceof ValueSetExpansionContainsComponent) {
					return ValueSetContainsEquals((ValueSetExpansionContainsComponent) sourceObj,(ValueSetExpansionContainsComponent) targetObj);
				} else {
					return false;
				}
			}).findAny();
			if (isInSource.isPresent()) {
				sourceMatches.add(isInSource.get());
				targetMatches.add(targetObj);
				sourceCopy.remove(isInSource.get());
			} else {
				insertions.add(targetObj);
			}
		});
		// check for deletions
		sourceCopy.forEach(sourceObj -> {
			boolean isInTarget = targetCopy.stream().anyMatch(targetObj -> {
				if (sourceObj instanceof RelatedArtifact && targetObj instanceof RelatedArtifact) {
					return relatedArtifactEquals((RelatedArtifact) sourceObj, (RelatedArtifact) targetObj);
				} else if (sourceObj instanceof ConceptSetComponent && targetObj instanceof ConceptSetComponent) {
					return conceptSetEquals((ConceptSetComponent)sourceObj, (ConceptSetComponent)targetObj);
				} else if (sourceObj instanceof ValueSetExpansionContainsComponent && targetObj instanceof ValueSetExpansionContainsComponent) {
					return ValueSetContainsEquals((ValueSetExpansionContainsComponent) sourceObj,(ValueSetExpansionContainsComponent) targetObj);
				} else {
					return false;
				}
			});
			if (!isInTarget) {
				deletions.add(sourceObj);
			}
		});
		return new additionsAndDeletions<T>(sourceMatches,targetMatches,insertions,deletions,t);
	}
	private boolean relatedArtifactEquals(RelatedArtifact ref1, RelatedArtifact ref2) {
		return Canonicals.getUrl(ref1.getResource()).equals(Canonicals.getUrl(ref2.getResource())) && ref1.getType() == ref2.getType();
	}
	// RelatedArtifact extensions should diff nicely too....eventually
	private boolean extensionEquals(Extension ref1, Extension ref2) {
		return ref1.getUrl().equals(ref2.getUrl());
	}
	private boolean conceptSetEquals(ConceptSetComponent ref1, ConceptSetComponent ref2) {
		// consider any includes which share at least 1 URL
		if (ref1.hasValueSet() && ref2.hasValueSet()) {
			List<String> ref1Urls = ref1.getValueSet().stream().map(CanonicalType::getValue).collect(Collectors.toList());
			List<String> intersect = ref2.getValueSet().stream().map(CanonicalType::getValue).filter(ref1Urls::contains).collect(Collectors.toList());		
			return intersect.size() > 0;
		} else if (!ref1.hasValueSet() && !ref2.hasValueSet()) {
			return ref1.getSystem().equals(ref2.getSystem());
		} else {
			// if one conceptSet has a value set but not the other then they can't be updates of each other
			return false;
		}
	}
	private boolean ValueSetContainsEquals(ValueSetExpansionContainsComponent ref1, ValueSetExpansionContainsComponent ref2) {
		return ref1.getSystem().equals(ref2.getSystem()) && ref1.getCode().equals(ref2.getCode());
	}
	private MetadataResource checkOrUpdateResourceCache(String url, diffCache cache, HapiFhirRepository hapiFhirRepository, IFhirResourceDaoValueSet<ValueSet> dao) throws UnprocessableEntityException {
		MetadataResource resource = cache.getResource(url);
		if (resource == null) {
			try {
				resource = retrieveResourcesByCanonical(url, hapiFhirRepository);
			} catch (ResourceNotFoundException e) {
				// ignore
			}
			if (resource != null) {
				if (resource instanceof ValueSet) {
					try {
						doesValueSetNeedExpansion((ValueSet)resource, dao);
					} catch (Exception e) {
						throw new UnprocessableEntityException("Could not expand ValueSet: " + e.getMessage());
					}
				}
				cache.addResource(url, resource);
			}
		}
		return resource;
	}
	private Optional<Parameters> checkOrUpdateDiffCache(String sourceCanonical, String targetCanonical, MetadataResource source, MetadataResource target, FhirPatch patch, diffCache cache, FhirContext ctx, boolean compareComputable, boolean compareExecutable,IFhirResourceDaoValueSet<ValueSet> dao) {
		Parameters retval = cache.getDiff(sourceCanonical, targetCanonical);
		if (retval == null) {
			if (target != null) {
				if (source instanceof Library || source instanceof PlanDefinition) {
					retval = handleRelatedArtifactArrayElementsDiff(source, target, patch);
				} else if (source instanceof ValueSet) {
					retval = advancedValueSetDiff(source, target, patch, compareComputable, compareExecutable);
				} else {
					retval = (Parameters) patch.diff(source, target);
				}
				cache.addDiff(sourceCanonical, targetCanonical, retval);
			}
		}
		return Optional.ofNullable(retval);
	}
	/* $revise */
	public MetadataResource revise(HapiFhirRepository hapiFhirRepository, MetadataResource resource) {
		MetadataResource existingResource = (MetadataResource)hapiFhirRepository.read(ResourceClassMapHelper.getClass(resource.getResourceType().name()), resource.getIdElement());
		if (existingResource == null) {
			throw new IllegalArgumentException(String.format("Resource with ID: '%s' not found.", resource.getId()));
		}

		if (!existingResource.getStatus().equals(Enumerations.PublicationStatus.DRAFT)) {
			throw new IllegalStateException(String.format("Current resource status is '%s'. Only resources with status of 'draft' can be revised.", resource.getStatus().toString()));
		}

		if (!resource.getStatus().equals(Enumerations.PublicationStatus.DRAFT)) {
			throw new IllegalStateException(String.format("The resource status can not be updated from 'draft'. The proposed resource has status: %s", resource.getStatus().toString()));
		}

		hapiFhirRepository.update(resource);

		return resource;
	}
	private class additionsAndDeletions<T> {
		private List<T> mySourceMatches;
		private List<T> myTargetMatches;
		private List<T> myInsertions;
		private List<T> myDeletions;
		private Class<T> t;
		public additionsAndDeletions(List<T> sourceMatches,List<T> targetMatches,List<T> additions,List<T> deletions, Class<T> t) {
			this.mySourceMatches = sourceMatches;
			this.myTargetMatches = targetMatches;
			this.myInsertions = additions;
			this.myDeletions = deletions;
			this.t = t;
		}
    public List<T> getSourceMatches() { return this.mySourceMatches; }
    public List<T> getTargetMatches() { return this.myTargetMatches; }
    public List<T> getInsertions() { return this.myInsertions; }
    public List<T> getDeletions() { return this.myDeletions; }
		public void appendInsertOperations	(Parameters theBase, FhirPatch thePatch, int theStartIndex) throws UnprocessableEntityException {
			prepareForComparison	( theBase,  thePatch,  theStartIndex,  true, this.myInsertions);
		}
		public void appendDeleteOperations	(Parameters theBase, FhirPatch thePatch, int theStartIndex) throws UnprocessableEntityException {
			prepareForComparison	( theBase,  thePatch,  theStartIndex,  false, this.myDeletions);
		}
		/**
		 * 
		 * @param theBase base diff to append to
		 * @param thePatch patch instance which performs the diff
		 * @param theStartIndex where the start numbering the operations
		 * @param theInsertOrDelete true = insert, false = delete
		 * @param theResourcesToAdd list of insertions or deletions
		 * @throws UnprocessableEntityException
		 */
		private void prepareForComparison	(Parameters theBase, FhirPatch thePatch, int theStartIndex, boolean theInsertOrDelete, List<T> theResourcesToAdd) throws UnprocessableEntityException {
			if (this.myInsertions.size() > 0) {
				MetadataResource empty;
				MetadataResource hasNewResources;
				if (this.t.isAssignableFrom(RelatedArtifact.class)) {
					empty = new Library();
					hasNewResources = new Library();
					((Library)hasNewResources).setRelatedArtifact((List<RelatedArtifact>)theResourcesToAdd);
				} else if (this.t.isAssignableFrom(ConceptSetComponent.class)) {
					empty = new ValueSet();
					((ValueSet)empty).setCompose(new ValueSetComposeComponent().setInclude(new ArrayList<>()));
					hasNewResources = new ValueSet();
					((ValueSet)hasNewResources).setCompose(new ValueSetComposeComponent().setInclude((List<ConceptSetComponent>)theResourcesToAdd));
				} else if (this.t.isAssignableFrom(ValueSetExpansionContainsComponent.class)) {
					empty = new ValueSet();
					((ValueSet)empty).setExpansion(new ValueSetExpansionComponent().setContains(new ArrayList<>()));
					hasNewResources = new ValueSet();
					((ValueSet)hasNewResources).setExpansion(new ValueSetExpansionComponent().setContains((List<ValueSetExpansionContainsComponent>)theResourcesToAdd));
				} else {
					throw new UnprocessableEntityException("Could not process object");
				}
				if (theInsertOrDelete) {
					appendInsertOperations(theBase, empty, hasNewResources, thePatch, theStartIndex);
				} else {
					// swap source and target for deletions
					appendDeleteOperations(theBase, hasNewResources, empty, thePatch, theStartIndex);
				}
			}
		}
		private void appendInsertOperations(Parameters theBase, IBaseResource theSource,IBaseResource theTarget, FhirPatch thePatch, int theStartIndex) {
			Parameters insertions = (Parameters) thePatch.diff(theSource,theTarget);
			fixInsertPathIndexes(insertions.getParameter(), theStartIndex);
			theBase.getParameter().addAll(insertions.getParameter());
		}
		private void appendDeleteOperations(Parameters theBase, IBaseResource theSource,IBaseResource theTarget, FhirPatch thePatch, int theStartIndex) {
			Parameters deletions = (Parameters) thePatch.diff(theSource,theTarget);
			fixDeletePathIndexes(deletions.getParameter(), theStartIndex);
			theBase.getParameter().addAll(deletions.getParameter());
		}
	}
}
