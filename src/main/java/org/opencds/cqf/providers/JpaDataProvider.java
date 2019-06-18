package org.opencds.cqf.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
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

import java.util.*;

public class JpaDataProvider extends FhirDataProviderStu3 {

    // need these to access the dao
    private Collection<IResourceProvider> collectionProviders;

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
            if (isPatientCompartment(dataType))
            {
                ReferenceParam patientParam = new ReferenceParam(contextValue.toString());
                map.add(getPatientSearchParam(dataType), patientParam);
            }
        }

        if (codePath != null && !codePath.equals("")) {

            if (valueSet != null && terminologyProvider != null && expandValueSets) {
                ValueSetInfo valueSetInfo = new ValueSetInfo().withId(valueSet);
                codes = terminologyProvider.expand(valueSetInfo);
            }
            else if (valueSet != null) {
                map.add(convertPathToSearchParam(dataType, codePath), new TokenParam(null, valueSet).setModifier(TokenParamModifier.IN));
            }
            if (codes != null) {
                TokenOrListParam codeParams = new TokenOrListParam();
                for (Code code : codes) {
                    codeParams.addOr(new TokenParam(code.getSystem(), code.getCode()));
                }
                map.add(convertPathToSearchParam(dataType, codePath), codeParams);
            }
        }

        if (dateRange != null) {
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

            map.add(convertPathToSearchParam(dataType, datePath), rangeParam);
        }

        JpaResourceProviderDstu3<? extends IAnyResource> jpaResProvider = resolveResourceProvider(dataType);
        IBundleProvider bundleProvider = jpaResProvider.getDao().search(map);
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

    public synchronized Iterable<Object> resolveResourceList(List<IBaseResource> resourceList) {
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
