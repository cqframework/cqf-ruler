package org.opencds.cqf.r4.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.rp.r4.ValueSetResourceProvider;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.jpa.term.api.ITermReadSvcR4;
import ca.uhn.fhir.jpa.term.VersionIndependentConcept;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.terminology.CodeSystemInfo;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.cql.terminology.ValueSetInfo;

import java.util.ArrayList;
import java.util.List;

public class JpaTerminologyProvider implements TerminologyProvider {

    private ITermReadSvcR4 terminologySvcR4;
    private FhirContext context;
    private ValueSetResourceProvider valueSetResourceProvider;

    public JpaTerminologyProvider(ITermReadSvcR4 terminologySvcR4, FhirContext context, ValueSetResourceProvider valueSetResourceProvider) {
        this.terminologySvcR4 = terminologySvcR4;
        this.context = context;
        this.valueSetResourceProvider = valueSetResourceProvider;
    }

    @Override
    public synchronized boolean in(Code code, ValueSetInfo valueSet) throws ResourceNotFoundException {
        for (Code c : expand(valueSet)) {
            if (c == null) continue;
            if (c.getCode().equals(code.getCode()) && c.getSystem().equals(code.getSystem())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized Iterable<Code> expand(ValueSetInfo valueSet) throws ResourceNotFoundException {
        List<Code> codes = new ArrayList<>();
        boolean needsExpand = false;
        ValueSet vs;
        if (valueSet.getId().startsWith("http://") || valueSet.getId().startsWith("https://")) {
            if (valueSet.getVersion() != null || (valueSet.getCodeSystems() != null && valueSet.getCodeSystems().size() > 0)) {
                if (!(valueSet.getCodeSystems().size() == 1 && valueSet.getCodeSystems().get(0).getVersion() == null)) {
                    throw new UnsupportedOperationException(String.format("Could not expand value set %s; version and code system bindings are not supported at this time.", valueSet.getId()));
                }
            }
            IBundleProvider bundleProvider = valueSetResourceProvider.getDao().search(new SearchParameterMap().add(ValueSet.SP_URL, new UriParam(valueSet.getId())));
            List<IBaseResource> valueSets = bundleProvider.getResources(0, bundleProvider.size());
            if (valueSets.isEmpty()) {
                throw new IllegalArgumentException(String.format("Could not resolve value set %s.", valueSet.getId()));
            }
            else if (valueSets.size() == 1) {
                vs = (ValueSet) valueSets.get(0);
            }
            else {
                throw new IllegalArgumentException("Found more than 1 ValueSet with url: " + valueSet.getId());
            }
        }
        else {
            vs = valueSetResourceProvider.getDao().read(new IdType(valueSet.getId()));
        }
        if (vs != null) {
            if (vs.hasCompose()) {
                if (vs.getCompose().hasInclude()) {
                    for (ValueSet.ConceptSetComponent include : vs.getCompose().getInclude()) {
                        if (include.hasValueSet() || include.hasFilter()) {
                            needsExpand = true;
                            break;
                        }
                        for (ValueSet.ConceptReferenceComponent concept : include.getConcept()) {
                            if (concept.hasCode()) {
                                codes.add(new Code().withCode(concept.getCode()).withSystem(include.getSystem()));
                            }
                        }
                    }
                    if (!needsExpand) {
                        return codes;
                    }
                }
            }
        }

        List<VersionIndependentConcept> expansion = terminologySvcR4.expandValueSet(valueSet.getId());
        for (VersionIndependentConcept concept : expansion) {
            codes.add(new Code().withCode(concept.getCode()).withSystem(concept.getSystem()));
        }

        return codes;
    }

    @Override
    public synchronized Code lookup(Code code, CodeSystemInfo codeSystem) throws ResourceNotFoundException {
        CodeSystem cs = terminologySvcR4.fetchCodeSystem(context, codeSystem.getId());
        for (CodeSystem.ConceptDefinitionComponent concept : cs.getConcept()) {
            if (concept.getCode().equals(code.getCode()))
                return code.withSystem(codeSystem.getId()).withDisplay(concept.getDisplay());
        }
        return code;
    }
}