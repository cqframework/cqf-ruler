package org.opencds.cqf.cdshooks.providers;

import ca.uhn.fhir.context.FhirContext;
import org.opencds.cqf.cql.data.fhir.FhirDataProviderDstu2;
import org.opencds.cqf.cql.elm.execution.InEvaluator;
import org.opencds.cqf.cql.elm.execution.IncludesEvaluator;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.terminology.ValueSetInfo;

import java.util.*;

public class PrefetchDataProviderDstu2 extends FhirDataProviderDstu2 {

    private Map<String, List<Object>> prefetchResources;

    public PrefetchDataProviderDstu2(List<Object> resources) {
        prefetchResources = PrefetchDataProviderHelper.populateMap(resources);
        setPackageName("ca.uhn.fhir.model.dstu2.resource");
        setFhirContext(FhirContext.forDstu2());
    }

    @Override
    public Iterable<Object> retrieve(String context, Object contextValue, String dataType, String templateId,
                                     String codePath, Iterable<Code> codes, String valueSet, String datePath,
                                     String dateLowPath, String dateHighPath, Interval dateRange)
    {
        if (codePath == null && (codes != null || valueSet != null)) {
            throw new IllegalArgumentException("A code path must be provided when filtering on codes or a valueset.");
        }

        if (dataType == null) {
            throw new IllegalArgumentException("A data type (i.e. Procedure, Valueset, etc...) must be specified for clinical data retrieval");
        }

        List<Object> resourcesOfType = prefetchResources.get(dataType);

        if (resourcesOfType == null) {
            return Collections.emptyList();
        }

        // no resources or no filtering -> return list
        if (resourcesOfType.isEmpty() || (dateRange == null && codePath == null)) {
            return resourcesOfType;
        }

        List<Object> returnList = new ArrayList<>();
        for (Object resource : resourcesOfType) {
            boolean includeResource = true;
            if (dateRange != null) {
                if (datePath != null) {
                    if (dateHighPath != null || dateLowPath != null) {
                        throw new IllegalArgumentException("If the datePath is specified, the dateLowPath and dateHighPath attributes must not be present.");
                    }

                    Object dateObject = PrefetchDataProviderHelper.getDstu2DateTime(resolvePath(resource, datePath));
                    DateTime date = dateObject instanceof DateTime ? (DateTime) dateObject : null;
                    Interval dateInterval = dateObject instanceof Interval ? (Interval) dateObject : null;
                    String precision = PrefetchDataProviderHelper.getPrecision(Arrays.asList(dateRange, date));

                    if (date != null && !(InEvaluator.in(date, dateRange, precision))) {
                        includeResource = false;
                    }
                    // TODO - add precision to includes evaluator
                    else if (dateInterval != null && !((Boolean) IncludesEvaluator.includes(dateRange, dateInterval, precision))) {
                        includeResource = false;
                    }
                }
                else {
                    if (dateHighPath == null && dateLowPath == null) {
                        throw new IllegalArgumentException("If the datePath is not given, either the lowDatePath or highDatePath must be provided.");
                    }

                    DateTime lowDate = dateLowPath == null ? null : (DateTime) PrefetchDataProviderHelper.getDstu2DateTime(resolvePath(resource, dateLowPath));
                    DateTime highDate = dateHighPath == null ? null : (DateTime) PrefetchDataProviderHelper.getDstu2DateTime(resolvePath(resource, dateHighPath));

                    String precision = PrefetchDataProviderHelper.getPrecision(Arrays.asList(dateRange, lowDate, highDate));

                    Interval interval = new Interval(lowDate, true, highDate, true);

                    // TODO - add precision to includes evaluator
                    if (!(Boolean) IncludesEvaluator.includes(dateRange, interval, precision)) {
                        includeResource = false;
                    }
                }
            }

            if (codePath != null && !codePath.equals("") && includeResource) {
                if (valueSet != null && terminologyProvider != null) {
                    if (valueSet.startsWith("urn:oid:")) {
                        valueSet = valueSet.replace("urn:oid:", "");
                    }
                    ValueSetInfo valueSetInfo = new ValueSetInfo().withId(valueSet);
                    codes = terminologyProvider.expand(valueSetInfo);
                }
                if (codes != null) {
                    Object codeObject = PrefetchDataProviderHelper.getDstu2Code(resolvePath(resource, convertPathFromCodeParam(dataType, codePath)));
                    includeResource = PrefetchDataProviderHelper.checkCodeMembership(codes, codeObject);
                }
            }

            if (includeResource) {
                returnList.add(resource);
            }
        }

        return returnList;
    }

    private String convertPathFromCodeParam(String dataType, String codeOrDatePath) {
        switch (dataType) {
            case "MedicationOrder":
                if (codeOrDatePath.equals("code")) return "medication";
                break;
        }
        return codeOrDatePath;
    }
}
