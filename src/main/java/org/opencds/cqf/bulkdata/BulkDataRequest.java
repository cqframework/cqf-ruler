package org.opencds.cqf.bulkdata;

import ca.uhn.fhir.jpa.dao.SearchParameterMap;
import ca.uhn.fhir.rest.param.*;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Group;
import org.hl7.fhir.dstu3.model.Reference;
import org.opencds.cqf.helpers.BulkDataHelper;
import org.opencds.cqf.providers.JpaDataProvider;
import org.opencds.cqf.servlet.BulkDataServlet;

import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BulkDataRequest implements Runnable {

    private String id;
    private String requestUrl;
    private Date since;
    private StringAndListParam type;

    private Group group;

    private JpaDataProvider provider;

    // TODO: experimental params
    // private String[] typeFilter;

    public BulkDataRequest(String id, String requestUrl, Date since, StringAndListParam type, JpaDataProvider provider) {
        this.id = id;
        this.requestUrl = requestUrl;
        this.since = since;
        this.type = type;
        this.provider = provider;
    }

    public BulkDataRequest(String id, String requestUrl, Date since, StringAndListParam type, Group group, JpaDataProvider provider) {
        this.id = id;
        this.requestUrl = requestUrl;
        this.since = since;
        this.type = type;
        this.group = group;
        this.provider = provider;
    }

    private void attachArrayToResponse(String theType, Set<String> resolvedResources, BulkDataResponse response) {
        if (!resolvedResources.isEmpty()) {
            response.addResource(theType, StringUtils.join(resolvedResources, "\n"));
        }
    }

    @Override
    public void run() {
        System.out.println("Running Bulk Data Request");
        BulkDataHelper helper = new BulkDataHelper(provider);

        SearchParameterMap searchMap = new SearchParameterMap();

        ReferenceOrListParam patientParams = new ReferenceOrListParam();
        if (this.group != null) {
            Reference reference;
            for (Group.GroupMemberComponent member : group.getMember()) {
                reference = member.getEntity();
                if (reference.getReferenceElement().getResourceType().equals("Patient")) {
                    patientParams.addOr(new ReferenceParam().setValue(reference.getReference()));
                }
            }
            if (patientParams.getValuesAsQueryTokens().isEmpty()) {
                throw new IllegalArgumentException("No patients found in the Group with id: " + group.getIdElement().getIdPart());
            }
        }

        searchMap.setLastUpdated(new DateRangeParam());
        if (since != null) {
            DateRangeParam rangeParam = new DateRangeParam(since, new Date());
            searchMap.setLastUpdated(rangeParam);
        }

        BulkDataResponse response = new BulkDataResponse();
        response.setRequest(requestUrl);
        response.setTransactionTime(Date.from(Instant.now()));
        response.setOutputUrlBase(System.getProperty("fhir.baseurl.dstu3") + "/export-results/" + id);
        response.setRequiresAccessToken(false);

        Set<String> resolvedResources;
        if (type != null) {
            for (StringOrListParam stringOrListParam : type.getValuesAsQueryTokens()) {
                for (StringParam theType : stringOrListParam.getValuesAsQueryTokens()) {
                    try {
                        if (this.group != null) {
                            resolvedResources = helper.resolveGroupType(theType.getValue(), patientParams, searchMap);
                            attachArrayToResponse(theType.getValue(), resolvedResources, response);
                        } else {
                            resolvedResources = helper.resolveType(theType.getValue(), searchMap);
                            attachArrayToResponse(theType.getValue(), resolvedResources, response);
                        }
                    } catch (Exception e) {
                        response.addError(helper.getErrorOutcomeString(e.getMessage()));
                    }
                }
            }
        }
        else {
            for (String theType : helper.compartmentPatient) {
                try {
                    if (this.group != null) {
                        resolvedResources = helper.resolveGroupType(theType, patientParams, searchMap);
                        attachArrayToResponse(theType, resolvedResources, response);
                    } else {
                        resolvedResources = helper.resolveType(theType, searchMap);
                        attachArrayToResponse(theType, resolvedResources, response);
                    }
                } catch (Exception e) {
                    response.addError(helper.getErrorOutcomeString(e.getMessage()));
                }
            }
        }

        BulkDataServlet.registerResponse(id, response);
    }
}
