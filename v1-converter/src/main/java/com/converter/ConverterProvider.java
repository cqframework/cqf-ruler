package com.converter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.UsageContext;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class ConverterProvider implements OperationProvider {
	@Autowired
	ConverterProperties converterProperties;

	/**
	 * Implements the $convert-v1 operation which transforms an ersd v2 Bundle
	 * into an ersd v1 compatible bundle
	 * @return a greeting
	 */
	@Description(shortDefinition = "Converts a v2 ERSD bundle into a v1 ERSD bundle", value = "Converts a v2 ERSD bundle into a v1 ERSD bundle")
	@Operation(idempotent = true, name = "$convert-v1")
	public Bundle convert_v1(
		RequestDetails requestDetails,
		@OperationParam(name = "resource") IBaseResource maybeBundle) throws UnprocessableEntityException {
		if (maybeBundle == null) {
			throw new UnprocessableEntityException("Resource is missing");
		}
		if (!(maybeBundle instanceof IBaseBundle )) {
			throw new UnprocessableEntityException("Resource is not a bundle");
		}
		Bundle v2 = (Bundle) maybeBundle;
		removeRootSpecificationLibrary(v2);
		final PlanDefinition v1PlanDefinition = getV1PlanDefinition(requestDetails);
		v2.getEntry().stream()
			.forEach(entry -> {
				if (entry.getResource() instanceof MetadataResource) {
					MetadataResource resource = (MetadataResource) entry.getResource();
					checkAndUpdateV2PlanDefinition(entry, v1PlanDefinition);
					updateV2TriggeringValueSets(resource, v1PlanDefinition.getUrl());
					updateV2TriggeringValueSetLibrary(resource);
					removeUSPHProfiles(resource);
					resource.setExperimentalElement(null);
				}
			});
		return v2;
	}
	private void removeRootSpecificationLibrary(Bundle v2) {
		List<BundleEntryComponent> filteredRootLib = v2.getEntry().stream()
			.filter(entry -> entry.hasResource())
			.filter(entry -> {
				if (entry.getResource().hasMeta() 
				 && entry.getResource().getMeta().getProfile().stream().anyMatch(canonical -> canonical.getValue().contains("us-ph-specification-library"))) {
					return false;
				 } else {
					return true;
				 }
			}).collect(Collectors.toList());
		v2.setEntry(filteredRootLib);
	}
	private void checkAndUpdateV2PlanDefinition(BundleEntryComponent entry, PlanDefinition v1PlanDefinition) {
		if (entry.getResource().getResourceType() == ResourceType.PlanDefinition
			&& entry.getResource().hasMeta()
			&& entry.getResource().getMeta().getProfile().stream().anyMatch(canonical -> canonical.getValue().contains("/ersd-plandefinition"))) {
			entry.setResource(v1PlanDefinition);
		}
	}
	private void updateV2TriggeringValueSetLibrary(MetadataResource resource) {
		if (resource.getResourceType() == ResourceType.ValueSet
			&& resource.hasMeta() 
			&& resource.getMeta().getProfile().stream().anyMatch(canonical -> canonical.getValue().contains("us-ph-triggering-valueset-library"))
		) {
			List<UsageContext> filteredUseContexts = resource.getUseContext().stream()
				.filter(useContext -> !useContext.getCode().getCode().equals("reporting") && !useContext.getCode().getCode().equals("specification-type"))
				.collect(Collectors.toList());
			resource.setUseContext(filteredUseContexts);
		}
	}
	private void updateV2TriggeringValueSets(MetadataResource resource, String v1PlanDefinitionUrl) {
		if (resource.getResourceType() == ResourceType.ValueSet
		 && resource.hasMeta() 
		 && resource.getMeta().getProfile().stream().anyMatch(canonical -> canonical.getValue().contains("us-ph-triggering-valueset"))) {
			resource.getUseContext().stream().forEach(useContext -> {
				if (useContext.getCode().getCode().equals("program")) {
					useContext.setValue(new CanonicalType(v1PlanDefinitionUrl));
				}
			});
			List<UsageContext> filteredUseContexts = resource.getUseContext().stream()
				.filter(useContext -> !useContext.getCode().getCode().equals("reporting") && !useContext.getCode().getCode().equals("priority"))
				.collect(Collectors.toList());
			resource.setUseContext(filteredUseContexts);
		}
	}
	private void removeUSPHProfiles(MetadataResource resource){
		if (resource.hasMeta() && resource.getMeta().hasProfile()) {
			// need to remove us ph meta profiles
			List<CanonicalType> filteredProfiles = resource.getMeta().getProfile().stream()
				.filter(profile -> !profile.getValueAsString().contains("us-ph")).collect(Collectors.toList());
			resource.getMeta().setProfile(filteredProfiles);
		}
	}
	private PlanDefinition getV1PlanDefinition(RequestDetails requestDetails) throws ResourceNotFoundException {
		Optional<PlanDefinition> maybePlanDefinition = Optional.ofNullable(null);
		try {
			PlanDefinition v1PlanDefinition = (PlanDefinition) converterProperties
				.getDaoRegistry()
				.getResourceDao(converterProperties.v1PlanDefinitionId.getResourceType())
				.read(converterProperties.v1PlanDefinitionId, requestDetails);	
			maybePlanDefinition = Optional.of(v1PlanDefinition);
		} catch (ResourceNotFoundException | ResourceGoneException e) {
			throw new ResourceNotFoundException("Could not find V1 PlanDefinition");
		}
		return maybePlanDefinition.orElseThrow(() -> new ResourceNotFoundException("Could not find V1 PlanDefinition"));
	}
}