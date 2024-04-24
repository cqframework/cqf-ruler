package org.opencds.cqf.ruler.casereporting.r4;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.beanutils.PropertyUtilsBean;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UsageContext;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetComposeComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.support.ValueSetExpansionOptions;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDaoValueSet;
import ca.uhn.fhir.jpa.patch.FhirPatch;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.parser.path.EncodeContextPath;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
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
	public static final String authoritativeSourceUrl = "http://hl7.org/fhir/StructureDefinition/valueset-authoritativeSource";
	public static final String expansionParametersUrl = "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-expansion-parameters-extension";
	public static final String valueSetPriorityUrl = "http://aphl.org/fhir/vsm/StructureDefinition/vsm-valueset-priority";
	public static final String valueSetConditionUrl = "http://aphl.org/fhir/vsm/StructureDefinition/vsm-valueset-condition";
	public static final String vsmValueSetTagCodeSystemUrl = "http://aphl.org/fhir/vsm/CodeSystem/vsm-valueset-tag";
	public static final String vsmValueSetTagVSMAuthoredCode = "vsm-authored";
	public static final String valueSetPriorityCode = "priority";
	public static final String valueSetConditionCode = "focus";
	public static final List<String> preservedExtensionUrls = List.of(
			valueSetPriorityUrl,
			valueSetConditionUrl
		);
	public static final String usPhContextTypeUrl = "http://hl7.org/fhir/us/ecr/CodeSystem/us-ph-usage-context-type";
	public static final String contextTypeUrl = "http://terminology.hl7.org/CodeSystem/usage-context-type";
	public static final String contextUrl = "http://hl7.org/fhir/us/ecr/CodeSystem/us-ph-usage-context";

	public static String getPriority(MetadataResource resource) {
		var priorities = getCodeableConceptExtensions(resource,KnowledgeArtifactProcessor.valueSetPriorityUrl);
		if (priorities == null || priorities.isEmpty()) {
			// throw new UnprocessableEntityException("Could not find priority for resource: " + resource.getUrl());
			return null;
		} else
		if (priorities.size() > 1) {
			throw new UnprocessableEntityException("Multiple priorities for resource: " + resource.getUrl());
		}
		return priorities.get(0).getCodingFirstRep().getCode();
	}
	public static List<CodeableConcept> getConditions(MetadataResource resource) {
		return getCodeableConceptExtensions(resource,KnowledgeArtifactProcessor.valueSetConditionUrl);
	}
	private static List<CodeableConcept> getCodeableConceptExtensions(MetadataResource resource, String url) {
		return resource.getExtensionsByUrl(url).stream()
    .filter(e-> e.hasValue())
    .filter(e -> e.getValue() instanceof CodeableConcept)
    .map(e -> (CodeableConcept)e.getValue())
    .collect(Collectors.toList());
	}
	private MetadataResource retrieveResourcesByCanonical(String reference, Repository hapiFhirRepository) throws ResourceNotFoundException {
		var referencedResourceBundle = SearchHelper.searchRepositoryByCanonicalWithPaging(hapiFhirRepository, reference);
		var referencedResource = KnowledgeArtifactAdapter.findLatestVersion(referencedResourceBundle);
		if (referencedResource.isEmpty()) {
			throw new ResourceNotFoundException(String.format("Resource for Canonical '%s' not found.", reference));
		}
		return (MetadataResource)referencedResource.get();
	}

	public static List<IDependencyInfo> getRelatedArtifactsWithPreservedExtensions(List<IDependencyInfo> deps) {
		return deps.stream()
			.filter(ra -> preservedExtensionUrls
				.stream().anyMatch(url -> ra.getExtension()
					.stream().anyMatch(ext -> ext.getUrl().equalsIgnoreCase(url))))
			.collect(Collectors.toList());
	}
	
	public static List<MetadataResource> getResourcesFromBundle(Bundle bundle) {
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
	public static void checkIfValueSetNeedsCondition(MetadataResource resource, RelatedArtifact relatedArtifact, Repository hapiFhirRepository) throws UnprocessableEntityException {
		if (resource == null 
		&& relatedArtifact != null 
		&& relatedArtifact.getResource() != null 
		&& Canonicals.getResourceType(relatedArtifact.getResource()).equals("ValueSet")) {
			var searchResults = BundleHelper.getEntryResources(SearchHelper.searchRepositoryByCanonicalWithPaging( hapiFhirRepository, relatedArtifact.getResource()));
			if (searchResults.size() > 0) {
				resource = (MetadataResource)searchResults.get(0);
			}
		}
		if (resource != null && resource.getResourceType() == ResourceType.ValueSet) {
			var valueSet = (ValueSet)resource;
			var isLeaf = !valueSet.hasCompose() || (valueSet.hasCompose() && valueSet.getCompose().getIncludeFirstRep().getValueSet().size() == 0);
			var maybeConditionExtension = Optional.ofNullable(relatedArtifact)
				.map(RelatedArtifact::getExtension)
				.map(list -> {
					return list.stream().map(e->(Extension)e).filter(ext -> ext.getUrl().equalsIgnoreCase(valueSetConditionUrl)).findFirst().orElse(null);
				});
			if (isLeaf && !maybeConditionExtension.isPresent()) {
				throw new UnprocessableEntityException("Missing condition on ValueSet : " + valueSet.getUrl());
			}
		}
	}
	public static void checkIfValueSetNeedsCondition(MetadataResource resource, IDependencyInfo relatedArtifact, Repository hapiFhirRepository) throws UnprocessableEntityException {
		if (resource == null 
		&& relatedArtifact != null 
		&& relatedArtifact.getReference() != null 
		&& Canonicals.getResourceType(relatedArtifact.getReference()).equals("ValueSet")) {
			var searchResults = BundleHelper.getEntryResources(SearchHelper.searchRepositoryByCanonicalWithPaging( hapiFhirRepository, relatedArtifact.getReference()));
			if (searchResults.size() > 0) {
				resource = (MetadataResource)searchResults.get(0);
			}
		}
		if (resource != null && resource.getResourceType() == ResourceType.ValueSet) {
			var valueSet = (ValueSet)resource;
			var isLeaf = !valueSet.hasCompose() || (valueSet.hasCompose() && valueSet.getCompose().getIncludeFirstRep().getValueSet().size() == 0);
			var maybeConditionExtension = Optional.ofNullable(relatedArtifact)
				.map(IDependencyInfo::getExtension)
				.map(list -> {
					return list.stream().map(e->(Extension)e).filter(ext -> ext.getUrl().equalsIgnoreCase(valueSetConditionUrl)).findFirst().orElse(null);
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
	public static void handleValueSetReferenceExtensions(MetadataResource manifest, List<BundleEntryComponent> bundleEntries, Repository hapiFhirRepository) throws UnprocessableEntityException, IllegalArgumentException {
		var adapter = AdapterFactory.forFhirVersion(FhirVersionEnum.R4).createKnowledgeArtifactAdapter(manifest);
		var relatedArtifactsWithPreservedExtension = getRelatedArtifactsWithPreservedExtensions(adapter.getDependencies());
		bundleEntries.stream()
			.forEach(entry -> {
				if (entry.getResource().getResourceType().equals(ResourceType.ValueSet)) {
					var valueSet = (ValueSet) entry.getResource();
					// remove any existing Priority and Conditions
					var usageContexts = removeExistingReferenceExtensionData(valueSet.getUseContext());
					valueSet.setUseContext(usageContexts);
					var maybeVSRelatedArtifact = relatedArtifactsWithPreservedExtension.stream().filter(ra -> Canonicals.getUrl(ra.getReference()).equals(valueSet.getUrl())).findFirst();
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
					var priority = getOrCreateUsageContext(usageContexts, usPhContextTypeUrl, valueSetPriorityCode);
					maybeVSRelatedArtifact
						.map(ra -> ra.getExtension().stream().map(e->(Extension)e).filter(e -> e.getUrl().equals(valueSetPriorityUrl)).findAny().orElse(null))
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
	public static List<UsageContext> removeExistingReferenceExtensionData(List<UsageContext> usageContexts) {
		List<String> useContextCodesToReplace = List.of(valueSetConditionCode,valueSetPriorityCode);
		return usageContexts.stream()
		// remove any useContexts which need to be replaced
			.filter(useContext -> !useContextCodesToReplace.stream()
				.anyMatch(code -> useContext.getCode().getCode().equals(code)))
			.collect(Collectors.toList());
	}

	public static void tryAddCondition(List<UsageContext> usageContexts, CodeableConcept condition) {
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
	public static UsageContext getOrCreateUsageContext(List<UsageContext> usageContexts, String system, String code) {
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
	public Parameters artifactDiff(MetadataResource theSourceLibrary, MetadataResource theTargetLibrary, FhirContext theContext, Repository hapiFhirRepository, boolean compareComputable, boolean compareExecutable,IFhirResourceDaoValueSet<ValueSet> dao, diffCache cache) throws UnprocessableEntityException {
		// setup
		FhirPatch patch = new FhirPatch(theContext);
		patch.setIncludePreviousValueInDiff(true);
		// ignore meta changes
		patch.addIgnorePath("*.meta");
		Parameters libraryDiff = handleRelatedArtifactArrayElementsDiff(theSourceLibrary,theTargetLibrary,patch);

		// then check for references and add those to the base Parameters object
		if (cache == null) {
			cache = new diffCache();
		}
		var sourceCanonical = theSourceLibrary.getUrl()+"|"+theSourceLibrary.getVersion();
		var targetCanonical = theTargetLibrary.getUrl()+"|"+theTargetLibrary.getVersion();
		cache.addSource(sourceCanonical, theSourceLibrary);
		cache.addTarget(targetCanonical, theTargetLibrary);
		cache.addDiff(sourceCanonical, targetCanonical, libraryDiff);
		checkForChangesInChildren(libraryDiff, theSourceLibrary, theTargetLibrary, hapiFhirRepository, patch, cache, theContext, compareComputable, compareExecutable,dao);
		return libraryDiff;
	}
	private Parameters handleRelatedArtifactArrayElementsDiff(MetadataResource theSourceLibrary,MetadataResource theTargetLibrary, FhirPatch patch) {
		var updateSource = AdapterFactory.forFhirVersion(FhirVersionEnum.R4).createKnowledgeArtifactAdapter(theSourceLibrary.copy());
		var updateTarget = AdapterFactory.forFhirVersion(FhirVersionEnum.R4).createKnowledgeArtifactAdapter(theTargetLibrary.copy());
		additionsAndDeletions<RelatedArtifact> processedRelatedArtifacts = extractAdditionsAndDeletions(updateSource.getRelatedArtifact(), updateTarget.getRelatedArtifact(), RelatedArtifact.class);
		updateSource.setRelatedArtifact(processedRelatedArtifacts.getSourceMatches());
		updateTarget.setRelatedArtifact(processedRelatedArtifacts.getTargetMatches());
		Parameters updateOperations = (Parameters) patch.diff(updateSource.copy(),updateTarget.copy());
		processedRelatedArtifacts.appendInsertOperations(updateOperations, patch, updateSource.getRelatedArtifact().size());
		processedRelatedArtifacts.appendDeleteOperations(updateOperations, patch, updateSource.getRelatedArtifact().size());
		processedRelatedArtifacts.reorderArrayElements(theSourceLibrary, theTargetLibrary);
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
			composeIncludeProcessed.reorderArrayElements(theSourceValueSet, theTargetValueSet);
		}
		if (compareExecutable) {
			expansionContainsProcessed.appendInsertOperations(vsDiff, patch, updateTarget.getExpansion().getContains().size());	
			expansionContainsProcessed.appendDeleteOperations(vsDiff, patch, updateTarget.getExpansion().getContains().size());	
			expansionContainsProcessed.reorderArrayElements(theSourceValueSet, theTargetValueSet);
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

	public void expandValueSet(ValueSet valueSet, Parameters expansionParameters) {
		// Gather the Terminology Service from the valueSet's authoritativeSourceUrl.
		Extension authoritativeSource = valueSet.getExtensionByUrl(authoritativeSourceUrl);
		String authoritativeSourceUrl = authoritativeSource != null && authoritativeSource.hasValue()
			? authoritativeSource.getValue().primitiveValue()
			: valueSet.getUrl();

		// TODO: Given the authoritativeSourceUrl, lookup Tx Service connection configuration - is this possible? Problem is we can't reliably infer Tx Service from authSource
//		terminologyServerClient.setUsername(config.getUsername(authoritativeSourceUrl));
//		terminologyServerClient.setApiKey(config.getApiKey(authoritativeSourceUrl));

		ValueSet expandedValueSet;
		if (isVSMAuthoredValueSet(valueSet) && hasSimpleCompose(valueSet)) {
			// Perform naive expansion independent of terminology servers. Copy all codes from compose into expansion.
			ValueSet.ValueSetExpansionComponent expansion = new ValueSet.ValueSetExpansionComponent();
			expansion.setTimestamp(Date.from(Instant.now()));

			ArrayList<ValueSet.ValueSetExpansionParameterComponent> expansionParams = new ArrayList<>();
			ValueSet.ValueSetExpansionParameterComponent parameterNaive = new ValueSet.ValueSetExpansionParameterComponent();
			parameterNaive.setName("naive");
			parameterNaive.setValue(new BooleanType(true));
			expansionParams.add(parameterNaive);
			expansion.setParameter(expansionParams);

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
		} else {
			try {
				expandedValueSet = terminologyServerClient.expand(valueSet, authoritativeSourceUrl, expansionParameters);
				valueSet.setExpansion(expandedValueSet.getExpansion());
			} catch (Exception ex) {
				myLog.warn("Terminology Server expansion failed: {}", valueSet.getIdElement().getValue(), ex);
			}
		}
	}

	public boolean isVSMAuthoredValueSet(ValueSet valueSet) {
		return valueSet.hasMeta()
			&& valueSet.getMeta().hasTag()
			&& valueSet.getMeta().getTag(vsmValueSetTagCodeSystemUrl, vsmValueSetTagVSMAuthoredCode) != null;
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

	public static class diffCache {
		private final Map<String,Parameters> diffs = new HashMap<String,Parameters>();
		private final Map<String,DiffCacheResource> resources = new HashMap<String,DiffCacheResource>();
		public diffCache() {
			super();
		}
		public void addDiff(String sourceUrl, String targetUrl, Parameters diff) {
			this.diffs.put(sourceUrl+"-"+targetUrl, diff);
		}
		public Parameters getDiff(String sourceUrl, String targetUrl) {
			return this.diffs.get(sourceUrl+"-"+targetUrl);
		}
		public void addSource(String url, MetadataResource resource) {
			this.resources.put(url, new DiffCacheResource(resource,true));
		}
		public void addTarget(String url, MetadataResource resource) {
			this.resources.put(url, new DiffCacheResource(resource,false));
		}
		public Optional<MetadataResource> getResource(String url) {
			var resource = Optional.ofNullable(this.resources.get(url)).map(r -> r.resource);
			if (!resource.isPresent()) {
				var possibleMatches = getResourcesForUrl(url);
				// what if there are multiple matches? Does this throw an error?
				if (possibleMatches.size() > 0) {
					resource = Optional.of(possibleMatches.get(0).resource);
				}
			}
			return resource;
		}
		public List<DiffCacheResource> getResourcesForUrl(String url) {
			return this.resources.keySet().stream()
				.filter(k -> Canonicals.getUrl(k).equals(url))
				.map(k -> this.resources.get(k))
				.collect(Collectors.toList());
		}
		public class DiffCacheResource {
			public MetadataResource resource;
			public boolean isSource;
			DiffCacheResource(MetadataResource resource, boolean isSource) {
				this.resource = resource;
				this.isSource = isSource;
			}
		}
	}
	private void fixDeletePathIndexesAndAddValues(List<ParametersParameterComponent> parameters, int newStart, IBaseResource sourceResource) {
		for (int i = 0; i < parameters.size(); i++) {
			ParametersParameterComponent parameter = parameters.get(i);
			Optional<ParametersParameterComponent> path = parameter.getPart().stream()
			.filter(part -> part.hasName())	
			.filter(part -> part.getName().equals("path"))
			.findFirst();
			if (path.isPresent()) {
				
				String pathString = ((StringType)path.get().getValue()).getValue();
				EncodeContextPath e = new EncodeContextPath(pathString);
				String noBase = removeBase(e);
				try {
					Optional.ofNullable((new PropertyUtilsBean()).getProperty(sourceResource, noBase))
						.ifPresent(oldVal -> {
							if (oldVal instanceof Type) {
								parameter.addPart().setName("previousValue").setValue((Type)oldVal);
							} else if (oldVal instanceof ValueSet.ValueSetExpansionContainsComponent) {
								var exp = (ValueSet.ValueSetExpansionContainsComponent)oldVal;
								parameter.addPart().setName("previousValue").setValue(exp.getCodeElement());
							} else if (oldVal instanceof ValueSet.ConceptSetComponent) {
								var cmp = (ValueSet.ConceptSetComponent)oldVal;
								cmp.getConcept().forEach(ref -> {
									parameter.addPart().setName("previousValue").setValue(ref.getCodeElement());
								});
								cmp.getValueSet().forEach(vs -> {
									parameter.addPart().setName("previousValue").setValue(vs);
								});
							} else if (oldVal instanceof RelatedArtifact) {
								var ra = (RelatedArtifact)oldVal;
								parameter.addPart().setName("previousValue").setValue(ra.getResourceElement());
							}
						});
					
				} catch (Exception oops) {
					// TODO: handle exception
					var opps = oops.getMessage();
				}
        String newIndex = "[" + String.valueOf(i + newStart) + "]"; // Replace with your desired string
				String result = pathString.replaceAll("\\[([^\\]]+)\\]", newIndex);
				path.get().setValue(new StringType(result));
			}
		};
	}
	private String removeBase(EncodeContextPath path) {
		return path.getPath().subList(1, path.getPath().size())
		.stream()
		.map(t -> t.toString())
		.collect(Collectors.joining("."));
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
	private void checkForChangesInChildren(Parameters baseDiff, MetadataResource theSourceBase, MetadataResource theTargetBase, Repository hapiFhirRepository, FhirPatch patch, diffCache cache, FhirContext ctx, boolean compareComputable, boolean compareExecutable,IFhirResourceDaoValueSet<ValueSet> dao) throws UnprocessableEntityException {
		// get the references in both the source and target
		var targetReferences = AdapterFactory.forFhirVersion(FhirVersionEnum.R4).createKnowledgeArtifactAdapter(theTargetBase).getRelatedArtifact().stream().map(ra->(RelatedArtifact)ra).collect(Collectors.toList());
		var sourceReferences = AdapterFactory.forFhirVersion(FhirVersionEnum.R4).createKnowledgeArtifactAdapter(theSourceBase).getRelatedArtifact().stream().map(ra->(RelatedArtifact)ra).collect(Collectors.toList());
		var combinedReferenceList = extractAdditionsAndDeletions(sourceReferences, targetReferences, RelatedArtifact.class);
		if (combinedReferenceList.getSourceMatches().size() > 0) {
			for(int i = 0; i < combinedReferenceList.getSourceMatches().size(); i++) {
				var sourceCanonical = combinedReferenceList.getSourceMatches().get(i).getResource();
				var targetCanonical = combinedReferenceList.getTargetMatches().get(i).getResource();
				boolean diffNotAlreadyComputedAndPresent = baseDiff.getParameter(Canonicals.getUrl(targetCanonical)) == null;
				if (diffNotAlreadyComputedAndPresent) {
					var source = checkOrUpdateResourceCache(sourceCanonical, cache, hapiFhirRepository, dao, true);
					var target = checkOrUpdateResourceCache(targetCanonical, cache, hapiFhirRepository, dao, false);
					// need to do something smart here to expand the executable or computable resources
					checkOrUpdateDiffCache(sourceCanonical, targetCanonical, source, target, patch, cache, ctx, compareComputable, compareExecutable, dao)
						.ifPresentOrElse(diffToAppend -> {
							var component = baseDiff.addParameter();
							component.setName(Canonicals.getUrl(sourceCanonical));
							component.setResource(diffToAppend);
							// check for changes in the children of those as well
							checkForChangesInChildren(diffToAppend, source, target, hapiFhirRepository, patch, cache, ctx, compareComputable, compareExecutable, dao);
						},
						() -> {
							if (target == null) {
								var component = baseDiff.addParameter();
								component.setName(Canonicals.getUrl(sourceCanonical));
								component.setValue(new StringType("Target could not be retrieved"));
							} else if (source == null) { 
								var component = baseDiff.addParameter();
								component.setName(Canonicals.getUrl(targetCanonical));
								component.setValue(new StringType("Source could not be retrieved"));
							}
						});
				}
			}
		}
		for (var addition : combinedReferenceList.getInsertions() ) {
			if (addition.hasResource()) {
				boolean diffNotAlreadyComputedAndPresent = baseDiff.getParameter(Canonicals.getUrl(addition.getResource())) == null;
				if (diffNotAlreadyComputedAndPresent) {
					var component = baseDiff.addParameter();
					component.setName(Canonicals.getUrl(addition.getResource()));
					component.setValue(new StringType("Related artifact was inserted"));
					checkOrUpdateResourceCache(addition.getResource(), cache, hapiFhirRepository, dao, false);
				}
			}
		}
		for (var deletion : combinedReferenceList.getDeletions() ) {
			if (deletion.hasResource()) {
				boolean diffNotAlreadyComputedAndPresent = baseDiff.getParameter(Canonicals.getUrl(deletion.getResource())) == null;
				if (diffNotAlreadyComputedAndPresent) {
					var component = baseDiff.addParameter();
					component.setName(Canonicals.getUrl(deletion.getResource()));
					component.setValue(new StringType("Related artifact was deleted"));
					checkOrUpdateResourceCache(deletion.getResource(), cache, hapiFhirRepository, dao, true);
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
	private MetadataResource checkOrUpdateResourceCache(String url, diffCache cache, Repository hapiFhirRepository, IFhirResourceDaoValueSet<ValueSet> dao, boolean isSource) throws UnprocessableEntityException {
		var resource = cache.getResource(url).orElse(null);
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
				if(isSource) {
					cache.addSource(url, resource);
				} else {
					cache.addTarget(url, resource);
				}
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
	public MetadataResource revise(Repository hapiFhirRepository, MetadataResource resource) {
		var existingResource = (MetadataResource)SearchHelper.readRepository(hapiFhirRepository, resource.getIdElement());
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
		public void reorderArrayElements(MetadataResource theSourceResource, MetadataResource theTargetResource) {
			var sourceAdapter = AdapterFactory.forFhirVersion(FhirVersionEnum.R4).createKnowledgeArtifactAdapter(theSourceResource);
			var targetAdapter = AdapterFactory.forFhirVersion(FhirVersionEnum.R4).createKnowledgeArtifactAdapter(theTargetResource);
			if (this.t.isAssignableFrom(RelatedArtifact.class) ) {
				var sourceRelatedArtifacts = (List<RelatedArtifact>)Stream.concat(this.mySourceMatches.stream(), this.myDeletions.stream()).collect(Collectors.toList());
				var targetRelatedArtifacts = (List<RelatedArtifact>)Stream.concat(this.myTargetMatches.stream(), this.myInsertions.stream()).collect(Collectors.toList());
				sourceAdapter.setRelatedArtifact(sourceRelatedArtifacts);
				targetAdapter.setRelatedArtifact(targetRelatedArtifacts);
			} else if (this.t.isAssignableFrom(ConceptSetComponent.class) 
			&& (theSourceResource instanceof ValueSet && theTargetResource instanceof ValueSet)) {
				var sourceComposeInclude = (List<ConceptSetComponent>)Stream.concat(this.mySourceMatches.stream(), this.myDeletions.stream()).collect(Collectors.toList());
				var targetComposeInclude = (List<ConceptSetComponent>)Stream.concat(this.myTargetMatches.stream(), this.myInsertions.stream()).collect(Collectors.toList());
				((ValueSet)theSourceResource).getCompose().setInclude(sourceComposeInclude);
				((ValueSet)theTargetResource).getCompose().setInclude(targetComposeInclude);
			} else if (this.t.isAssignableFrom(ValueSetExpansionContainsComponent.class)
			&& (theSourceResource instanceof ValueSet && theTargetResource instanceof ValueSet)) {
				var sourceExpansionContains = (List<ValueSetExpansionContainsComponent>)Stream.concat(this.mySourceMatches.stream(), this.myDeletions.stream()).collect(Collectors.toList());
				var targetExpansionContains = (List<ValueSetExpansionContainsComponent>)Stream.concat(this.myTargetMatches.stream(), this.myInsertions.stream()).collect(Collectors.toList());
				((ValueSet)theSourceResource).getExpansion().setContains(sourceExpansionContains);
				((ValueSet)theTargetResource).getExpansion().setContains(targetExpansionContains);
			} else {

			}
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
			fixDeletePathIndexesAndAddValues(deletions.getParameter(), theStartIndex, theSource);
			theBase.getParameter().addAll(deletions.getParameter());
		}
	}
}