package org.opencds.cqf.ruler.plugin.cdshooks.providers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.opencds.cqf.cql.engine.elm.execution.InEvaluator;
import org.opencds.cqf.cql.engine.elm.execution.IncludesEvaluator;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.retrieve.TerminologyAwareRetrieveProvider;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;
import org.opencds.cqf.cql.evaluator.engine.util.CodeUtil;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public class PrefetchDataProviderDstu2 extends TerminologyAwareRetrieveProvider {

    private Map<String, List<Object>> prefetchResources;
    private ModelResolver resolver;
    private CodeUtil codeUtil;

    public PrefetchDataProviderDstu2(List<Object> resources, ModelResolver modelResolver) {
        prefetchResources = PrefetchDataProviderHelper.populateMap(resources);
        this.resolver = modelResolver;
        this.codeUtil = new CodeUtil(FhirContext.forCached(FhirVersionEnum.DSTU2));
    }

    @Override
    public Iterable<Object> retrieve(String context, String contextPath, Object contextValue, String dataType,
            String templateId, String codePath, Iterable<Code> codes, String valueSet, String datePath,
            String dateLowPath, String dateHighPath, Interval dateRange) {


        if (codePath == null && (codes != null || valueSet != null)) {
            throw new IllegalArgumentException("A code path must be provided when filtering on codes or a valueset.");
        }

        if (dataType == null) {
            throw new IllegalArgumentException(
                    "A data type (i.e. Procedure, Valueset, etc...) must be specified for clinical data retrieval");
        }

        // This dataType can't be related to patient, therefore may
        // not be in the pre-fetch bundle, or might required a lookup by Id
        if (context.equals("Patient") && contextPath == null) {
            return null;
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
                        throw new IllegalArgumentException(
                                "If the datePath is specified, the dateLowPath and dateHighPath attributes must not be present.");
                    }

                    Object dateObject = PrefetchDataProviderHelper
                            .getDstu2DateTime(this.resolver.resolvePath(resource, datePath));
                    DateTime date = dateObject instanceof DateTime ? (DateTime) dateObject : null;
                    Interval dateInterval = dateObject instanceof Interval ? (Interval) dateObject : null;
                    String precision = PrefetchDataProviderHelper.getPrecision(Arrays.asList(dateRange, date));

                    if (date != null && !(InEvaluator.in(date, dateRange, precision))) {
                        includeResource = false;
                    }
                    // TODO - add precision to includes evaluator
                    else if (dateInterval != null
                            && !(IncludesEvaluator.includes(dateRange, dateInterval, precision))) {
                        includeResource = false;
                    }
                } else {
                    if (dateHighPath == null && dateLowPath == null) {
                        throw new IllegalArgumentException(
                                "If the datePath is not given, either the lowDatePath or highDatePath must be provided.");
                    }

                    DateTime lowDate = dateLowPath == null ? null
                            : (DateTime) PrefetchDataProviderHelper
                                    .getDstu2DateTime(this.resolver.resolvePath(resource, dateLowPath));
                    DateTime highDate = dateHighPath == null ? null
                            : (DateTime) PrefetchDataProviderHelper
                                    .getDstu2DateTime(this.resolver.resolvePath(resource, dateHighPath));

                    String precision = PrefetchDataProviderHelper
                            .getPrecision(Arrays.asList(dateRange, lowDate, highDate));

                    Interval interval = new Interval(lowDate, true, highDate, true);

                    // TODO - add precision to includes evaluator
                    if (!IncludesEvaluator.includes(dateRange, interval, precision)) {
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
                    Object codeObject = PrefetchDataProviderHelper
                            .getDstu2Code(this.resolver.resolvePath(resource, codePath));

                    includeResource = PrefetchDataProviderHelper.checkCodeMembership(codes, codeObject, this.codeUtil);
                }
            }

            if (includeResource) {
                returnList.add(resource);
            }
        }

        return returnList;
    }
}
