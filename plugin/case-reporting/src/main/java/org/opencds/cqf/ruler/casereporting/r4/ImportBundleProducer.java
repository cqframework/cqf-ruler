package org.opencds.cqf.ruler.casereporting.r4;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.UsageContext;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.ruler.casereporting.TransformProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ImportBundleProducer {

	private static final Logger myLogger = LoggerFactory.getLogger(ImportBundleProducer.class);


	/**
	 * Determines whether a given ValueSet is a grouper
	 * @param resource
	 * @return
	 */
	public static boolean isGrouper(MetadataResource resource) {
		return resource.getResourceType() == ResourceType.ValueSet
			&& ((ValueSet) resource).hasCompose()
			&& ((ValueSet) resource).getCompose().getIncludeFirstRep().getValueSet().size() > 0;
	}

	public static boolean isRootSpecificationLibrary(Resource resource) {
		return resource.hasMeta() && resource.getMeta().hasProfile(TransformProperties.usPHSpecLibProfile);
	}

	private static boolean isModelGrouperUseContextMissing(ValueSet vs) {
		return vs.getUseContext().stream()
			.noneMatch(uc ->
				uc.getValue() instanceof CodeableConcept &&
					uc.getValueCodeableConcept().getCodingFirstRep().getCode().equals("model-grouper") &&
					uc.getCode().getCode().equals("grouper-type")
			);
	}

	private static void addModelGrouperUseContextIfMissing(ValueSet vs) {
		if(isModelGrouperUseContextMissing(vs)){
			UsageContext usageContext = new UsageContext();

			Coding code = new Coding();
			code.setSystem(TransformProperties.grouperUsageContextCodeURL);
			code.setCode("grouper-type");

			Coding valueCodeableConceptCoding = new Coding();
			valueCodeableConceptCoding.setCode("model-grouper");
			valueCodeableConceptCoding.setSystem(TransformProperties.grouperUsageContextCodableConceptSystemURL);

			usageContext.setCode(code);
			usageContext.getValueCodeableConcept().setText("Model grouper");
			usageContext.getValueCodeableConcept().getCoding().add(valueCodeableConceptCoding);

			vs.addUseContext(usageContext);
		}
	}

	public static List<Bundle.BundleEntryComponent> transformImportBundle(Bundle parameterBundle, TransformProperties transformProperties) throws FhirResourceExists {
		// store for processing root library
		HashMap<String, List<CodeableConcept>> conditionsMap = new HashMap<>();
		HashMap<String, List<CodeableConcept>> priorityMap = new HashMap<>();
		List<String> groupers = new ArrayList<>();

		PlanDefinition planDefinition = null;
		Library rootLibrary = null;
		Library rctcLibrary = null;

		List<Bundle.BundleEntryComponent> bundleEntries = new ArrayList<>();
		List<Bundle.BundleEntryComponent> entries = parameterBundle.getEntry();
		for (Bundle.BundleEntryComponent entry : entries) {
			if (entry.getResource() instanceof MetadataResource) {
				MetadataResource resource = (MetadataResource) entry.getResource();

				switch (resource.getResourceType()) {
					case ValueSet:
						ValueSet valueSet = (ValueSet) resource;
						String pinnedVersionKey = valueSet.getVersion() == null ? valueSet.getUrl() : valueSet.getUrl() + "|" + valueSet.getVersion();
						if (isGrouper(valueSet)) {
							addModelGrouperUseContextIfMissing(valueSet);
							List<CanonicalType> grouperProfiles = addMetaProfileUrl(valueSet.getMeta(), Collections.singletonList(TransformProperties.valueSetGrouperProfile));
							valueSet.getMeta().setProfile(grouperProfiles);
							groupers.add(pinnedVersionKey);
						} else {
							// Leaf ValueSets
							List<CanonicalType> leafVsProfiles = addMetaProfileUrl(
								resource.getMeta(),
								Arrays.asList(TransformProperties.leafValueSetVsmHostedProfile, TransformProperties.leafValueSetConditionProfile)
							);
							valueSet.getMeta().setProfile(leafVsProfiles);

							// Capture all the conditions and priority from the leaf valueset
							valueSet.getUseContext().forEach(context -> {
								if (context.hasCode()) {
									String code = context.getCode().getCode();
									if (code.equals("focus")) {
										if (conditionsMap.containsKey(pinnedVersionKey)) {
											List<CodeableConcept> conditions = conditionsMap.get(pinnedVersionKey);
											conditions.add(context.getValueCodeableConcept());
										} else {
											conditionsMap.put(pinnedVersionKey, new ArrayList<>(Collections.singletonList(context.getValueCodeableConcept())));
										}
									} else if (code.equals("priority")) {
										if (priorityMap.containsKey(pinnedVersionKey)) {
											List<CodeableConcept> priorities = priorityMap.get(pinnedVersionKey);
											priorities.add(context.getValueCodeableConcept());
										} else {
											priorityMap.put(pinnedVersionKey, new ArrayList<>(Collections.singletonList(context.getValueCodeableConcept())));
										}
									}
								}
							});

							if (valueSet.getExtensionByUrl(TransformProperties.authoritativeSourceExtUrl) == null) {
								Extension ext = new Extension();
								ext.setUrl(TransformProperties.authoritativeSourceExtUrl);
								ext.setValue(new UriType(TransformProperties.vsacUrl));
								valueSet.getExtension().add(ext);
							}
						}

						// Remove conditions and priority from useContext of leaf valuesets and groupers
						List<UsageContext> cleanedContext = valueSet
							.getUseContext()
							.stream()
							.filter(ctx -> ctx.hasCode() && !(ctx.getCode().getCode().equals("focus") || ctx.getCode().getCode().equals("priority")))
							.collect(Collectors.toList());
						valueSet.setUseContext(cleanedContext);

						// Check if ValueSet already exists
						if (!doesResourceExist(valueSet.getUrl(), valueSet.getVersion(), ValueSet.class, transformProperties)) {
							// Save the resource into entry bundle
							bundleEntries.add(getPutResourceRequest(valueSet, "/ValueSet", valueSet.getIdPart()));
						}
						break;
					case Library:
						Library library = (Library) resource;
						if (doesResourceExist(library.getUrl(), library.getVersion(), Library.class, transformProperties)) {
							throw new FhirResourceExists("Library", library.getUrl(), library.getVersion());
						} else {
							if (isRootSpecificationLibrary(resource)) {
								rootLibrary = library;
							} else {
								rctcLibrary = library;
							}
						}
						break;
					case PlanDefinition:
						planDefinition = (PlanDefinition) resource;
						break;
					default:
						myLogger.info("resourceType:  " + resource.getResourceType() + " is not supported by $import operation");
						break;
				}
			}
		}

		assert rctcLibrary != null;
		assert planDefinition != null;
		assert rootLibrary != null;

		prepareRootLibrary(
			conditionsMap,
			priorityMap,
			planDefinition,
			rctcLibrary,
			groupers,
			rootLibrary
		);

		bundleEntries.add(getPutResourceRequest(rootLibrary, "/Library", rootLibrary.getIdPart()));
		bundleEntries.add(getPutResourceRequest(rctcLibrary, "/Library", rctcLibrary.getIdPart()));
		bundleEntries.add(getPutResourceRequest(planDefinition, "/PlanDefinition", planDefinition.getIdPart()));
		return bundleEntries;
	}

	private static boolean doesResourceExist(String url, String version, Class resource, TransformProperties transformProperties) {
		try {
			SearchParameterMap sp = new SearchParameterMap();
			sp.add("url", new UriParam(url));
			sp.add("version", new TokenParam(version));
			IBundleProvider results = transformProperties.search(resource, sp);
			return !results.isEmpty();
		} catch(Exception e) {
			return false;
		}
	}


	private static Bundle.BundleEntryComponent getPutResourceRequest(MetadataResource value, String resourceType, String id) {
		Bundle.BundleEntryComponent bundleEntry = new Bundle.BundleEntryComponent();

		Bundle.BundleEntryRequestComponent bundleRequest = new Bundle.BundleEntryRequestComponent();
		bundleRequest.setMethod(Bundle.HTTPVerb.PUT);
		bundleRequest.setUrl(resourceType + "?_id=" + id);
		bundleEntry.setRequest(bundleRequest);
		bundleEntry.setResource(value);
		bundleEntry.setFullUrl(value.getUrl());
		return bundleEntry;
	}

	private static List<CanonicalType> addMetaProfileUrl(Meta meta, List<String> urls) {
		List<CanonicalType> profiles = meta.getProfile();

		// Add to profile and ensure not duplicated
		List<CanonicalType> finalProfiles = profiles;
		urls.forEach(url -> finalProfiles.add(new CanonicalType(url)));
		profiles = profiles.stream()
			.filter(distinctByKey(CanonicalType::getValueAsString))
			.collect(Collectors.toList());
		return profiles;
	}

	private static void prepareRootLibrary(
		HashMap<String, List<CodeableConcept>> conditionsMap,
		HashMap<String, List<CodeableConcept>> priorityMap,
		PlanDefinition planDefinition,
		Library rctcLibrary,
		List<String> groupers,
		Library rootLibrary
	) {
		// Add to profile and ensure not duplicated
		List<CanonicalType> rootLibraryProfiles = addMetaProfileUrl(rootLibrary.getMeta(), Collections.singletonList(TransformProperties.crmiManifestLibrary));
		rootLibrary.getMeta().setProfile(rootLibraryProfiles);

		List<RelatedArtifact> relatedArtifacts = new ArrayList<>();

		groupers.forEach(grouper -> {
			RelatedArtifact relatedArtifact = new RelatedArtifact();
			relatedArtifact.setType(RelatedArtifact.RelatedArtifactType.COMPOSEDOF);
			relatedArtifact.setResource(grouper);
			relatedArtifacts.add(relatedArtifact);
		});

		// Set PlanDefinition
		String planDefResourceUrl = planDefinition.getVersion() != null ? planDefinition.getUrl() + "|" + planDefinition.getVersion() : planDefinition.getUrl();
		RelatedArtifact relatedArtifactPlanDefComposedOf = new RelatedArtifact();
		relatedArtifactPlanDefComposedOf.setType(RelatedArtifact.RelatedArtifactType.COMPOSEDOF);
		relatedArtifactPlanDefComposedOf.setResource(planDefResourceUrl);
		Extension extension = new Extension();
		extension.setUrl(TransformProperties.crmiIsOwned);

		extension.setValue( new BooleanType(true));
		relatedArtifactPlanDefComposedOf.setExtension(new ArrayList<>(Collections.singletonList(extension)));
		relatedArtifacts.add(relatedArtifactPlanDefComposedOf);

		RelatedArtifact relatedArtifactPlanDefDependsOn = new RelatedArtifact();
		relatedArtifactPlanDefDependsOn.setType(RelatedArtifact.RelatedArtifactType.DEPENDSON);
		relatedArtifactPlanDefDependsOn.setResource(planDefResourceUrl);
		relatedArtifacts.add(relatedArtifactPlanDefDependsOn);

		// Set rctc Library
		String rctcUrl = rctcLibrary.getVersion() != null ? rctcLibrary.getUrl() + "|" + rctcLibrary.getVersion() : rctcLibrary.getUrl();
		RelatedArtifact relatedArtifactRCTCComposedOf = new RelatedArtifact();
		relatedArtifactRCTCComposedOf.setType(RelatedArtifact.RelatedArtifactType.COMPOSEDOF);
		relatedArtifactRCTCComposedOf.setResource(rctcUrl);
		extension = new Extension();
		extension.setUrl(TransformProperties.crmiIsOwned);

		extension.setValue( new BooleanType(true));
		relatedArtifactRCTCComposedOf.setExtension(new ArrayList<>(Collections.singletonList(extension)));
		relatedArtifacts.add(relatedArtifactRCTCComposedOf);

		RelatedArtifact relatedArtifactRCTCDependsOn = new RelatedArtifact();
		relatedArtifactRCTCDependsOn.setType(RelatedArtifact.RelatedArtifactType.DEPENDSON);
		relatedArtifactRCTCDependsOn.setResource(rctcUrl);
		relatedArtifacts.add(relatedArtifactRCTCDependsOn);

		processCodeableConceptMapForLibrary(conditionsMap, TransformProperties.vsmCondition, relatedArtifacts);
		processCodeableConceptMapForLibrary(priorityMap, TransformProperties.vsmPriority, relatedArtifacts);
		rootLibrary.setRelatedArtifact(relatedArtifacts);
	}

	private static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
		Set<Object> seen = new HashSet<>();
		return t -> seen.add(keyExtractor.apply(t));
	}

	private static void processCodeableConceptMapForLibrary(HashMap<String, List<CodeableConcept>> targetedMap, String extensionUrl, List<RelatedArtifact> relatedArtifacts) {
		for (Map.Entry<String, List<CodeableConcept>> entry : targetedMap.entrySet()) {
			String k = entry.getKey();
			List<CodeableConcept> v = entry.getValue();
			List<Extension> extensions = new ArrayList<>();
			v.forEach(codeableConcept -> {
				Extension extension = new Extension();
				extension.setUrl(extensionUrl);
				extension.setValue(codeableConcept);
				extensions.add(extension);
			});

			Optional<RelatedArtifact> foundArtifact = relatedArtifacts.stream().filter(i -> i.getResource().equals(k)).findFirst();
			if (foundArtifact.isPresent()) {
				List<Extension> existingExtensions = foundArtifact.get().getExtension();
				existingExtensions.addAll(extensions);
			} else {
				RelatedArtifact relatedArtifact = new RelatedArtifact();
				relatedArtifact.setType(RelatedArtifact.RelatedArtifactType.DEPENDSON);
				relatedArtifact.setResource(k);
				relatedArtifact.setExtension(extensions);

				relatedArtifacts.add(relatedArtifact);
			}
		}
	}
}
