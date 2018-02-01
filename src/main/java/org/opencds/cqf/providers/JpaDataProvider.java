package org.opencds.cqf.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.dao.SearchParameterMap;
import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.data.fhir.FhirDataProviderStu3;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.terminology.ValueSetInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Christopher Schuler on 7/17/2017.
 */
public class JpaDataProvider extends FhirDataProviderStu3 {

    // need these to access the dao
    private HashMap<String, IResourceProvider> providers;
    private Collection<IResourceProvider> collectionProviders;

    public Collection<IResourceProvider> getCollectionProviders() {
        return this.collectionProviders;
    }

    public JpaDataProvider(Collection<IResourceProvider> providers) {
        this.collectionProviders = providers;
        this.providers = new HashMap<>();
        for (IResourceProvider i : providers) {
            this.providers.put(i.getResourceType().getSimpleName(), i);
        }

        // NOTE: Defaults to STU3
        setPackageName("org.hl7.fhir.dstu3.model");
        setFhirContext(FhirContext.forDstu3());
    }

    public Iterable<Object> retrieve(String context, Object contextValue, String dataType, String templateId,
                                     String codePath, Iterable<Code> codes, String valueSet, String datePath,
                                     String dateLowPath, String dateHighPath, Interval dateRange)
    {

        SearchParameterMap map = new SearchParameterMap();
        map.setLastUpdated(new DateRangeParam());

        if (templateId != null && !templateId.equals("")) {
            // do something?
        }

        if (valueSet != null && valueSet.startsWith("urn:oid:")) {
            valueSet = valueSet.replace("urn:oid:", "");
        }

        if (codePath == null && (codes != null || valueSet != null)) {
            throw new IllegalArgumentException("A code path must be provided when filtering on codes or a valueset.");
        }

        if (dataType == null) {
            throw new IllegalArgumentException("A data type (i.e. Procedure, Valueset, etc...) must be specified for clinical data retrieval");
        }

        if (context != null && context.equals("Patient") && contextValue != null) {
            ReferenceParam patientParam = new ReferenceParam(contextValue.toString());
            map.add(getPatientSearchParam(dataType), patientParam);
        }

        if (codePath != null && !codePath.equals("")) {

            if (valueSet != null && terminologyProvider != null && expandValueSets) {
                ValueSetInfo valueSetInfo = new ValueSetInfo().withId(valueSet);
                codes = terminologyProvider.expand(valueSetInfo);
            }
            if (codes != null) {
                TokenOrListParam codeParams = new TokenOrListParam();
                for (Code code : codes) {
                    codeParams.addOr(new TokenParam(code.getSystem(), code.getCode()));
                }
                map.add(convertCodePath(codePath), codeParams);
            }
        }

        if (dateRange != null) {
            DateParam low = null;
            DateParam high = null;
            if (dateRange.getLow() != null) {
                low = new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, ((DateTime) dateRange.getLow()).getJodaDateTime().toDate());
            }

            if (dateRange.getHigh() != null) {
                high = new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, ((DateTime) dateRange.getHigh()).getJodaDateTime().toDate());
            }

            DateRangeParam rangeParam;
            if (low == null && high != null) {
                rangeParam = new DateRangeParam(high);
            }
            else if (high == null && low != null) {
                rangeParam = new DateRangeParam(low);
            }
            else {
                rangeParam = new DateRangeParam(low, high);
            }

            map.add(convertDatePath(datePath), rangeParam);
        }

        JpaResourceProviderDstu3<? extends IAnyResource> jpaResProvider = resolveResourceProvider(dataType);
        IBundleProvider bundleProvider = jpaResProvider.getDao().search(map);
        List<IBaseResource> resourceList = bundleProvider.getResources(0, 10000);
        return resolveResourceList(resourceList);
    }

    public Iterable<Object> resolveResourceList(List<IBaseResource> resourceList) {
        List<Object> ret = new ArrayList<>();
        for (IBaseResource res : resourceList) {
            Class clazz = res.getClass();
            ret.add(clazz.cast(res));
        }
        // ret.addAll(resourceList);
        return ret;
    }

    public JpaResourceProviderDstu3<? extends IAnyResource> resolveResourceProvider(String datatype) {
        return (JpaResourceProviderDstu3<? extends IAnyResource>) providers.get(datatype);
    }

    public String convertDatePath(String path) {
        if (path.contains("effective")) {
            return "date";
        }

        return path;
    }

    public String convertCodePath(String path) {
        if (path.contains("medication")) {
            return "code";
        }
        else if (path.equals("vaccineCode")) {
            return "vaccine-code";
        }

        return path;
    }
}
