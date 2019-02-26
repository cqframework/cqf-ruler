package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.*;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringParam;
import org.hl7.fhir.dstu3.model.*;
import org.opencds.cqf.helpers.BulkDataHelper;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;

public class BulkDataGroupProvider extends JpaResourceProviderDstu3<Group> {

    private JpaDataProvider provider;

    public BulkDataGroupProvider(JpaDataProvider provider) {
        this.provider = provider;
    }

//    @Operation(name = "$export", idempotent = true)
//    public OperationOutcome exportGroupData(
//            javax.servlet.http.HttpServletRequest theServletRequest,
//            RequestDetails theRequestDetails,
//            @IdParam IdType theId,
//            @OperationParam(name="_outputFormat") String outputFormat,
//            @OperationParam(name="_since") DateParam since,
//            @OperationParam(name="_type") StringAndListParam type) throws ServletException, IOException
//    {
//        BulkDataHelper helper = new BulkDataHelper(provider);
//
//        if (theRequestDetails.getHeader("Accept") == null) {
//            return helper.createErrorOutcome("Please provide the Accept header, which must be set to application/fhir+json");
//        } else if (!theRequestDetails.getHeader("Accept").equals("application/fhir+json")) {
//            return helper.createErrorOutcome("Only the application/fhir+json value for the Accept header is currently supported");
//        }
//        if (theRequestDetails.getHeader("Prefer") == null) {
//            return helper.createErrorOutcome("Please provide the Prefer header, which must be set to respond-async");
//        } else if (!theRequestDetails.getHeader("Prefer").equals("respond-async")) {
//            return helper.createErrorOutcome("Only the respond-async value for the Prefer header is currently supported");
//        }
//
//        if (outputFormat != null) {
//            if (!(outputFormat.equals("application/fhir+ndjson")
//                    || outputFormat.equals("application/ndjson")
//                    || outputFormat.equals("ndjson"))) {
//                return helper.createErrorOutcome("Only ndjson for the _outputFormat parameter is currently supported");
//            }
//        }
//
//        Group group = this.getDao().read(theId);
//
//        if (group == null) {
//            return helper.createErrorOutcome("Group with id " + theId + " could not be found");
//        }
//
//        SearchParameterMap searchMap = new SearchParameterMap();
//
//        ReferenceOrListParam patientParams = new ReferenceOrListParam();
//        Reference reference;
//        for (Group.GroupMemberComponent member : group.getMember()) {
//            reference = member.getEntity();
//            if (reference.getReferenceElement().getResourceType().equals("Patient")) {
//                patientParams.addOr(new ReferenceParam().setValue(reference.getReference()));
//            }
//        }
//
//        if (patientParams.getValuesAsQueryTokens().isEmpty()) {
//            return helper.createErrorOutcome("No patients found in the Group with id: " + theId.getIdPart());
//        }
//
//        searchMap.setLastUpdated(new DateRangeParam());
//        if (since != null) {
//            DateRangeParam rangeParam = new DateRangeParam(since.getValue(), new Date());
//            searchMap.setLastUpdated(rangeParam);
//        }
//
//        List<List<Resource> > resources = new ArrayList<>();
//        List<Resource> resolvedResources;
//        if (type != null) {
//            for (StringOrListParam stringOrListParam : type.getValuesAsQueryTokens()) {
//                for (StringParam theType : stringOrListParam.getValuesAsQueryTokens()) {
//                    SearchParameterMap newMap = (SearchParameterMap)((SearchParameterMap) searchMap).clone();
//                    for (String param : helper.getPatientInclusionPath(theType.getValue())) {
//                        newMap.add(param, patientParams);
//                    }
//                    resolvedResources = helper.resolveType(theType.getValue(), newMap);
//                    if (!resolvedResources.isEmpty()) {
//                        resources.add(resolvedResources);
//                    }
//                }
//            }
//        }
//        else {
//            for (String theType : helper.compartmentPatient) {
//                SearchParameterMap newMap = (SearchParameterMap)((SearchParameterMap) searchMap).clone();
//                for (String param : helper.getPatientInclusionPath(theType)) {
//                    newMap.add(param, patientParams);
//                }
//                resolvedResources = helper.resolveType(theType, newMap);
//                if (!resolvedResources.isEmpty()) {
//                    resources.add(resolvedResources);
//                }
//            }
//        }
//
//        return helper.createOutcome(resources, theServletRequest, theRequestDetails);
//    }

    @Search(allowUnknownParams=true)
    public ca.uhn.fhir.rest.api.server.IBundleProvider search(
            javax.servlet.http.HttpServletRequest theServletRequest,
            javax.servlet.http.HttpServletResponse theServletResponse,
            ca.uhn.fhir.rest.api.server.RequestDetails theRequestDetails,
            @Description(shortDefinition="Search the contents of the resource's data using a fulltext search")
            @OptionalParam(name=ca.uhn.fhir.rest.api.Constants.PARAM_CONTENT)
                    StringAndListParam theFtContent,
            @Description(shortDefinition="Search the contents of the resource's narrative using a fulltext search")
            @OptionalParam(name=ca.uhn.fhir.rest.api.Constants.PARAM_TEXT)
                    StringAndListParam theFtText,
            @Description(shortDefinition="Search for resources which have the given tag")
            @OptionalParam(name=ca.uhn.fhir.rest.api.Constants.PARAM_TAG)
                    TokenAndListParam theSearchForTag,
            @Description(shortDefinition="Search for resources which have the given security labels")
            @OptionalParam(name=ca.uhn.fhir.rest.api.Constants.PARAM_SECURITY)
                    TokenAndListParam theSearchForSecurity,
            @Description(shortDefinition="Search for resources which have the given profile")
            @OptionalParam(name=ca.uhn.fhir.rest.api.Constants.PARAM_PROFILE)
                    UriAndListParam theSearchForProfile,
            @Description(shortDefinition="Return resources linked to by the given target")
            @OptionalParam(name="_has")
                    HasAndListParam theHas,
            @Description(shortDefinition="The ID of the resource")
            @OptionalParam(name="_id")
                    TokenAndListParam the_id,
            @Description(shortDefinition="The language of the resource")
            @OptionalParam(name="_language")
                    StringAndListParam the_language,
            @Description(shortDefinition="Descriptive or actual")
            @OptionalParam(name="actual")
                    TokenAndListParam theActual,
            @Description(shortDefinition="Kind of characteristic")
            @OptionalParam(name="characteristic")
                    TokenAndListParam theCharacteristic,
            @Description(shortDefinition="A composite of both characteristic and value")
            @OptionalParam(name="characteristic-value", compositeTypes= { TokenParam.class, TokenParam.class })
                    CompositeAndListParam<TokenParam, TokenParam> theCharacteristic_value,
            @Description(shortDefinition="The kind of resources contained")
            @OptionalParam(name="code")
                    TokenAndListParam theCode,
            @Description(shortDefinition="Group includes or excludes")
            @OptionalParam(name="exclude")
                    TokenAndListParam theExclude,
            @Description(shortDefinition="Unique id")
            @OptionalParam(name="identifier")
                    TokenAndListParam theIdentifier,
            @Description(shortDefinition="Reference to the group member")
            @OptionalParam(name="member", targetTypes={  } )
                    ReferenceAndListParam theMember,
            @Description(shortDefinition="The type of resources the group contains")
            @OptionalParam(name="type")
                    TokenAndListParam theType,
            @Description(shortDefinition="Value held by characteristic")
            @OptionalParam(name="value")
                    TokenAndListParam theValue,
            @RawParam
                    Map<String, List<String>> theAdditionalRawParams,
            @IncludeParam(reverse=true)
                    Set<Include> theRevIncludes,
            @Description(shortDefinition="Only return resources which were last updated as specified by the given range")
            @OptionalParam(name="_lastUpdated")
                    DateRangeParam theLastUpdated,
            @IncludeParam(allow= {
                    "Group:member" 					, "*"
            })
                    Set<Include> theIncludes,
            @Sort
                    SortSpec theSort,
            @ca.uhn.fhir.rest.annotation.Count
                    Integer theCount
    ) {
        startRequest(theServletRequest);
        try {
            SearchParameterMap paramMap = new SearchParameterMap();
            paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_CONTENT, theFtContent);
            paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_TEXT, theFtText);
            paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_TAG, theSearchForTag);
            paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_SECURITY, theSearchForSecurity);
            paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_PROFILE, theSearchForProfile);
            paramMap.add("_has", theHas);
            paramMap.add("_id", the_id);
            paramMap.add("_language", the_language);
            paramMap.add("actual", theActual);
            paramMap.add("characteristic", theCharacteristic);
            paramMap.add("characteristic-value", theCharacteristic_value);
            paramMap.add("code", theCode);
            paramMap.add("exclude", theExclude);
            paramMap.add("identifier", theIdentifier);
            paramMap.add("member", theMember);
            paramMap.add("type", theType);
            paramMap.add("value", theValue);
            paramMap.setRevIncludes(theRevIncludes);
            paramMap.setLastUpdated(theLastUpdated);
            paramMap.setIncludes(theIncludes);
            paramMap.setSort(theSort);
            paramMap.setCount(theCount);
            getDao().translateRawParameters(theAdditionalRawParams, paramMap);
            return getDao().search(paramMap, theRequestDetails, theServletResponse);
        } finally {
            endRequest(theServletRequest);
        }
    }
}
