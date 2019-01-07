package org.opencds.cqf.bulkdata;

import ca.uhn.fhir.jpa.dao.SearchParameterMap;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Resource;
import org.opencds.cqf.helpers.BulkDataHelper;
import org.opencds.cqf.providers.JpaDataProvider;
import org.opencds.cqf.servlet.BulkDataServlet;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BulkDataRequest implements Runnable {

    private String id;
    private String requestUrl;
    private Date since;
    private StringAndListParam type;

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

    @Override
    public void run() {
        System.out.println("Running Bulk Data Request");
        BulkDataHelper helper = new BulkDataHelper(provider);
        SearchParameterMap searchMap = new SearchParameterMap();
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

        List<String> resolvedResources;
        if (type != null) {
            for (StringOrListParam stringOrListParam : type.getValuesAsQueryTokens()) {
                for (StringParam theType : stringOrListParam.getValuesAsQueryTokens()) {
                    resolvedResources = helper.resolveType(theType.getValue(), searchMap);
                    if (!resolvedResources.isEmpty()) {
                        response.addResource(theType.getValue(), "[" + StringUtils.join(resolvedResources, ",") + "]");
                    }
                }
            }
        }
        else {
            for (String theType : helper.compartmentPatient) {
                resolvedResources = helper.resolveType(theType, searchMap);
                if (!resolvedResources.isEmpty()) {
                    response.addResource(theType, "[" + StringUtils.join(resolvedResources, ",") + "]");
                }
            }
        }

        BulkDataServlet.registerResponse(id, response);
    }
}
