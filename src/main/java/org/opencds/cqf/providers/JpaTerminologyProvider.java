package org.opencds.cqf.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.term.IHapiTerminologySvcDstu3;
import ca.uhn.fhir.jpa.term.VersionIndependentConcept;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.terminology.CodeSystemInfo;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.cql.terminology.ValueSetInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Christopher Schuler on 7/17/2017.
 */
public class JpaTerminologyProvider implements TerminologyProvider {

    private IHapiTerminologySvcDstu3 terminologySvcDstu3;
    private FhirContext context;

    public JpaTerminologyProvider(IHapiTerminologySvcDstu3 terminologySvcDstu3, FhirContext context) {
        this.terminologySvcDstu3 = terminologySvcDstu3;
        this.context = context;
    }

    @Override
    public boolean in(Code code, ValueSetInfo valueSet) throws ResourceNotFoundException {
        for (Code c : expand(valueSet)) {
            if (c == null) continue;
            if (c.getCode().equals(code.getCode()) && c.getSystem().equals(code.getSystem())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterable<Code> expand(ValueSetInfo valueSet) throws ResourceNotFoundException {
        List<Code> codes = new ArrayList<>();
        List<VersionIndependentConcept> expansion = terminologySvcDstu3.expandValueSet(valueSet.getId());
        for (VersionIndependentConcept concept : expansion) {
            codes.add(new Code().withCode(concept.getCode()).withSystem(concept.getSystem()));
        }

        return codes;
    }

    @Override
    public Code lookup(Code code, CodeSystemInfo codeSystem) throws ResourceNotFoundException {
        CodeSystem cs = terminologySvcDstu3.fetchCodeSystem(context, codeSystem.getId());
        for (CodeSystem.ConceptDefinitionComponent concept : cs.getConcept()) {
            if (concept.getCode().equals(code.getCode()))
                return code.withSystem(codeSystem.getId()).withDisplay(concept.getDisplay());
        }
        return code;
    }
}