package org.opencds.cqf.ruler.plugin.devtools.dstu3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.plugin.utility.IdUtilities;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDaoCodeSystem;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDaoValueSet;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.UriParam;

/**
 * This class provides an {@link OperationProvider OperationProvider} implementation
 * to enable {@link ValueSet ValueSet} expansion and validation without complete {@link CodeSystem CodeSystems}.
 */
public class CodeSystemUpdateProvider implements OperationProvider, IdUtilities {
    @Autowired
    private IFhirResourceDaoValueSet<ValueSet, Coding, CodeableConcept> myValueSetDaoDSTU3;
    @Autowired
    private IFhirResourceDaoCodeSystem<CodeSystem, Coding, CodeableConcept> myCodeSystemDaoDSTU3;

    /***
     * Update existing {@link CodeSystem CodeSystems} with the codes in all {@link ValueSet ValueSet} resources.
     * System level CodeSystem update operation
     *
     * @return FHIR {@link OperationOutcome OperationOutcome} detailing the success or failure of the
     *         operation
     */
    @Description(
        shortDefinition = "$updateCodeSystems",
        value = "Update existing CodeSystems with the codes in all ValueSet resources. System level CodeSystem update operation",
        example = "$updateCodeSystems"
    )
    @Operation(name = "$updateCodeSystems", idempotent = true)
    public OperationOutcome updateCodeSystems() {
        IBundleProvider valuesets = this.myValueSetDaoDSTU3.search(SearchParameterMap.newSynchronous());

        List<ValueSet> valueSets =  valuesets.getAllResources().stream().map(x -> (ValueSet)x).collect(Collectors.toList());

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
     * Update existing {@link CodeSystem CodeSystems} with the codes in the specified {@link ValueSet ValueSet}.
     *
     * This is for development environment purposes to enable ValueSet expansion and
     * validation without complete CodeSystems.
     *
     * @param theId the id of the {@link ValueSet ValueSet}
     * @return FHIR {@link OperationOutcome OperationOutcome} detailing the success or failure of the
     *         operation
     */
    @Description(
        shortDefinition = "$updateCodeSystems",
        value = "Update existing CodeSystems with the codes in the specified ValueSet This is for development environment purposes to enable ValueSet expansion and validation without complete CodeSystems.",
        example = "ValueSet/example-id/$updateCodeSystems"
    )
    @Operation(name = "$updateCodeSystems", idempotent = true, type = ValueSet.class)
    public OperationOutcome updateCodeSystems(@IdParam IdType theId) {
        OperationOutcome response = new OperationOutcome();
        ValueSet vs = null;
        try {
            vs = this.myValueSetDaoDSTU3.read(theId);
            if (vs == null) {
                return buildIssue(response, "error", "not-found", "Unable to find Resource: " + theId.getIdPart());
            }  
        } catch (Exception e) {
            return buildIssue(response, "error", "not-found", "Unable to find Resource: " + theId.getIdPart() + "\n" + e);
        }

        return performCodeSystemUpdate(Collections.singletonList(vs));
    }

    public OperationOutcome performCodeSystemUpdate(List<ValueSet> valueSets) {
        OperationOutcome response = new OperationOutcome();

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
            return buildIssue(response, "information", "informational",
            "Successfully updated the following CodeSystems: " + String.join(", ", codeSystems));
        }
        else {
            return buildIssue(response, "information", "informational",
            "No code systems were updated");
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
        IBundleProvider bundleProvider = this.myCodeSystemDaoDSTU3
                .search(SearchParameterMap.newSynchronous().add(CodeSystem.SP_URL, new UriParam(url)));

        if (bundleProvider.size() >= 1) {
            return (CodeSystem) bundleProvider.getResources(0, 1).get(0);
        }

        return (CodeSystem) new CodeSystem().setUrl(url).setId(this.createId(CodeSystem.class, UUID.randomUUID().toString()));
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

        this.myCodeSystemDaoDSTU3.update(codeSystem);
    }
    
    private OperationOutcome buildIssue(OperationOutcome outcome, String severity, String code, String details) {
        outcome.addIssue(new OperationOutcome.OperationOutcomeIssueComponent()
                .setSeverity(OperationOutcome.IssueSeverity.fromCode(severity))
                .setCode(OperationOutcome.IssueType.fromCode(code)).setDetails(new CodeableConcept().setText(details)));

        return outcome;
    }

}
