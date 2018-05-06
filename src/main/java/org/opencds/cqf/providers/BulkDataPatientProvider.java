package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.dao.SearchParameterMap;
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
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Resource;
import org.opencds.cqf.helpers.BulkDataHelper;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;

public class BulkDataPatientProvider extends JpaResourceProviderDstu3<Patient> {

    private JpaDataProvider provider;
    public BulkDataPatientProvider(JpaDataProvider provider) {
        this.provider = provider;
    }

    @Operation(name = "$export", idempotent = true)
    public OperationOutcome exportAllPatientData(
            javax.servlet.http.HttpServletRequest theServletRequest,
            RequestDetails theRequestDetails,
            @OperationParam(name="_outputFormat") String outputFormat,
            @OperationParam(name="_since") DateParam since,
            @OperationParam(name="_type") StringAndListParam type) throws ServletException, IOException
    {
        BulkDataHelper helper = new BulkDataHelper(provider);

        if (theRequestDetails.getHeader("Accept") == null) {
            return helper.createErrorOutcome("Please provide the Accept header, which must be set to application/fhir+json");
        }
        else if (!theRequestDetails.getHeader("Accept").equals("application/fhir+json")) {
            return helper.createErrorOutcome("Only the application/fhir+json value for the Accept header is currently supported");
        }
        if (theRequestDetails.getHeader("Prefer") == null) {
            return helper.createErrorOutcome("Please provide the Prefer header, which must be set to respond-async");
        }
        else if (!theRequestDetails.getHeader("Prefer").equals("respond-async")) {
            return helper.createErrorOutcome("Only the respond-async value for the Prefer header is currently supported");
        }

        if (outputFormat != null) {
            if (!(outputFormat.equals("application/fhir+ndjson")
                    || outputFormat.equals("application/ndjson")
                    || outputFormat.equals("ndjson")))
            {
                return helper.createErrorOutcome("Only ndjson for the _outputFormat parameter is currently supported");
            }
        }

        SearchParameterMap searchMap = new SearchParameterMap();
        searchMap.setLastUpdated(new DateRangeParam());
        if (since != null) {
            DateRangeParam rangeParam = new DateRangeParam(since.getValue(), new Date());
            searchMap.setLastUpdated(rangeParam);
        }

        List<List<Resource> > resources = new ArrayList<>();
        List<Resource> resolvedResources;
        if (type != null) {
            for (StringOrListParam stringOrListParam : type.getValuesAsQueryTokens()) {
                for (StringParam theType : stringOrListParam.getValuesAsQueryTokens()) {
                    resolvedResources = helper.resolveType(theType.getValue(), searchMap);
                    if (!resolvedResources.isEmpty()) {
                        resources.add(resolvedResources);
                    }
                }
            }
        }
        else {
            for (String theType : helper.compartmentPatient) {
                resolvedResources = helper.resolveType(theType, searchMap);
                if (!resolvedResources.isEmpty()) {
                    resources.add(resolvedResources);
                }
            }
        }

        return helper.createOutcome(resources, theServletRequest, theRequestDetails);
    }

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
            @Description(shortDefinition="Whether the patient record is active")
            @OptionalParam(name="active")
                    TokenAndListParam theActive,
            @Description(shortDefinition="A server defined search that may match any of the string fields in the Address, including line, city, state, country, postalCode, and/or text")
            @OptionalParam(name="address")
                    StringAndListParam theAddress,
            @Description(shortDefinition="A city specified in an address")
            @OptionalParam(name="address-city")
                    StringAndListParam theAddress_city,
            @Description(shortDefinition="A country specified in an address")
            @OptionalParam(name="address-country")
                    StringAndListParam theAddress_country,
            @Description(shortDefinition="A postalCode specified in an address")
            @OptionalParam(name="address-postalcode")
                    StringAndListParam theAddress_postalcode,
            @Description(shortDefinition="A state specified in an address")
            @OptionalParam(name="address-state")
                    StringAndListParam theAddress_state,
            @Description(shortDefinition="A use code specified in an address")
            @OptionalParam(name="address-use")
                    TokenAndListParam theAddress_use,
            @Description(shortDefinition="The breed for animal patients")
            @OptionalParam(name="animal-breed")
                    TokenAndListParam theAnimal_breed,
            @Description(shortDefinition="The species for animal patients")
            @OptionalParam(name="animal-species")
                    TokenAndListParam theAnimal_species,
            @Description(shortDefinition="The patient's date of birth")
            @OptionalParam(name="birthdate")
                    DateRangeParam theBirthdate,
            @Description(shortDefinition="The date of death has been provided and satisfies this search value")
            @OptionalParam(name="death-date")
                    DateRangeParam theDeath_date,
            @Description(shortDefinition="This patient has been marked as deceased, or as a death date entered")
            @OptionalParam(name="deceased")
                    TokenAndListParam theDeceased,
            @Description(shortDefinition="A value in an email contact")
            @OptionalParam(name="email")
                    TokenAndListParam theEmail,
            @Description(shortDefinition="A portion of the family name of the patient")
            @OptionalParam(name="family")
                    StringAndListParam theFamily,
            @Description(shortDefinition="Gender of the patient")
            @OptionalParam(name="gender")
                    TokenAndListParam theGender,
            @Description(shortDefinition="Patient's nominated general practitioner, not the organization that manages the record")
            @OptionalParam(name="general-practitioner", targetTypes={  } )
                    ReferenceAndListParam theGeneral_practitioner,
            @Description(shortDefinition="A portion of the given name of the patient")
            @OptionalParam(name="given")
                    StringAndListParam theGiven,
            @Description(shortDefinition="A patient identifier")
            @OptionalParam(name="identifier")
                    TokenAndListParam theIdentifier,
            @Description(shortDefinition="Language code (irrespective of use value)")
            @OptionalParam(name="language")
                    TokenAndListParam theLanguage,
            @Description(shortDefinition="All patients linked to the given patient")
            @OptionalParam(name="link", targetTypes={  } )
                    ReferenceAndListParam theLink,
            @Description(shortDefinition="A server defined search that may match any of the string fields in the HumanName, including family, give, prefix, suffix, suffix, and/or text")
            @OptionalParam(name="name")
                    StringAndListParam theName,
            @Description(shortDefinition="The organization at which this person is a patient")
            @OptionalParam(name="organization", targetTypes={  } )
                    ReferenceAndListParam theOrganization,
            @Description(shortDefinition="A value in a phone contact")
            @OptionalParam(name="phone")
                    TokenAndListParam thePhone,
            @Description(shortDefinition="A portion of either family or given name using some kind of phonetic matching algorithm")
            @OptionalParam(name="phonetic")
                    StringAndListParam thePhonetic,
            @Description(shortDefinition="The value in any kind of telecom details of the patient")
            @OptionalParam(name="telecom")
                    TokenAndListParam theTelecom,
            @RawParam
                    Map<String, List<String>> theAdditionalRawParams,
            @IncludeParam(reverse=true)
                    Set<Include> theRevIncludes,
            @Description(shortDefinition="Only return resources which were last updated as specified by the given range")
            @OptionalParam(name="_lastUpdated")
                    DateRangeParam theLastUpdated,
            @IncludeParam(allow= {
                    "Patient:general-practitioner" , 					"Patient:link" , 					"Patient:organization" , 						"Patient:general-practitioner" , 					"Patient:link" , 					"Patient:organization" , 						"Patient:general-practitioner" , 					"Patient:link" , 					"Patient:organization" 					, "*"
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
            paramMap.add("active", theActive);
            paramMap.add("address", theAddress);
            paramMap.add("address-city", theAddress_city);
            paramMap.add("address-country", theAddress_country);
            paramMap.add("address-postalcode", theAddress_postalcode);
            paramMap.add("address-state", theAddress_state);
            paramMap.add("address-use", theAddress_use);
            paramMap.add("animal-breed", theAnimal_breed);
            paramMap.add("animal-species", theAnimal_species);
            paramMap.add("birthdate", theBirthdate);
            paramMap.add("death-date", theDeath_date);
            paramMap.add("deceased", theDeceased);
            paramMap.add("email", theEmail);
            paramMap.add("family", theFamily);
            paramMap.add("gender", theGender);
            paramMap.add("general-practitioner", theGeneral_practitioner);
            paramMap.add("given", theGiven);
            paramMap.add("identifier", theIdentifier);
            paramMap.add("language", theLanguage);
            paramMap.add("link", theLink);
            paramMap.add("name", theName);
            paramMap.add("organization", theOrganization);
            paramMap.add("phone", thePhone);
            paramMap.add("phonetic", thePhonetic);
            paramMap.add("telecom", theTelecom);
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
