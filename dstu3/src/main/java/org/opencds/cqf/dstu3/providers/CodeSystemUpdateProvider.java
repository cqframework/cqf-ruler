package org.opencds.cqf.dstu3.providers;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class CodeSystemUpdateProvider
{
    private FHIRValueSetResourceProvider provider;

    public CodeSystemUpdateProvider(FHIRValueSetResourceProvider provider)
    {
        this.provider = provider;
    }

    /***
     * Update existing CodeSystems with the codes in all ValueSet resources.
     * System level CodeSystem update operation
     *
     * @return FHIR OperationOutcome detailing the success or failure of the operation
     */
    @Operation(name = "$updateCodeSystems", idempotent = true)
    public OperationOutcome updateCodeSystems()
    {
        IBundleProvider valuesets = provider.getDao().search(new SearchParameterMap());
        OperationOutcome response = new OperationOutcome();

        OperationOutcome outcome;
        for (IBaseResource valueSet : valuesets.getResources(0, valuesets.size()))
        {
            outcome = provider.performCodeSystemUpdate((ValueSet) valueSet);
            if (outcome.hasIssue())
            {
                for (OperationOutcome.OperationOutcomeIssueComponent issue : outcome.getIssue())
                {
                    response.addIssue(issue);
                }
            }
        }

        return response;
    }

}
