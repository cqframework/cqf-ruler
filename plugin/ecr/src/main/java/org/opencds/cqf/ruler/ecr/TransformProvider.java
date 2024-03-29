package org.opencds.cqf.ruler.ecr;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.UsageContext;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class TransformProvider implements OperationProvider {
	@Autowired
	TransformProperties transformProperties;

	/**
	 * Implements the $ersd-v2-to-v1-transform operation which transforms an ersd v2
	 * Bundle
	 * into an ersd v1 compatible bundle
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
		if (maybePlanDefinition != null && !(maybePlanDefinition instanceof PlanDefinition)) {
			throw new UnprocessableEntityException("Provided v1 PlanDefinition is not a PlanDefinition resource");
		}
		Bundle v2 = (Bundle) maybeBundle;
		final PlanDefinition v1PlanDefinition = maybePlanDefinition != null ? (PlanDefinition) maybePlanDefinition
				: getV1PlanDefinition(requestDetails);
		removeRootSpecificationLibrary(v2);
		v2.getEntry().stream()
				.forEach(entry -> {
					if (entry.getResource() instanceof MetadataResource) {
						MetadataResource resource = (MetadataResource) entry.getResource();
						checkAndUpdateV2PlanDefinition(entry, v1PlanDefinition);
						updateV2GroupersUseContext(resource, v1PlanDefinition.getIdElement());
						updateV2TriggeringValueSets(resource, v1PlanDefinition.getUrl());
						updateV2TriggeringValueSetLibrary(resource);
						resource.setExperimentalElement(null);
						if (targetVersion != null) {
							resource.setVersion(targetVersion);
						}
					}
				});
		return v2;
	}

	private void updateV2GroupersUseContext(MetadataResource resource, IIdType planDefinitionId) {
		// if resourc is a vs
		if (resource.getResourceType() == ResourceType.ValueSet) {
			ValueSet valueSet = (ValueSet) resource;
			// if vs is a grouper
			if (valueSet.hasCompose()
					&& valueSet.getCompose().getIncludeFirstRep().getValueSet().size() > 0) {
				List<UsageContext> usageContexts = valueSet.getUseContext();
				UsageContext program = usageContexts.stream().filter(
						useContext -> useContext.getCode().getSystem().equals(TransformProperties.hl7UsageContextType)
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
	}

	private void removeRootSpecificationLibrary(Bundle v2) {
		List<BundleEntryComponent> filteredRootLib = v2.getEntry().stream()
				.filter(entry -> entry.hasResource())
				.filter(entry -> !(entry.getResource().hasMeta()
						&& entry.getResource().getMeta().hasProfile(TransformProperties.usPHSpecLibProfile)))
				.collect(Collectors.toList());
		v2.setEntry(filteredRootLib);
	}

	private void checkAndUpdateV2PlanDefinition(BundleEntryComponent entry, PlanDefinition v1PlanDefinition)
			throws UnprocessableEntityException {
		if (entry.getResource().getResourceType() == ResourceType.PlanDefinition
				&& entry.getResource().hasMeta()
				&& entry.getResource().getMeta().getProfile().stream()
						.anyMatch(canonical -> canonical.getValue().contains("/ersd-plandefinition"))) {
			entry.setResource(v1PlanDefinition);
			String url = Optional.ofNullable(v1PlanDefinition.getUrl())
					.orElseThrow(() -> new UnprocessableEntityException("URL missing from PlanDefinition"));
			String version = Optional.ofNullable(v1PlanDefinition.getVersion())
					.orElseThrow(() -> new UnprocessableEntityException("Version missing from PlanDefinition"));
			entry.setFullUrl(url + "|" + version);
		}
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
							&& useContext.getValueCodeableConcept().hasCoding(TransformProperties.usPHUsageContext,
									"triggering"))
							&& !(useContext.getCode().getCode().equals("specification-type")
									&& useContext.getValueCodeableConcept()
											.hasCoding(TransformProperties.usPHUsageContext, "value-set-library")))
					.collect(Collectors.toList());
			resource.setUseContext(filteredUseContexts);
		}
	}

	private void updateV2TriggeringValueSets(MetadataResource resource, String v1PlanDefinitionUrl) {
		if (resource.getResourceType() == ResourceType.ValueSet
				&& resource.hasMeta()
				&& resource.getMeta().hasProfile(TransformProperties.usPHTriggeringVSProfile)) {
			replaceProfile(resource.getMeta(), TransformProperties.usPHTriggeringVSProfile,
					TransformProperties.ersdVSProfile);
			List<UsageContext> filteredUseContexts = resource.getUseContext().stream()
					.filter(useContext -> !(useContext.getCode().getCode().equals("reporting")
							&& useContext.getValueCodeableConcept().hasCoding(TransformProperties.usPHUsageContext,
									"triggering"))
							&& !(useContext.getCode().getCode().equals("priority")
									&& useContext.getValueCodeableConcept()
											.hasCoding(TransformProperties.usPHUsageContext, "routine")))
					.collect(Collectors.toList());
			resource.setUseContext(filteredUseContexts);
		}
	}

	private PlanDefinition getV1PlanDefinition(RequestDetails requestDetails) throws ResourceNotFoundException {
		Optional<PlanDefinition> maybePlanDefinition = Optional.ofNullable(null);
		try {
			PlanDefinition v1PlanDefinition = (PlanDefinition) transformProperties
					.getDaoRegistry()
					.getResourceDao(TransformProperties.v1PlanDefinitionId.getResourceType())
					.read(TransformProperties.v1PlanDefinitionId, requestDetails);
			maybePlanDefinition = Optional.of(v1PlanDefinition);
		} catch (ResourceNotFoundException | ResourceGoneException e) {
			throw new ResourceNotFoundException("Could not find V1 PlanDefinition");
		}
		return maybePlanDefinition.orElseThrow(() -> new ResourceNotFoundException("Could not find V1 PlanDefinition"));
	}
}
