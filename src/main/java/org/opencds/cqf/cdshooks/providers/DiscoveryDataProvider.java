package org.opencds.cqf.cdshooks.providers;

import org.opencds.cqf.cql.data.fhir.BaseFhirDataProvider;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.terminology.ValueSetInfo;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import static org.hl7.fhir.utilities.Utilities.URLEncode;

public abstract class DiscoveryDataProvider extends BaseFhirDataProvider {

    private Set<String> prefetchUrls;
    public Set<String> getPrefetchUrls() {
        return prefetchUrls;
    }
    public DiscoveryDataProvider() {
        prefetchUrls = new TreeSet<>();
    }

    public abstract String convertPathToSearchParam(String dataType, String codeOrDatePath);

    @Override
    public Iterable<Object> retrieve(String context, Object contextValue, String dataType, String templateId,
                                     String codePath, Iterable<Code> codes, String valueSet, String datePath,
                                     String dateLowPath, String dateHighPath, Interval dateRange)
    {
        StringBuilder prefetchUrlBuilder = new StringBuilder();

        if (dataType == null) {
            throw new IllegalArgumentException("A data type (i.e. Procedure, Valueset, etc...) must be specified for clinical data retrieval");
        }

        if (context != null && context.equals("Patient")) {
            prefetchUrlBuilder.append(String.format("%s=%s", getPatientSearchParam(dataType), contextValue));
        }

        if (codePath != null && !codePath.equals("")) {
            if (prefetchUrlBuilder.length() > 0) {
                prefetchUrlBuilder.append("&");
            }
            if (valueSet != null && !valueSet.equals("")) {
                if (terminologyProvider != null && expandValueSets) {
                    ValueSetInfo valueSetInfo = new ValueSetInfo().withId(valueSet);
                    codes = terminologyProvider.expand(valueSetInfo);
                }
                else {
                    prefetchUrlBuilder.append(String.format("%s:in=%s", convertPathToSearchParam(dataType, codePath), URLEncode(valueSet)));
                }
                if (codes != null) {
                    StringBuilder codeList = new StringBuilder();
                    for (Code code : codes) {
                        if (codeList.length() > 0) {
                            codeList.append(",");
                        }

                        if (code.getSystem() != null) {
                            codeList.append(URLEncode(code.getSystem()));
                            codeList.append("|");
                        }

                        codeList.append(URLEncode(code.getCode()));
                    }
                    prefetchUrlBuilder.append(String.format("%s=%s", convertPathToSearchParam(dataType, codePath), codeList.toString()));
                }
            }
        }

        if (dateRange != null) {
            if (dateRange.getLow() != null) {
                String lowDatePath = convertPathToSearchParam(dataType, dateLowPath != null ? dateLowPath : datePath);
                if (lowDatePath == null || lowDatePath.equals("")) {
                    throw new IllegalArgumentException("A date path or low date path must be provided when filtering on a date range.");
                }

                prefetchUrlBuilder.append(String.format("&%s=%s%s",
                        lowDatePath,
                        dateRange.getLowClosed() ? "ge" : "gt",
                        dateRange.getLow().toString()));
            }

            if (dateRange.getHigh() != null) {
                String highDatePath = convertPathToSearchParam(dataType, dateHighPath != null ? dateHighPath : datePath);
                if (highDatePath == null || highDatePath.equals("")) {
                    throw new IllegalArgumentException("A date path or high date path must be provided when filtering on a date range.");
                }

                prefetchUrlBuilder.append(String.format("&%s=%s%s",
                        highDatePath,
                        dateRange.getHighClosed() ? "le" : "lt",
                        dateRange.getHigh().toString()));
            }
        }

        if (prefetchUrlBuilder.length() > 0) {
            prefetchUrls.add(String.format("%s?%s", dataType, prefetchUrlBuilder.toString()));
        }
        else {
            prefetchUrls.add(String.format("%s", dataType));
        }

        return Collections.emptyList();
    }

    @Override
    public String getPatientSearchParam(String dataType) {
        switch (dataType) {
            case "Coverage": return "beneficiary";
            case "Patient": return "_id";
            default: return "patient";
        }
    }

    @Override
    protected String resolveClassName(String s) {
        return null;
    }

    @Override
    protected Object fromJavaPrimitive(Object o, Object o1) {
        return null;
    }

    @Override
    protected Object toJavaPrimitive(Object o, Object o1) {
        return null;
    }
}
