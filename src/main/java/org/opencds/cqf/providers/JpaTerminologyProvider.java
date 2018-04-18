package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.dao.SearchParameterMap;
import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.terminology.CodeSystemInfo;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.cql.terminology.ValueSetInfo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Christopher Schuler on 7/17/2017.
 */
public class JpaTerminologyProvider implements TerminologyProvider {
    private JpaResourceProviderDstu3<ValueSet> valueSetProvider;
    private JpaResourceProviderDstu3<CodeSystem> codeSystemProvider;

    public JpaTerminologyProvider(JpaResourceProviderDstu3<ValueSet> valueSetProvider, JpaResourceProviderDstu3<CodeSystem> codeSystemProvider) {
        this.valueSetProvider = valueSetProvider;
        this.codeSystemProvider = codeSystemProvider;
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
        ValueSet vs = null;
        try {
            URL url = new URL(valueSet.getId());
            // Get valueset by url
            IBundleProvider bundleProvider =
                    valueSetProvider.getDao().search(new SearchParameterMap().add(ValueSet.SP_URL, new UriParam(url.toString())));
            List<IBaseResource> resources = bundleProvider.getResources(0,1);
            if (!resources.isEmpty()) {
                vs = (ValueSet) resources.get(0);
            }
        } catch (MalformedURLException mfe) {
            // continue
        }

        if (vs == null) {
            vs = valueSetProvider.getDao().read(new IdType(valueSet.getId()));
        }

        List<Code> codes = new ArrayList<>();
        for (ValueSet.ValueSetExpansionContainsComponent expansion : vs.getExpansion().getContains()) {
            codes.add(new Code().withCode(expansion.getCode()).withSystem(expansion.getSystem()));
        }

        if (codes.isEmpty()) {
            for (ValueSet.ConceptSetComponent include : vs.getCompose().getInclude()) {
                String system = include.getSystem();
                for (ValueSet.ConceptReferenceComponent component : include.getConcept()) {
                    codes.add(new Code().withCode(component.getCode()).withSystem(system));
                }
            }
        }

        return codes;
    }

    @Override
    public Code lookup(Code code, CodeSystemInfo codeSystem) throws ResourceNotFoundException {
        CodeSystem cs = codeSystemProvider.getDao().read(new IdType(codeSystem.getId()));
        for (CodeSystem.ConceptDefinitionComponent concept : cs.getConcept()) {
            if (concept.getCode().equals(code.getCode()))
                return code.withSystem(codeSystem.getId()).withDisplay(concept.getDisplay());
        }
        return code;
    }
}
