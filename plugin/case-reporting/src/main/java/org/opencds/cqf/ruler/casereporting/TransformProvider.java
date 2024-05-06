package org.opencds.cqf.ruler.casereporting;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.UsageContext;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.casereporting.r4.FhirResourceExists;
import org.opencds.cqf.ruler.casereporting.r4.ImportBundleProducer;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TransformProvider implements OperationProvider {
	@Autowired
	TransformProperties transformProperties;

	/**
	 * Implements the $ersd-v2-to-v1-transform operation which transforms an
	 * eCR Version 2.1.1 (http://hl7.org/fhir/us/ecr/ImplementationGuide/hl7.fhir.us.ecr|2.1.1) conformant
	 * eRSD Bundle
	 * into an eCR Version 1.0.0 compatible eRSD bundle
	 *
	 * @param requestDetails      the incoming request details
	 * @param maybeBundle         the v2 bundle to transform
	 * @param maybePlanDefinition the v1 PlanDefinition to include
	 * @return the v1 compatible bundle
	 */
	@Description(shortDefinition = "Converts a v2 ERSD bundle into a v1 ERSD bundle", value = "Converts a v2 ERSD bundle into a v1 ERSD bundle")
	@Operation(idempotent = true, name = "$ersd-v2-to-v1-transform")
	public Bundle convert_v1(
		RequestDetails requestDetails,
		@OperationParam(name = "bundle") IBaseResource maybeBundle,
		@OperationParam(name = "planDefinition") IBaseResource maybePlanDefinition,
		@OperationParam(name = "targetVersion") String targetVersion) throws UnprocessableEntityException {
		if (maybeBundle == null) {
			throw new UnprocessableEntityException("Resource is missing");
		}
		if (!(maybeBundle instanceof IBaseBundle)) {
			throw new UnprocessableEntityException("Resource is not a bundle");
		}
		var v2Bundle = (Bundle) maybeBundle;
		var planDefinition = getPlanDefinition(v2Bundle, maybePlanDefinition);

		removeRootSpecificationLibrary(v2Bundle);
		for (final var entry: v2Bundle.getEntry()) {
			if (entry.getResource() instanceof MetadataResource) {
				var currentResource = (MetadataResource) entry.getResource();

				if (isErsdPlanDefinition(currentResource) && planDefinition != null) {
					updatePlanDefinition(entry, planDefinition);
				}

				updateV2GroupersUseContext(currentResource, planDefinition.getIdElement());
				updateV2TriggeringValueSets(currentResource);
				updateV2TriggeringValueSetLibrary(currentResource);
				currentResource.setExperimentalElement(null);
				if (targetVersion != null) {
					currentResource.setVersion(targetVersion);
				}
			}
		}
		return v2Bundle;
	}
	
	private boolean isPlanDefinitionAndConformsToProfile(Resource resource, String profileUrl) {
		return resource.getResourceType() == ResourceType.PlanDefinition
			&& resource.hasMeta()
			&& resource.getMeta().hasProfile()
			&& resource.getMeta().getProfile().stream().anyMatch(canonical -> canonical.getValue().equalsIgnoreCase(profileUrl));
	}
	private PlanDefinition getPlanDefinition(Bundle bundle, IBaseResource maybePlanDefinition) throws UnprocessableEntityException {
		if (maybePlanDefinition == null) {
			return getErsdPlanDefinition(bundle);
		} else {
			if (!(maybePlanDefinition instanceof PlanDefinition)) {
				throw new UnprocessableEntityException("Provided v1 PlanDefinition is not a PlanDefinition resource");
			}
			return (PlanDefinition) maybePlanDefinition;
		}
	}

	/**
	 * Implements the $ersd-v2-import operation which loads an active (released)
	 * eCR Version 2.1.1 (http://hl7.org/fhir/us/ecr/ImplementationGuide/hl7.fhir.us.ecr|2.1.1) conformant
	 * eRSD Bundle
	 * and transforms it into and Value Set Manager authoring state
	 *
	 * @param requestDetails      the incoming request details
	 * @param maybeBundle         the v2 bundle to import
	 * @return the OperationOutcome
	 */
	@Description(shortDefinition = "Imports a v2 ERSD bundle", value = "Imports a v2 ERSD bundle")
	@Operation(idempotent = true, name = "$ersd-v2-import")
	public OperationOutcome importReportSpec(
		RequestDetails requestDetails,
		@OperationParam(name = "bundle") IBaseResource maybeBundle) throws UnprocessableEntityException, FhirResourceExists {
		if (maybeBundle == null) {
			throw new UnprocessableEntityException("Resource is missing");
		}
		if (!(maybeBundle instanceof IBaseBundle)) {
			throw new UnprocessableEntityException("Resource is not a bundle");
		}
		Bundle v2Bundle = (Bundle) maybeBundle;
		List<Bundle.BundleEntryComponent> importTxBundleEntries = ImportBundleProducer.transformImportBundle(v2Bundle, transformProperties);

		new Thread(() -> {
			executeImportTransactionBundle(importTxBundleEntries);
		}).start();
		OperationOutcome response = new OperationOutcome();
		OperationOutcome.OperationOutcomeIssueComponent issue = new OperationOutcome.OperationOutcomeIssueComponent();
		issue.setSeverity(OperationOutcome.IssueSeverity.INFORMATION);
		issue.setCode(OperationOutcome.IssueType.PROCESSING);

		response.addIssue(issue);
		return response;
	}

	private Bundle executeImportTransactionBundle(List<BundleEntryComponent> bundleEntry) {
		Bundle importBundle = new Bundle();
		importBundle.setType(Bundle.BundleType.TRANSACTION);
		importBundle.setEntry(bundleEntry);
		return transformProperties.transaction(importBundle);
	}

	private void updateV2GroupersUseContext(MetadataResource resource, IIdType planDefinitionId) {
		// if resource is a ValueSet
		if (ImportBundleProducer.isGrouper(resource)) {
			ValueSet valueSet = (ValueSet) resource;
			// if ValueSet is a grouper
			List<UsageContext> usageContexts = valueSet.getUseContext();
			UsageContext program = usageContexts.stream()
				.filter(useContext -> useContext.getCode().getSystem().equals(TransformProperties.hl7UsageContextType)
					&& useContext.getCode().getCode().equals("program"))
				.findFirst().orElseGet(() -> {
					UsageContext retval = new UsageContext(
						new Coding(TransformProperties.hl7UsageContextType, "program", null), null);
					usageContexts.add(retval);
					return retval;
				});
			program.setValue(new Reference(planDefinitionId));
		}
	}

	private void removeRootSpecificationLibrary(Bundle v2) {
		List<BundleEntryComponent> filteredRootLib = v2.getEntry().stream()
			.filter(entry -> entry.hasResource())
			.filter(entry -> !ImportBundleProducer.isRootSpecificationLibrary(entry.getResource()))
			.collect(Collectors.toList());
		v2.setEntry(filteredRootLib);
	}

	private void updatePlanDefinition(BundleEntryComponent entry, PlanDefinition v1PlanDefinition) throws UnprocessableEntityException{
			entry.setResource(v1PlanDefinition);
			String url = Optional.ofNullable(v1PlanDefinition.getUrl())
				.orElseThrow(() -> new UnprocessableEntityException("URL missing from PlanDefinition"));
			String version = Optional.ofNullable(v1PlanDefinition.getVersion())
				.orElseThrow(() -> new UnprocessableEntityException("Version missing from PlanDefinition"));
			entry.setFullUrl(url + "|" + version);
	}

	/**
	 * Remove all instances of an old profile and add one instance of a new profile
	 *
	 * @param meta       the meta object to update
	 * @param oldProfile the profile URL to remove
	 * @param newProfile the profile URL to add
	 */
	private void replaceProfile(Meta meta, String oldProfile, String newProfile) {
		// remove all instances of old profile
		List<CanonicalType> updatedProfiles = meta.getProfile().stream()
			.filter(profile -> !profile.getValue().equals(oldProfile)).collect(Collectors.toList());
		// add the new profile if it doesn't exist
		if (!updatedProfiles.stream().anyMatch(profile -> profile.getValue().equals(newProfile))) {
			updatedProfiles.add(new CanonicalType(newProfile));
		}
		meta.setProfile(updatedProfiles);
	}

	private void updateV2TriggeringValueSetLibrary(MetadataResource resource) {
		if (resource.getResourceType() == ResourceType.Library
			&& resource.hasMeta()
			&& resource.getMeta().hasProfile(TransformProperties.usPHTriggeringVSLibProfile)) {
			replaceProfile(resource.getMeta(), TransformProperties.usPHTriggeringVSLibProfile,
				TransformProperties.ersdVSLibProfile);
			List<UsageContext> filteredUseContexts = resource.getUseContext().stream()
				.filter(useContext -> !(useContext.getCode().getCode().equals("reporting")
					&& useContext.getValueCodeableConcept().hasCoding(TransformProperties.usPHUsageContext, "triggering"))
					&& !(useContext.getCode().getCode().equals("specification-type")
					&& useContext.getValueCodeableConcept().hasCoding(TransformProperties.usPHUsageContext,
					"value-set-library")))
				.collect(Collectors.toList());
			resource.setUseContext(filteredUseContexts);
		}
	}

	private void updateV2TriggeringValueSets(MetadataResource resource) {
		if (resource.getResourceType() == ResourceType.ValueSet
			&& resource.hasMeta()
			&& resource.getMeta().hasProfile(TransformProperties.usPHTriggeringVSProfile)) {
			replaceProfile(resource.getMeta(), TransformProperties.usPHTriggeringVSProfile,
				TransformProperties.ersdVSProfile);
			List<UsageContext> filteredUseContexts = resource.getUseContext().stream()
				.filter(useContext -> !(useContext.getCode().getCode().equals("reporting")
					&& useContext.getValueCodeableConcept().hasCoding(TransformProperties.usPHUsageContext, "triggering"))
					&& !(useContext.getCode().getCode().equals("priority")
					&& useContext.getValueCodeableConcept().hasCoding(TransformProperties.usPHUsageContext, "routine")))
				.collect(Collectors.toList());
			resource.setUseContext(filteredUseContexts);
		}
	}
	private boolean isErsdPlanDefinition(Resource resource) {
		// V2 PlanDefinitions have both profiles : http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-plandefinition and http://hl7.org/fhir/us/ecr/StructureDefinition/ersd-plandefinition
		// V1 PlanDefinitions only have profile : http://hl7.org/fhir/us/ecr/StructureDefinition/ersd-plandefinition
		return isPlanDefinitionAndConformsToProfile(resource, TransformProperties.usPHPlanDefinitionProfile) 
				|| isPlanDefinitionAndConformsToProfile(resource, TransformProperties.ersdPlanDefinitionProfile);
	}
	private PlanDefinition getErsdPlanDefinition(Bundle bundle) throws UnprocessableEntityException {
		var planDefinitions = bundle.getEntry().stream()
				.map(BundleEntryComponent::getResource)
				.filter(resource -> isErsdPlanDefinition(resource))
				.map(resource -> (PlanDefinition) resource)
				.collect(Collectors.toList());

		if (planDefinitions.isEmpty()) {
			throw new UnprocessableEntityException("No eRSD V1 or V2 PlanDefinition found in the source Bundle, and no PlanDefinition was provided.");
		} else if (planDefinitions.size() > 1) {
			throw new UnprocessableEntityException("More than one eRSD PlanDefinition found in the source Bundle.");
		}

		return planDefinitions.get(0);
	}
}