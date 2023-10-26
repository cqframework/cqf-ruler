package com.converter;

import java.util.List;
import java.util.stream.Collectors;

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
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

/**
 * This is an example OperationProvider that returns a simple greeting. This is
 * meant to be a demonstration of how to implement an OperationProvider,
 * and not an actual implementation of anything. It also shows hows to use the
 * {@link Description} and {@link Operation}
 * annotations.
 * <p>
 * When implementing the operations it's important to capture the specific IG
 * the operation is defined in. Additional, release versions should be used
 * whenever possible.
 * Please add both the appropriate Javadoc comments so that implementors have
 * documentation when writing Java code, and also use the {@link Description}
 * annotation so that the relevant information is surfaced via the Tester UI and
 * Swagger UI.
 */
public class ConverterProvider implements OperationProvider {

	@Autowired
	ConverterProperties converterProperties;

	/**
	 * Implements the $hello-world operation found in the
	 * <a href="https://www.hl7.org/fhir/clinicalreasoning-module.html">FHIR CR
	 * Module</a>
	 *
	 * @return a greeting
	 */
	@Description(shortDefinition = "Converts a v2 ERSD bundle into a v1 ERSD bundle", value = "Converts a v2 ERSD bundle into a v1 ERSD bundle")
	@Operation(idempotent = true, name = "$convert-v1")
	public Bundle convert_v1(Bundle v2) {
		if (v2 == null) {
			throw new UnprocessableEntityException("Bundle is missing");
		}
		if (isV2(v2)) { 
			return v2;
		}
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
		PlanDefinition v1PlanDefinition = new PlanDefinition();
		v2.getEntry().stream()
			.forEach(entry -> {
				// V2 has the root specification Library
				if (entry.getResource() instanceof MetadataResource) {
					MetadataResource resource = (MetadataResource) entry.getResource();
					if (resource.getResourceType() == ResourceType.PlanDefinition) {
						entry.setResource(v1PlanDefinition);
					}
					if (resource.getResourceType() == ResourceType.ValueSet
					 && resource.hasMeta() 
					 && resource.getMeta().getProfile().stream().anyMatch(canonical -> canonical.getValue().contains("us-ph-triggering-valueset"))) {
						resource.getUseContext().stream().forEach(useContext -> {
							if (useContext.getCode().getCode().equals("program")) {
								useContext.setValue(new CanonicalType(v1PlanDefinition.getUrl()));
							}
						});
						List<UsageContext> filteredUseContexts = resource.getUseContext().stream()
							.filter(useContext -> !useContext.getCode().getCode().equals("reporting") && !useContext.getCode().getCode().equals("priority"))
							.collect(Collectors.toList());
						resource.setUseContext(filteredUseContexts);
					}
					if (resource.hasMeta() && resource.getMeta().hasProfile()) {
						// need to remove us ph meta profiles
						List<CanonicalType> filteredProfiles = resource.getMeta().getProfile().stream()
							.filter(profile -> !profile.getValueAsString().contains("us-ph")).collect(Collectors.toList());
						resource.getMeta().setProfile(filteredProfiles);
					}
					if (resource.getResourceType() == ResourceType.ValueSet
					 && resource.hasMeta() 
					 && resource.getMeta().getProfile().stream().anyMatch(canonical -> canonical.getValue().contains("us-ph-triggering-valueset-library"))
					) {
						List<UsageContext> filteredUseContexts = resource.getUseContext().stream()
							.filter(useContext -> !useContext.getCode().getCode().equals("reporting") && !useContext.getCode().getCode().equals("specification-type"))
							.collect(Collectors.toList());
						resource.setUseContext(filteredUseContexts);
					}
					resource.setExperimentalElement(null);
				}
			});
		
			return v2;
	}
	private boolean isV2(Bundle maybeV2) {
		return true;
	}
}