package org.opencds.cqf.r4.providers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.r4.builders.OperationOutcomeBuilder;
import org.opencds.cqf.r4.builders.RandomIdBuilder;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.UriParam;

@Component
public class CodeSystemUpdateProvider {
    private IFhirResourceDao<ValueSet> valueSetDao;
    private IFhirResourceDao<CodeSystem> codeSystemDao;

    @Inject
    public CodeSystemUpdateProvider(IFhirResourceDao<ValueSet> valueSetDao,
            IFhirResourceDao<CodeSystem> codeSystemDao) {
        this.valueSetDao = valueSetDao;
        this.codeSystemDao = codeSystemDao;
    }

    /***
     * Update existing CodeSystems with the codes in all ValueSet resources. System
     * level CodeSystem update operation
     *
     * @return FHIR OperationOutcome detailing the success or failure of the
     *         operation
     */
    @Operation(name = "$updateCodeSystems", idempotent = true)
    public OperationOutcome updateCodeSystems() {
        IBundleProvider valuesets = this.valueSetDao.search(new SearchParameterMap());

        List<ValueSet> valueSets =  valuesets.getResources(0, valuesets.size()).stream().map(x -> (ValueSet)x).collect(Collectors.toList());

        OperationOutcome outcome = this.performCodeSystemUpdate(valueSets);

        OperationOutcome response = new OperationOutcome();
        if (outcome.hasIssue()) {
            for (OperationOutcome.OperationOutcomeIssueComponent issue : outcome.getIssue()) {
                response.addIssue(issue);
            }
        }

        return response;
    }

    /***
     * Update existing CodeSystems with the codes in the specified ValueSet.
     *
     * This is for development environment purposes to enable ValueSet expansion and
     * validation without complete CodeSystems.
     *
     * @param theId the id of the ValueSet
     * @return FHIR OperationOutcome detailing the success or failure of the
     *         operation
     */
    @Operation(name = "$updateCodeSystems", idempotent = true, type = ValueSet.class)
    public OperationOutcome updateCodeSystems(@IdParam IdType theId) {
        ValueSet vs = this.valueSetDao.read(theId);

        OperationOutcomeBuilder responseBuilder = new OperationOutcomeBuilder();
        if (vs == null) {
            return responseBuilder.buildIssue("error", "notfound", "Unable to find Resource: " + theId.getId()).build();
        }

        return performCodeSystemUpdate(Collections.singletonList(vs));
    }

    public OperationOutcome performCodeSystemUpdate(List<ValueSet> valueSets) {
        OperationOutcomeBuilder responseBuilder = new OperationOutcomeBuilder();

        List<String> codeSystems = new ArrayList<>();

        // Possible for this to run out of memory with really large ValueSets and CodeSystems.
        Map<String, Set<String>> codesBySystem = new HashMap<>();
        for (ValueSet vs : valueSets){

        if (vs.hasCompose() && vs.getCompose().hasInclude()) {
            for (ValueSet.ConceptSetComponent csc : vs.getCompose().getInclude()) {
                if (!csc.hasSystem() || !csc.hasConcept()){
                    continue;
                }

                String system = csc.getSystem();
                if (!codesBySystem.containsKey(system)){
                    codesBySystem.put(system, new HashSet<>());
                }

                Set<String> codes = codesBySystem.get(system);
                
                codes.addAll(csc.getConcept().stream().map(ValueSet.ConceptReferenceComponent::getCode)
                .collect(Collectors.toList()));
            }
        }

        }

        for(Map.Entry<String, Set<String>> entry : codesBySystem.entrySet()) {
            String system = entry.getKey();
            CodeSystem codeSystem = getCodeSystemByUrl(system);
            updateCodeSystem(codeSystem.setUrl(system), getUnionDistinctCodes(entry.getValue(), codeSystem));

            codeSystems.add(codeSystem.getUrl());

        }

        if (codeSystems.size() > 0) {
            return responseBuilder.buildIssue("information", "informational",
            "Successfully updated the following CodeSystems: " + String.join(", ", codeSystems)).build();
        }
        else {
            return responseBuilder.buildIssue("information", "informational",
            "No code systems were updated").build();
        }
    }

    /***
     * Fetch CodeSystem matching the given url search parameter
     *
     * @param url The url of the CodeSystem to fetch
     * @return The CodeSystem that matches the url parameter or a new CodeSystem
     *         with the url and id populated
     */
    private CodeSystem getCodeSystemByUrl(String url) {
        IBundleProvider bundleProvider = this.codeSystemDao
                .search(new SearchParameterMap().add(CodeSystem.SP_URL, new UriParam(url)));

        if (bundleProvider.size() >= 1) {
            return (CodeSystem) bundleProvider.getResources(0, 1).get(0);
        }

        return (CodeSystem) new CodeSystem().setUrl(url).setId(RandomIdBuilder.build(null));
    }

    /***
     * Perform union of codes within a ValueSet and CodeSystem
     *
     * @param valueSetCodes The codes contained within a ValueSet
     * @param codeSystem    A CodeSystem resource
     * @return List of distinct codes strings
     */
    private Set<String> getUnionDistinctCodes(Set<String> valueSetCodes, CodeSystem codeSystem) {
        if (!codeSystem.hasConcept()) {
            return valueSetCodes;
        }

        valueSetCodes.addAll(codeSystem.getConcept().stream().map(CodeSystem.ConceptDefinitionComponent::getCode)
            .collect(Collectors.toSet()));

        return valueSetCodes;
    }

    /***
     * Overwrite the given CodeSystem codes with the given codes
     *
     * @param codeSystem A CodeSystem resource
     * @param codes      List of (unique) code strings
     */
    private void updateCodeSystem(CodeSystem codeSystem, Set<String> codes) {
        codeSystem
                .setConcept(codes.stream().map(x -> new CodeSystem.ConceptDefinitionComponent().setCode(x))
                        .collect(Collectors.toList()))
                .setContent(CodeSystem.CodeSystemContentMode.COMPLETE).setStatus(Enumerations.PublicationStatus.ACTIVE);

        this.codeSystemDao.update(codeSystem);
    }

}
