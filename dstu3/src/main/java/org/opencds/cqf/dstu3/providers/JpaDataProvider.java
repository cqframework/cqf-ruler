package org.opencds.cqf.dstu3.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.model.api.IQueryParameterAnd;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.IResourceProvider;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.search.BooleanQuery;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.data.fhir.FhirDataProviderStu3;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.terminology.ValueSetInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

public class JpaDataProvider extends FhirDataProviderStu3 {

    // need these to access the dao
    private Collection<IResourceProvider> collectionProviders;

    private static final int MAX_CODES_PER_QUERY = 1024;

    public synchronized Collection<IResourceProvider> getCollectionProviders() {
        return this.collectionProviders;
    }

    public JpaDataProvider(Collection<IResourceProvider> providers) {
        this.collectionProviders = providers;
        setPackageName("org.hl7.fhir.dstu3.model");
        setFhirContext(FhirContext.forDstu3());
    }

    public synchronized Iterable<Object> retrieve(String context, Object contextValue, String dataType, String templateId,
                                     String codePath, Iterable<Code> codes, String valueSet, String datePath,
                                     String dateLowPath, String dateHighPath, Interval dateRange)
    {
        if (dataType == null) {
            throw new IllegalArgumentException("A data type (i.e. Procedure, Valueset, etc...) must be specified for clinical data retrieval");
        }

        List<SearchParameterMap> queries = this.setupQueries(context, contextValue, dataType, 
            templateId, codePath, codes, valueSet, datePath, dateLowPath, dateHighPath, dateRange);

        return this.executeQueries(dataType, queries);
    }

    protected Pair<String, IQueryParameterType> getTemplateParam(String dataType, String templateId) {
        if (templateId == null || templateId.equals("")) {
            return null;
        }

        // Do something?
        return null;
    }

    protected Pair<String, DateRangeParam> getDateRangeParam(String dataType, String datePath, String dateLowPath, String dateHighPath, Interval dateRange) 
    {
        if (dateRange == null) {
            return null;
        }

        DateParam low = null;
        DateParam high = null;
        if (dateRange.getLow() != null) {
            low = new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, Date.from(((DateTime) dateRange.getLow()).getDateTime().toInstant()));
        }

        if (dateRange.getHigh() != null) {
            high = new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, Date.from(((DateTime) dateRange.getHigh()).getDateTime().toInstant()));
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

        return Pair.of(convertPathToSearchParam(dataType, datePath), rangeParam);
    }

    protected Pair<String, ReferenceParam> getContextParam(String dataType, String context, Object contextValue) {
        if (context != null && context.equals("Patient") && contextValue != null) {
            if (isPatientCompartment(dataType)) {
                ReferenceParam patientParam = new ReferenceParam(contextValue.toString());
                return Pair.of(getPatientSearchParam(dataType), patientParam);
            }
        }

        return null;
    }

    protected Pair<String, List<TokenOrListParam>> getCodeParams(String dataType, String codePath, Iterable<Code> codes, String valueSet) {
        if (valueSet != null && valueSet.startsWith("urn:oid:")) {
            valueSet = valueSet.replace("urn:oid:", "");
        }

        if (codePath == null && (codes != null || valueSet != null)) {
            throw new IllegalArgumentException("A code path must be provided when filtering on codes or a valueset.");
        }

        if (codePath == null || codePath.isEmpty()) {
            return null;
        }

        List<TokenOrListParam> codeParamLists = this.getCodeParams(codePath, codes, valueSet);
        if (codeParamLists == null || codeParamLists.isEmpty()) {
            return null;
        }

        return Pair.of(convertPathToSearchParam(dataType, codePath), codeParamLists);
    }

    protected List<TokenOrListParam> getCodeParams(String codePath, Iterable<Code> codes, String valueSet) {
        if (valueSet != null) {
            if (expandValueSets) {
                if (terminologyProvider == null) {
                    throw new IllegalArgumentException("Expand value sets cannot be used without a terminology provider and no terminology provider is set.");
                }
                ValueSetInfo valueSetInfo = new ValueSetInfo().withId(valueSet);
                codes = terminologyProvider.expand(valueSetInfo);
            }
            else {
                return Collections.singletonList(new TokenOrListParam().addOr(new TokenParam(null, valueSet).setModifier(TokenParamModifier.IN)));
            }
        }

        if (codes == null) {
            return Collections.emptyList();
        }

        List<TokenOrListParam> codeParamsList = new ArrayList<>();

        TokenOrListParam codeParams = null;
        int codeCount = 0;
        for (Code code : codes) {
            if (codeCount % MAX_CODES_PER_QUERY == 0) {
                if (codeParams != null) {
                    codeParamsList.add(codeParams);
                }

                codeParams = new TokenOrListParam();
            }

            codeCount++;
            codeParams.addOr(new TokenParam(code.getSystem(), code.getCode()));
        }

        if (codeParams != null) {
            codeParamsList.add(codeParams);
        }

        return codeParamsList;
    }

    protected List<SearchParameterMap> setupQueries(String context, Object contextValue, String dataType, String templateId,
    String codePath, Iterable<Code> codes, String valueSet, String datePath,
    String dateLowPath, String dateHighPath, Interval dateRange) {

        Pair<String, IQueryParameterType> templateParam = this.getTemplateParam(dataType, templateId);
        Pair<String, ReferenceParam> contextParam = this.getContextParam(dataType, context, contextValue);
        Pair<String, DateRangeParam> dateRangeParam = this.getDateRangeParam(dataType, datePath, dateLowPath, dateHighPath, dateRange);
        Pair<String, List<TokenOrListParam>> codeParams = this.getCodeParams(dataType, codePath, codes, valueSet);

        return this.innerSetupQueries(templateParam, contextParam, dateRangeParam, codeParams);
    }

    protected List<SearchParameterMap> innerSetupQueries(
        Pair<String, IQueryParameterType> templateParam,
        Pair<String, ReferenceParam> contextParam,
        Pair<String, DateRangeParam> dateRangeParam,
        Pair<String, List<TokenOrListParam>> codeParams) {

        if (codeParams == null || codeParams.getValue() == null || codeParams.getValue().isEmpty()) {
            return Collections.singletonList(this.getBaseMap(templateParam, contextParam, dateRangeParam));
        }

        List<SearchParameterMap> maps = new ArrayList<>();
        for (TokenOrListParam tolp: codeParams.getValue())
        {
            SearchParameterMap base = this.getBaseMap(templateParam, contextParam, dateRangeParam);
            base.add(codeParams.getKey(), tolp);
            maps.add(base);
        }

        return maps;
    }

    protected SearchParameterMap getBaseMap(        
        Pair<String, IQueryParameterType> templateParam,
        Pair<String, ReferenceParam> contextParam,
        Pair<String, DateRangeParam> dateRangeParam
    ) {

        SearchParameterMap baseMap = new SearchParameterMap();
        baseMap.setLastUpdated(new DateRangeParam());

        if (templateParam != null) {
            baseMap.add(templateParam.getKey(), templateParam.getValue());
        }

        if (dateRangeParam != null) {
            baseMap.add(dateRangeParam.getKey(), dateRangeParam.getValue());
        }

        if (contextParam != null) {
            baseMap.add(contextParam.getKey(), contextParam.getValue());
        }

        return baseMap;
    }

    protected Iterable<Object> executeQueries(String dataType, List<SearchParameterMap> queries) {

        if (queries == null || queries.isEmpty()) {
            return Collections.emptyList();
        }

        JpaResourceProviderDstu3<? extends IAnyResource> jpaResProvider = resolveResourceProvider(dataType);

        List<Object> objects = new ArrayList<>();
        for (SearchParameterMap map : queries) {
            objects.addAll(executeQuery(jpaResProvider, map));
        }

        return objects;
    }

    protected Collection<Object> executeQuery(JpaResourceProviderDstu3<? extends IAnyResource> resourceProvider, SearchParameterMap map) {
        IBundleProvider bundleProvider = resourceProvider.getDao().search(map);
        if (bundleProvider.size() == null)
        {
            return resolveResourceList(bundleProvider.getResources(0, 10000));
        }
        if (bundleProvider.size() == 0) {
            return new ArrayList<>();
        }
        List<IBaseResource> resourceList = bundleProvider.getResources(0, bundleProvider.size());
        return resolveResourceList(resourceList);
    }

    public synchronized Collection<Object> resolveResourceList(List<IBaseResource> resourceList) {
        List<Object> ret = new ArrayList<>();
        for (IBaseResource res : resourceList) {
            Class clazz = res.getClass();
            ret.add(clazz.cast(res));
        }
        // ret.addAll(resourceList);
        return ret;
    }

    public synchronized JpaResourceProviderDstu3<? extends IAnyResource> resolveResourceProvider(String datatype) {
        for (IResourceProvider resource : collectionProviders) {
            if (resource.getResourceType().getSimpleName().toLowerCase().equals(datatype.toLowerCase())) {
                return (JpaResourceProviderDstu3<? extends IAnyResource>) resource;
            }
        }
        throw new RuntimeException("Could not find resource provider for type: " + datatype);
    }

    @Override
    protected String convertPathToSearchParam(String type, String path) {
        switch (type) {
            case "Coverage":
                if (path.equals("patient")) return "beneficiary";
        }

        return super.convertPathToSearchParam(type, path);
    }

    @Override
    public String getPatientSearchParam(String dataType) {
        switch (dataType) {
            case "Coverage":
                return "beneficiary";
        }

        return super.getPatientSearchParam(dataType);
    }

    private boolean isPatientCompartment(String dataType)
    {
        switch (dataType)
        {
            case "Account":
            case "AdverseEvent":
            case "AllergyIntolerance":
            case "Appointment":
            case "AppointmentResponse":
            case "AuditEvent":
            case "Basic":
            case "BodySite":
            case "CarePlan":
            case "CareTeam":
            case "ChargeItem":
            case "Claim":
            case "ClaimResponse":
            case "ClinicalImpression":
            case "Communication":
            case "CommunicationRequest":
            case "Composition":
            case "Condition":
            case "Consent":
            case "Coverage":
            case "DetectedIssue":
            case "DeviceRequest":
            case "DeviceUseStatement":
            case "DiagnosticReport":
            case "DocumentManifest":
            case "EligibilityRequest":
            case "Encounter":
            case "EnrollmentRequest":
            case "EpisodeOfCare":
            case "ExplanationOfBenefit":
            case "FamilyMemberHistory":
            case "Flag":
            case "Goal":
            case "Group":
            case "ImagingManifest":
            case "ImagingStudy":
            case "Immunization":
            case "ImmunizationRecommendation":
            case "List":
            case "MeasureReport":
            case "Media":
            case "MedicationAdministration":
            case "MedicationDispense":
            case "MedicationRequest":
            case "MedicationStatement":
            case "NutritionOrder":
            case "Observation":
            case "Patient":
            case "Person":
            case "Procedure":
            case "ProcedureRequest":
            case "Provenance":
            case "QuestionnaireResponse":
            case "ReferralRequest":
            case "RelatedPerson":
            case "RequestGroup":
            case "ResearchSubject":
            case "RiskAssessment":
            case "Schedule":
            case "Specimen":
            case "SupplyDelivery":
            case "SupplyRequest":
            case "VisionPrescription": return true;
            default: return false;
        }
    }
}
