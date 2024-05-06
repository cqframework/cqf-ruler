package org.opencds.cqf.ruler.casereporting.r4;

import ca.uhn.fhir.cr.common.IRepositoryFactory;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptReferenceComponent;
import org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent;
import org.opencds.cqf.fhir.utility.iterable.BundleMappingIterable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ValueSetSynonymUpdateInterceptor {
	private final String synonymUrl;
	private final IRepositoryFactory repositoryFactory;

	public ValueSetSynonymUpdateInterceptor(String synonymUrl, IRepositoryFactory repositoryFactory) {
		this.synonymUrl = synonymUrl;
		this.repositoryFactory = repositoryFactory;
	}

	/**
	 * Handle updates
	 */
	@Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED)
	public void update(
		IBaseResource theOldResource,
		IBaseResource theResource,
		RequestDetails theRequestDetails) throws UnprocessableEntityException {
		if (theResource.fhirType().equals(ResourceType.ValueSet.toString())) {
			ValueSet vs = (ValueSet) theResource;
			if (vs.getUrl().equals(this.synonymUrl)) {
				var repository = repositoryFactory.create(theRequestDetails);
				List<ConceptSetComponent> sets = vs.getCompose().getInclude();
				List<Library> librariesToUpdate = new ArrayList<Library>();
				for (ConceptSetComponent set : sets) {
					// for each system in the value set
					String system = set.getSystem();
					List<ConceptReferenceComponent> concepts = set.getConcept();
					for (ConceptReferenceComponent concept : concepts) {
						// for each concept.code need to update the extension.valueCodeableConcept.coding.display to concept.designation(where code = synonym).value
						TokenParam token = new TokenParam(system, concept.getCode());
						Map<String, List<IQueryParameterType>> paramMap = new HashMap<>();
						paramMap.put("manifest-contains-code", Arrays.asList(token));
						var resources = repository.search(Bundle.class, Library.class, paramMap);
						var iter = new BundleMappingIterable<>(repository, resources, p -> (Library) p.getResource());
						iter.toStream().forEach(lib -> {
							Library getCachedLibraryOrAdd = librariesToUpdate
								.stream().filter(existing -> existing.getId().equals(lib.getId())).findFirst()
								.orElseGet(() -> {
									librariesToUpdate.add(lib);
									return lib;
								});
							String newText = concept.getDesignation()
								.stream().filter(designation -> designation.getUse().getCode().equals("synonym"))
								.findFirst().orElseThrow(() -> new UnprocessableEntityException("No synonym found for code: " + system + "|" + concept.getCode()))
								.getValue();
							updateConditionWithNewSynonym(getCachedLibraryOrAdd, system, concept.getCode(), newText);
						});
					}
				}
				for (Library library : librariesToUpdate) {
					repository.update(library);
				}
			}
		}
	}

	private void updateConditionWithNewSynonym(Library library, String system, String code, String newText) {
		List<RelatedArtifact> relatedArtifacts = library.getRelatedArtifact();
		relatedArtifacts.stream()
			.map(ra -> ra.getExtensionsByUrl(KnowledgeArtifactProcessor.valueSetConditionUrl))
			.flatMap(exts -> exts.stream())
			.filter(ext -> ext.hasValue())
			.map(ext -> (CodeableConcept) ext.getValue())
			.filter(concept -> concept.getCoding().stream().anyMatch(coding -> coding.getSystem().equals(system) && coding.getCode().equals(code)))
			.forEach(concept -> {
				concept.setText(newText);
			});
	}
}
