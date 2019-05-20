package org.opencds.cqf.cdshooks.providers;

import ca.uhn.fhir.rest.gclient.DateClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import org.opencds.cqf.cql.data.fhir.BaseFhirDataProvider;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.terminology.ValueSetInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hl7.fhir.utilities.Utilities.URLEncode;

public abstract class DiscoveryDataProvider extends BaseFhirDataProvider {

    private Discovery discovery;
    private List<Retrieve> retrieveCache;

    public Discovery getDiscovery() {
        return discovery;
    }

    public DiscoveryDataProvider() {
        discovery = new Discovery();
        retrieveCache = new ArrayList<>();
    }

    public abstract String convertPathToSearchParam(String dataType, String codeOrDatePath);

    @Override
    public Iterable<Object> retrieve(String context, Object contextValue, String dataType, String templateId,
                                     String codePath, Iterable<Code> codes, String valueSet, String datePath,
                                     String dateLowPath, String dateHighPath, Interval dateRange)
    {
        // Avoid duplicating items
        Retrieve retrieve = new Retrieve(context, contextValue, dataType, templateId, codePath,
                codes, valueSet, datePath, dateLowPath, dateHighPath, dateRange);
        if (retrieveCache.contains(retrieve)) {
            return Collections.emptyList();
        }
        else {
            retrieveCache.add(retrieve);
        }

        StringBuilder prefetchUrlBuilder = new StringBuilder();
        DiscoveryItem item = discovery.newItem().setResource(dataType);

        if (dataType == null) {
            throw new IllegalArgumentException("A data type (i.e. Procedure, Valueset, etc...) must be specified for clinical data retrieval");
        }

        if (context != null && context.equals("Patient")) {
            String patientPath = getPatientSearchParam(dataType);
            if (patientPath != null) {
                item.hasPatientCriteria();
                item.setPatientPath(patientPath);
                prefetchUrlBuilder.append(String.format("%s=%s", patientPath, contextValue));
            }
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
                    // this should never occur... TODO - throw exception?
                    prefetchUrlBuilder.append(String.format("%s:in=%s", convertPathToSearchParam(dataType, codePath), URLEncode(valueSet)));
                }
                if (codes != null) {
                    StringBuilder codeList = new StringBuilder();
                    List<String> codeValues = new ArrayList<>();
                    for (Code code : codes) {
                        if (codeList.length() > 0) {
                            codeList.append(",");
                        }

                        if (code.getSystem() != null) {
                            codeList.append(URLEncode(code.getSystem()));
                            codeList.append("|");
                        }

                        codeList.append(URLEncode(code.getCode()));
                        codeValues.add(code.getCode());
                    }

                    item.addCriteria(
                            new TokenClientParam(convertPathToSearchParam(dataType, codePath))
                                    .exactly().codes(codeValues.toArray(new String[codeValues.size()]))
                    );
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

                item.addCriteria(
                        dateRange.getLowClosed()
                                ? new DateClientParam(lowDatePath).afterOrEquals().day(dateRange.getLow().toString())
                                : new DateClientParam(lowDatePath).after().day(dateRange.getLow().toString())
                );
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

                item.addCriteria(
                        dateRange.getHighClosed()
                                ? new DateClientParam(highDatePath).beforeOrEquals().day(dateRange.getHigh().toString())
                                : new DateClientParam(highDatePath).before().day(dateRange.getHigh().toString())
                );
                prefetchUrlBuilder.append(String.format("&%s=%s%s",
                        highDatePath,
                        dateRange.getHighClosed() ? "le" : "lt",
                        dateRange.getHigh().toString()));
            }
        }

        if (prefetchUrlBuilder.length() > 0) {
            item.setUrl(String.format("%s?%s", dataType, prefetchUrlBuilder.toString()));
        }
        else {
            item.setUrl(String.format("%s", dataType));
        }

        discovery.addItem(item);
        return Collections.emptyList();
    }

    @Override
    public String getPatientSearchParam(String dataType) {
        switch (dataType) {
            case "Composition": return "subject";
            case "Coverage": return "beneficiary";
            case "Group": return "member";
            case "Patient": return "_id";
            case "Binary": case "Bundle": case "EligibilityResponse": case "Endpoint":
            case "EnrollmentResponse": case "HealthcareService": case "Location": case "Medication":
            case "Organization": case "Practitioner": case "PractitionerRole": case "ProcessRequest":
            case "ProcessResponse": case "Questionnaire": case "Substance": return null;
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
