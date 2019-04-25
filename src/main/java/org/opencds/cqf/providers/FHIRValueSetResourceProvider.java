package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.jpa.rp.dstu3.CodeSystemResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.ValueSetResourceProvider;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.UriParam;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.instance.model.api.IBaseResource;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FHIRValueSetResourceProvider extends ValueSetResourceProvider {

    /*
        The purpose of this resource provider overload is to build/update CodeSystems
        as ValueSets are created/updated
     */

    private JpaDataProvider provider;
    public FHIRValueSetResourceProvider(JpaDataProvider provider) {
        this.provider = provider;
    }

    private Map<String, CodeSystem> codeSystems = new HashMap<>();

    @Override
    @Create
    public MethodOutcome create(
            HttpServletRequest theRequest,
            @ResourceParam ValueSet valueSet,
            @ConditionalUrlParam String theConditional,
            RequestDetails theRequestDetails)
    {
        populateCodeSystem(valueSet);
        return super.create(theRequest, valueSet, theConditional, theRequestDetails);
    }

    @Override
    @Update
    public MethodOutcome update(
            HttpServletRequest theRequest,
            @ResourceParam ValueSet valueSet,
            @IdParam IdType theId,
            @ConditionalUrlParam String theConditional,
            RequestDetails theRequestDetails)
    {
        populateCodeSystem(valueSet);
        return super.update(theRequest, valueSet, theId, theConditional, theRequestDetails);
    }

    public void populateCodeSystem(ValueSet valueSet) {
        CodeSystemResourceProvider codeSystemResourceProvider = (CodeSystemResourceProvider) provider.resolveResourceProvider("CodeSystem");
        if (valueSet.hasCompose() && valueSet.getCompose().hasInclude()) {
            for (ValueSet.ConceptSetComponent include : valueSet.getCompose().getInclude()) {
                CodeSystem codeSystem = null;
                boolean updateCodeSystem = false;
                boolean createCodeSystem = false;
                if (include.hasSystem()) {
                    // fetch the CodeSystem associated with the system url
                    IBundleProvider bundleProvider = codeSystemResourceProvider
                            .getDao()
                            .search(
                                    new SearchParameterMap()
                                            .add("url", new UriParam(include.getSystem()))
                            );

                    List<IBaseResource> resources = bundleProvider.getResources(0, 1);
                    if (!resources.isEmpty()) {
                        codeSystem = (CodeSystem) resources.get(0);
                    }
                    // if the CodeSystem doesn't exist, create it
                    else {
                        String id = "CodeSystem-" + Integer.toString(include.getSystem().hashCode());
                        if (codeSystems.containsKey(id)) {
                            codeSystem = codeSystems.get(id);
                        }
                        else {
                            codeSystem = new CodeSystem()
                                    .setUrl(include.getSystem())
                                    .setStatus(Enumerations.PublicationStatus.ACTIVE);
                            codeSystem.setContent(CodeSystem.CodeSystemContentMode.COMPLETE);
                            codeSystem.setId(id);
                            codeSystems.put(id, codeSystem);
                        }
                    }

                    // Go through the codes in the ValueSet and, if the codes are not included in the CodeSystem, add them
                    for (ValueSet.ConceptReferenceComponent concept : include.getConcept()) {
                        if (concept.hasCode()) {
                            boolean isCodeInSystem = false;
                            for (CodeSystem.ConceptDefinitionComponent codeConcept : codeSystem.getConcept()) {
                                if (codeConcept.hasCode() && codeConcept.getCode().equals(concept.getCode())) {
                                    isCodeInSystem = true;
                                    break;
                                }
                            }
                            if (!isCodeInSystem) {
                                codeSystem.addConcept(
                                        new CodeSystem.ConceptDefinitionComponent()
                                                .setCode(concept.getCode())
                                                .setDisplay(concept.getDisplay())
                                );
                                updateCodeSystem = true;
                            }
                        }
                    }
                }
                // update the CodeSystem if missing codes were found
                if (codeSystem != null && updateCodeSystem) {
                    codeSystemResourceProvider.getDao().update(codeSystem);
                }
            }
        }
    }
}
