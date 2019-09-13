package org.opencds.cqf.r4.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.provider.r4.JpaResourceProviderR4;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.apache.lucene.search.BooleanQuery;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.data.fhir.FhirDataProviderR4;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.terminology.ValueSetInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class JpaDataProvider extends FhirDataProviderR4 {

    // need these to access the dao
    private Collection<IResourceProvider> collectionProviders;

    public synchronized Collection<IResourceProvider> getCollectionProviders() {
        return this.collectionProviders;
    }

    public JpaDataProvider(Collection<IResourceProvider> providers) {
        this.collectionProviders = providers;
        setPackageName("org.hl7.fhir.r4.model");
        setFhirContext(FhirContext.forR4());
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

        boolean noResults = false;
        if (codePath != null && !codePath.equals("")) {

            if (valueSet != null) {
                if (expandValueSets) {
                    if (terminologyProvider == null) {
                        throw new IllegalArgumentException("Expand value sets cannot be used without a terminology provider and no terminology provider is set.");
                    }
                    ValueSetInfo valueSetInfo = new ValueSetInfo().withId(valueSet);
                    codes = terminologyProvider.expand(valueSetInfo);
                }
                else {
                    map.add(convertPathToSearchParam(dataType, codePath), new TokenParam(null, valueSet).setModifier(TokenParamModifier.IN));
                }
            }

            if (codes != null) {
                TokenOrListParam codeParams = new TokenOrListParam();
                int codeCount = 0;
                for (Code code : codes) {
                    codeCount++;
                    codeParams.addOr(new TokenParam(code.getSystem(), code.getCode()));
                }
                map.add(convertPathToSearchParam(dataType, codePath), codeParams);
                if (codeCount == 0) {
                    noResults = true;
                }
                if (codeCount > 1023) {
                    BooleanQuery.setMaxClauseCount(codeCount);
                }
            }
        }

        // If the retrieve is filtered to a value set that has no codes, there are no possible satisfying results, don't even search, just return empty
        if (noResults) {
            return new ArrayList();
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

        JpaResourceProviderR4<? extends IAnyResource> jpaResProvider = resolveResourceProvider(dataType);
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

    public synchronized JpaResourceProviderR4<? extends IAnyResource> resolveResourceProvider(String datatype) {
        for (IResourceProvider resource : collectionProviders) {
            if (resource.getResourceType().getSimpleName().toLowerCase().equals(datatype.toLowerCase())) {
                return (JpaResourceProviderR4<? extends IAnyResource>) resource;
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
            case "BodyStructure":
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
            case "CoverageEligibilityRequest":
            case "CoverageEligibilityResponse":
            case "DetectedIssue":
            case "DeviceRequest":
            case "DeviceUseStatement":
            case "DiagnosticReport":
            case "DocumentManifest":
            case "DocumentReference":
            case "Encounter":
            case "EnrollmentRequest":
            case "EpisodeOfCare":
            case "ExplanationOfBenefit":
            case "FamilyMemberHistory":
            case "Flag":
            case "Goal":
            case "Group":
            case "ImagingStudy":
            case "Immunization":
            case "ImmunizationEvaluation":
            case "ImmunizationRecommendation":
            case "Invoice":
            case "List":
            case "MeasureReport":
            case "Media":
            case "MedicationAdministration":
            case "MedicationDispense":
            case "MedicationRequest":
            case "MedicationStatement":
            case "MolecularSequence":
            case "NutritionOrder":
            case "Observation":
            case "Patient":
            case "Person":
            case "Procedure":
            case "Provenance":
            case "QuestionnaireResponse":
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

    @Override
    protected String getContextSearchParam(String contextType, String dataType) {
        switch (contextType) {
            case "Device":
                switch (dataType) {
                    case "Account":
                        return "subject";
                    case "ActivityDefinition":
                        break;
                    case "AdverseEvent":
                        break;
                    case "AllergyIntolerance":
                        break;
                    case "Appointment":
                        return "actor";
                    case "AppointmentResponse":
                        return "actor";
                    case "AuditEvent":
                        return "agent";
                    case "Basic":
                        break;
                    case "Binary":
                        break;
                    case "BiologicallyDerivedProduct":
                        break;
                    case "BodyStructure":
                        break;
                    case "Bundle":
                        break;
                    case "CapabilityStatement":
                        break;
                    case "CarePlan":
                        break;
                    case "CareTeam":
                        break;
                    case "CatalogEntry":
                        break;
                    case "ChargeItem":
                        break;
                    case "ChargeItemDefinition":
                        break;
                    case "Claim":
                        break;
                    case "ClaimResponse":
                        break;
                    case "ClinicalImpression":
                        break;
                    case "CodeSystem":
                        break;
                    case "Communication":
                        break;
                    case "CommunicationRequest":
                        break;
                    case "CompartmentDefinition":
                        break;
                    case "Composition":
                        return "author";
                    case "ConceptMap":
                        break;
                    case "Condition":
                        break;
                    case "Consent":
                        break;
                    case "Contract":
                        break;
                    case "Coverage":
                        break;
                    case "CoverageEligibilityRequest":
                        break;
                    case "CoverageEligibilityResponse":
                        break;
                    case "DetectedIssue":
                        return "author";
                    case "Device":
                        break;
                    case "DeviceDefinition":
                        break;
                    case "DeviceMetric":
                        break;
                    case "DeviceRequest":
                        break;
                    case "DeviceUseStatement":
                        return "device";
                    case "DiagnosticReport":
                        return "subject";
                    case "DocumentManifest":
                        break;
                    case "DocumentReference":
                        break;
                    case "EffectEvidenceSynthesis":
                        break;
                    case "Encounter":
                        break;
                    case "Endpoint":
                        break;
                    case "EnrollmentRequest":
                        break;
                    case "EnrollmentResponse":
                        break;
                    case "EpisodeOfCare":
                        break;
                    case "EventDefinition":
                        break;
                    case "Evidence":
                        break;
                    case "EvidenceVariable":
                        break;
                    case "ExampleScenario":
                        break;
                    case "ExplanationOfBenefit":
                        break;
                    case "FamilyMemberHistory":
                        break;
                    case "Flag":
                        return "author";
                    case "Goal":
                        break;
                    case "GraphDefinition":
                        break;
                    case "Group":
                        return "member";
                    case "GuidanceResponse":
                        break;
                    case "HealthcareService":
                        break;
                    case "ImagingStudy":
                        break;
                    case "Immunization":
                        break;
                    case "ImmunizationEvaluation":
                        break;
                    case "ImmunizationRecommendation":
                        break;
                    case "ImplementationGuide":
                        break;
                    case "InsurancePlan":
                        break;
                    case "Invoice":
                        return "participant";
                    case "Library":
                        break;
                    case "Linkage":
                        break;
                    case "List":
                        break;
                    case "Location":
                        break;
                    case "Measure":
                        break;
                    case "MeasureReport":
                        break;
                    case "Media":
                        return "subject";
                    case "Medication":
                        break;
                    case "MedicationAdministration":
                        return "device";
                    case "MedicationDispense":
                        break;
                    case "MedicationKnowledge":
                        break;
                    case "MedicationRequest":
                        break;
                    case "MedicationStatement":
                        break;
                    case "MedicinalProduct":
                        break;
                    case "MedicinalProductAuthorization":
                        break;
                    case "MedicinalProductContraindication":
                        break;
                    case "MedicinalProductIndication":
                        break;
                    case "MedicinalProductIngredient":
                        break;
                    case "MedicinalProductInteraction":
                        break;
                    case "MedicinalProductManufactured":
                        break;
                    case "MedicinalProductPackaged":
                        break;
                    case "MedicinalProductPharmaceutical":
                        break;
                    case "MedicinalProductUndesirableEffect":
                        break;
                    case "MessageDefinition":
                        break;
                    case "MessageHeader":
                        return "target";
                    case "MolecularSequence":
                        break;
                    case "NamingSystem":
                        break;
                    case "NutritionOrder":
                        break;
                    case "Observation":
                        break;
                    case "ObservationDefinition":
                        break;
                    case "OperationDefinition":
                        break;
                    case "OperationOutcome":
                        break;
                    case "Organization":
                        break;
                    case "OrganizationAffiliation":
                        break;
                    case "Patient":
                        break;
                    case "PaymentNotice":
                        break;
                    case "PaymentReconciliation":
                        break;
                    case "Person":
                        break;
                    case "PlanDefinition":
                        break;
                    case "Practitioner":
                        break;
                    case "PractitionerRole":
                        break;
                    case "Procedure":
                        break;
                    case "Provenance":
                        return "agent";
                    case "Questionnaire":
                        break;
                    case "QuestionnaireResponse":
                        return "author";
                    case "RelatedPerson":
                        break;
                    case "RequestGroup":
                        return "author";
                    case "ResearchDefinition":
                        break;
                    case "ResearchElementDefinition":
                        break;
                    case "ResearchStudy":
                        break;
                    case "ResearchSubject":
                        break;
                    case "RiskAssessment":
                        return "performer";
                    case "RiskEvidenceSynthesis":
                        break;
                    case "Schedule":
                        return "actor";
                    case "SearchParameter":
                        break;
                    case "ServiceRequest":
                        break;
                    case "Slot":
                        break;
                    case "Specimen":
                        return "subject";
                    case "SpecimenDefinition":
                        break;
                    case "StructureDefinition":
                        break;
                    case "StructureMap":
                        break;
                    case "Subscription":
                        break;
                    case "Substance":
                        break;
                    case "SubstanceNucleicAcid":
                        break;
                    case "SubstancePolymer":
                        break;
                    case "SubstanceProtein":
                        break;
                    case "SubstanceReferenceInformation":
                        break;
                    case "SubstanceSourceMaterial":
                        break;
                    case "SubstanceSpecification":
                        break;
                    case "SupplyDelivery":
                        break;
                    case "SupplyRequest":
                        return "requester";
                    case "Task":
                        break;
                    case "TerminologyCapabilities":
                        break;
                    case "TestReport":
                        break;
                    case "TestScript":
                        break;
                    case "ValueSet":
                        break;
                    case "VerificationResult":
                        break;
                    case "VisionPrescription":
                        break;
                }
                break;
            case "Encounter":
                switch (dataType) {
                    case "Account":
                        break;
                    case "ActivityDefinition":
                        break;
                    case "AdverseEvent":
                        break;
                    case "AllergyIntolerance":
                        break;
                    case "Appointment":
                        break;
                    case "AppointmentResponse":
                        break;
                    case "AuditEvent":
                        break;
                    case "Basic":
                        break;
                    case "Binary":
                        break;
                    case "BiologicallyDerivedProduct":
                        break;
                    case "BodyStructure":
                        break;
                    case "Bundle":
                        break;
                    case "CapabilityStatement":
                        break;
                    case "CarePlan":
                        return "encounter";
                    case "CareTeam":
                        return "encounter";
                    case "CatalogEntry":
                        break;
                    case "ChargeItem":
                        return "context";
                    case "ChargeItemDefinition":
                        break;
                    case "Claim":
                        return "encounter";
                    case "ClaimResponse":
                        break;
                    case "ClinicalImpression":
                        return "encounter";
                    case "CodeSystem":
                        break;
                    case "Communication":
                        return "encounter";
                    case "CommunicationRequest":
                        return "encounter";
                    case "CompartmentDefinition":
                        break;
                    case "Composition":
                        return "encounter";
                    case "ConceptMap":
                        break;
                    case "Condition":
                        return "encounter";
                    case "Consent":
                        break;
                    case "Contract":
                        break;
                    case "Coverage":
                        break;
                    case "CoverageEligibilityRequest":
                        break;
                    case "CoverageEligibilityResponse":
                        break;
                    case "DetectedIssue":
                        break;
                    case "Device":
                        break;
                    case "DeviceDefinition":
                        break;
                    case "DeviceMetric":
                        break;
                    case "DeviceRequest":
                        return "encounter";
                    case "DeviceUseStatement":
                        break;
                    case "DiagnosticReport":
                        return "encounter";
                    case "DocumentManifest":
                        return "related-ref";
                    case "DocumentReference":
                        return "encounter";
                    case "EffectEvidenceSynthesis":
                        break;
                    case "Encounter":
                        return "{def}";
                    case "Endpoint":
                        break;
                    case "EnrollmentRequest":
                        break;
                    case "EnrollmentResponse":
                        break;
                    case "EpisodeOfCare":
                        break;
                    case "EventDefinition":
                        break;
                    case "Evidence":
                        break;
                    case "EvidenceVariable":
                        break;
                    case "ExampleScenario":
                        break;
                    case "ExplanationOfBenefit":
                        return "encounter";
                    case "FamilyMemberHistory":
                        break;
                    case "Flag":
                        break;
                    case "Goal":
                        break;
                    case "GraphDefinition":
                        break;
                    case "Group":
                        break;
                    case "GuidanceResponse":
                        break;
                    case "HealthcareService":
                        break;
                    case "ImagingStudy":
                        break;
                    case "Immunization":
                        break;
                    case "ImmunizationEvaluation":
                        break;
                    case "ImmunizationRecommendation":
                        break;
                    case "ImplementationGuide":
                        break;
                    case "InsurancePlan":
                        break;
                    case "Invoice":
                        break;
                    case "Library":
                        break;
                    case "Linkage":
                        break;
                    case "List":
                        break;
                    case "Location":
                        break;
                    case "Measure":
                        break;
                    case "MeasureReport":
                        break;
                    case "Media":
                        return "encounter";
                    case "Medication":
                        break;
                    case "MedicationAdministration":
                        return "context";
                    case "MedicationDispense":
                        break;
                    case "MedicationKnowledge":
                        break;
                    case "MedicationRequest":
                        return "encounter";
                    case "MedicationStatement":
                        break;
                    case "MedicinalProduct":
                        break;
                    case "MedicinalProductAuthorization":
                        break;
                    case "MedicinalProductContraindication":
                        break;
                    case "MedicinalProductIndication":
                        break;
                    case "MedicinalProductIngredient":
                        break;
                    case "MedicinalProductInteraction":
                        break;
                    case "MedicinalProductManufactured":
                        break;
                    case "MedicinalProductPackaged":
                        break;
                    case "MedicinalProductPharmaceutical":
                        break;
                    case "MedicinalProductUndesirableEffect":
                        break;
                    case "MessageDefinition":
                        break;
                    case "MessageHeader":
                        break;
                    case "MolecularSequence":
                        break;
                    case "NamingSystem":
                        break;
                    case "NutritionOrder":
                        return "encounter";
                    case "Observation":
                        return "encounter";
                    case "ObservationDefinition":
                        break;
                    case "OperationDefinition":
                        break;
                    case "OperationOutcome":
                        break;
                    case "Organization":
                        break;
                    case "OrganizationAffiliation":
                        break;
                    case "Patient":
                        break;
                    case "PaymentNotice":
                        break;
                    case "PaymentReconciliation":
                        break;
                    case "Person":
                        break;
                    case "PlanDefinition":
                        break;
                    case "Practitioner":
                        break;
                    case "PractitionerRole":
                        break;
                    case "Procedure":
                        return "encounter";
                    case "Provenance":
                        break;
                    case "Questionnaire":
                        break;
                    case "QuestionnaireResponse":
                        return "encounter";
                    case "RelatedPerson":
                        break;
                    case "RequestGroup":
                        return "encounter";
                    case "ResearchDefinition":
                        break;
                    case "ResearchElementDefinition":
                        break;
                    case "ResearchStudy":
                        break;
                    case "ResearchSubject":
                        break;
                    case "RiskAssessment":
                        break;
                    case "RiskEvidenceSynthesis":
                        break;
                    case "Schedule":
                        break;
                    case "SearchParameter":
                        break;
                    case "ServiceRequest":
                        return "encounter";
                    case "Slot":
                        break;
                    case "Specimen":
                        break;
                    case "SpecimenDefinition":
                        break;
                    case "StructureDefinition":
                        break;
                    case "StructureMap":
                        break;
                    case "Subscription":
                        break;
                    case "Substance":
                        break;
                    case "SubstanceNucleicAcid":
                        break;
                    case "SubstancePolymer":
                        break;
                    case "SubstanceProtein":
                        break;
                    case "SubstanceReferenceInformation":
                        break;
                    case "SubstanceSourceMaterial":
                        break;
                    case "SubstanceSpecification":
                        break;
                    case "SupplyDelivery":
                        break;
                    case "SupplyRequest":
                        break;
                    case "Task":
                        break;
                    case "TerminologyCapabilities":
                        break;
                    case "TestReport":
                        break;
                    case "TestScript":
                        break;
                    case "ValueSet":
                        break;
                    case "VerificationResult":
                        break;
                    case "VisionPrescription":
                        return "encounter";
                }
                break;
            case "Patient":
                switch (dataType) {
                    case "Account":
                        return "subject";
                    case "ActivityDefinition":
                        break;
                    case "AdverseEvent":
                        return "subject";
                    case "AllergyIntolerance":
                        break;
                    case "Appointment":
                        return "actor";
                    case "AppointmentResponse":
                        return "actor";
                    case "AuditEvent":
                        return "patient";
                    case "Basic":
                        break;
                    case "Binary":
                        break;
                    case "BiologicallyDerivedProduct":
                        break;
                    case "BodyStructure":
                        return "patient";
                    case "Bundle":
                        break;
                    case "CapabilityStatement":
                        break;
                    case "CarePlan":
                        break;
                    case "CareTeam":
                        break;
                    case "CatalogEntry":
                        break;
                    case "ChargeItem":
                        return "subject";
                    case "ChargeItemDefinition":
                        break;
                    case "Claim":
                        break;
                    case "ClaimResponse":
                        return "patient";
                    case "ClinicalImpression":
                        return "subject";
                    case "CodeSystem":
                        break;
                    case "Communication":
                        break;
                    case "CommunicationRequest":
                        break;
                    case "CompartmentDefinition":
                        break;
                    case "Composition":
                        break;
                    case "ConceptMap":
                        break;
                    case "Condition":
                        return "subject";
                    case "Consent":
                        return "patient";
                    case "Contract":
                        break;
                    case "Coverage":
                        break;
                    case "CoverageEligibilityRequest":
                        return "patient";
                    case "CoverageEligibilityResponse":
                        return "patient";
                    case "DetectedIssue":
                        return "patient";
                    case "Device":
                        break;
                    case "DeviceDefinition":
                        break;
                    case "DeviceMetric":
                        break;
                    case "DeviceRequest":
                        break;
                    case "DeviceUseStatement":
                        return "subject";
                    case "DiagnosticReport":
                        return "subject";
                    case "DocumentManifest":
                        break;
                    case "DocumentReference":
                        break;
                    case "EffectEvidenceSynthesis":
                        break;
                    case "Encounter":
                        return "patient";
                    case "Endpoint":
                        break;
                    case "EnrollmentRequest":
                        return "subject";
                    case "EnrollmentResponse":
                        break;
                    case "EpisodeOfCare":
                        return "patient";
                    case "EventDefinition":
                        break;
                    case "Evidence":
                        break;
                    case "EvidenceVariable":
                        break;
                    case "ExampleScenario":
                        break;
                    case "ExplanationOfBenefit":
                        break;
                    case "FamilyMemberHistory":
                        return "patient";
                    case "Flag":
                        return "patient";
                    case "Goal":
                        return "patient";
                    case "GraphDefinition":
                        break;
                    case "Group":
                        return "member";
                    case "GuidanceResponse":
                        break;
                    case "HealthcareService":
                        break;
                    case "ImagingStudy":
                        return "patient";
                    case "Immunization":
                        return "patient";
                    case "ImmunizationEvaluation":
                        return "patient";
                    case "ImmunizationRecommendation":
                        return "patient";
                    case "ImplementationGuide":
                        break;
                    case "InsurancePlan":
                        break;
                    case "Invoice":
                        break;
                    case "Library":
                        break;
                    case "Linkage":
                        break;
                    case "List":
                        break;
                    case "Location":
                        break;
                    case "Measure":
                        break;
                    case "MeasureReport":
                        return "patient";
                    case "Media":
                        return "subject";
                    case "Medication":
                        break;
                    case "MedicationAdministration":
                        break;
                    case "MedicationDispense":
                        break;
                    case "MedicationKnowledge":
                        break;
                    case "MedicationRequest":
                        return "subject";
                    case "MedicationStatement":
                        return "subject";
                    case "MedicinalProduct":
                        break;
                    case "MedicinalProductAuthorization":
                        break;
                    case "MedicinalProductContraindication":
                        break;
                    case "MedicinalProductIndication":
                        break;
                    case "MedicinalProductIngredient":
                        break;
                    case "MedicinalProductInteraction":
                        break;
                    case "MedicinalProductManufactured":
                        break;
                    case "MedicinalProductPackaged":
                        break;
                    case "MedicinalProductPharmaceutical":
                        break;
                    case "MedicinalProductUndesirableEffect":
                        break;
                    case "MessageDefinition":
                        break;
                    case "MessageHeader":
                        break;
                    case "MolecularSequence":
                        return "patient";
                    case "NamingSystem":
                        break;
                    case "NutritionOrder":
                        return "patient";
                    case "Observation":
                        return "subject";
                    case "ObservationDefinition":
                        break;
                    case "OperationDefinition":
                        break;
                    case "OperationOutcome":
                        break;
                    case "Organization":
                        break;
                    case "OrganizationAffiliation":
                        break;
                    case "Patient":
                        return "_id";
                    case "PaymentNotice":
                        break;
                    case "PaymentReconciliation":
                        break;
                    case "Person":
                        return "patient";
                    case "PlanDefinition":
                        break;
                    case "Practitioner":
                        break;
                    case "PractitionerRole":
                        break;
                    case "Procedure":
                        break;
                    case "Provenance":
                        return "patient";
                    case "Questionnaire":
                        break;
                    case "QuestionnaireResponse":
                        break;
                    case "RelatedPerson":
                        return "patient";
                    case "RequestGroup":
                        break;
                    case "ResearchDefinition":
                        break;
                    case "ResearchElementDefinition":
                        break;
                    case "ResearchStudy":
                        break;
                    case "ResearchSubject":
                        return "individual";
                    case "RiskAssessment":
                        return "subject";
                    case "RiskEvidenceSynthesis":
                        break;
                    case "Schedule":
                        return "actor";
                    case "SearchParameter":
                        break;
                    case "ServiceRequest":
                        break;
                    case "Slot":
                        break;
                    case "Specimen":
                        return "subject";
                    case "SpecimenDefinition":
                        break;
                    case "StructureDefinition":
                        break;
                    case "StructureMap":
                        break;
                    case "Subscription":
                        break;
                    case "Substance":
                        break;
                    case "SubstanceNucleicAcid":
                        break;
                    case "SubstancePolymer":
                        break;
                    case "SubstanceProtein":
                        break;
                    case "SubstanceReferenceInformation":
                        break;
                    case "SubstanceSourceMaterial":
                        break;
                    case "SubstanceSpecification":
                        break;
                    case "SupplyDelivery":
                        return "patient";
                    case "SupplyRequest":
                        return "subject";
                    case "Task":
                        break;
                    case "TerminologyCapabilities":
                        break;
                    case "TestReport":
                        break;
                    case "TestScript":
                        break;
                    case "ValueSet":
                        break;
                    case "VerificationResult":
                        break;
                    case "VisionPrescription":
                        return "patient";
                    default:
                        return "patient";
                }
                break;
            case "Practitioner":
                switch (dataType) {
                    case "Account":
                        return "subject";
                    case "ActivityDefinition":
                        break;
                    case "AdverseEvent":
                        return "recorder";
                    case "AllergyIntolerance":
                        break;
                    case "Appointment":
                        return "actor";
                    case "AppointmentResponse":
                        return "actor";
                    case "AuditEvent":
                        return "agent";
                    case "Basic":
                        return "author";
                    case "Binary":
                        break;
                    case "BiologicallyDerivedProduct":
                        break;
                    case "BodyStructure":
                        break;
                    case "Bundle":
                        break;
                    case "CapabilityStatement":
                        break;
                    case "CarePlan":
                        return "performer";
                    case "CareTeam":
                        return "participant";
                    case "CatalogEntry":
                        break;
                    case "ChargeItem":
                        break;
                    case "ChargeItemDefinition":
                        break;
                    case "Claim":
                        break;
                    case "ClaimResponse":
                        return "requestor";
                    case "ClinicalImpression":
                        return "assessor";
                    case "CodeSystem":
                        break;
                    case "Communication":
                        break;
                    case "CommunicationRequest":
                        break;
                    case "CompartmentDefinition":
                        break;
                    case "Composition":
                        break;
                    case "ConceptMap":
                        break;
                    case "Condition":
                        return "asserter";
                    case "Consent":
                        break;
                    case "Contract":
                        break;
                    case "Coverage":
                        break;
                    case "CoverageEligibilityRequest":
                        break;
                    case "CoverageEligibilityResponse":
                        return "requestor";
                    case "DetectedIssue":
                        return "author";
                    case "Device":
                        break;
                    case "DeviceDefinition":
                        break;
                    case "DeviceMetric":
                        break;
                    case "DeviceRequest":
                        break;
                    case "DeviceUseStatement":
                        break;
                    case "DiagnosticReport":
                        return "performer";
                    case "DocumentManifest":
                        break;
                    case "DocumentReference":
                        break;
                    case "EffectEvidenceSynthesis":
                        break;
                    case "Encounter":
                        break;
                    case "Endpoint":
                        break;
                    case "EnrollmentRequest":
                        break;
                    case "EnrollmentResponse":
                        break;
                    case "EpisodeOfCare":
                        return "care-manager";
                    case "EventDefinition":
                        break;
                    case "Evidence":
                        break;
                    case "EvidenceVariable":
                        break;
                    case "ExampleScenario":
                        break;
                    case "ExplanationOfBenefit":
                        break;
                    case "FamilyMemberHistory":
                        break;
                    case "Flag":
                        return "author";
                    case "Goal":
                        break;
                    case "GraphDefinition":
                        break;
                    case "Group":
                        return "member";
                    case "GuidanceResponse":
                        break;
                    case "HealthcareService":
                        break;
                    case "ImagingStudy":
                        break;
                    case "Immunization":
                        return "performer";
                    case "ImmunizationEvaluation":
                        break;
                    case "ImmunizationRecommendation":
                        break;
                    case "ImplementationGuide":
                        break;
                    case "InsurancePlan":
                        break;
                    case "Invoice":
                        return "participant";
                    case "Library":
                        break;
                    case "Linkage":
                        return "author";
                    case "List":
                        return "source";
                    case "Location":
                        break;
                    case "Measure":
                        break;
                    case "MeasureReport":
                        break;
                    case "Media":
                        break;
                    case "Medication":
                        break;
                    case "MedicationAdministration":
                        return "performer";
                    case "MedicationDispense":
                        break;
                    case "MedicationKnowledge":
                        break;
                    case "MedicationRequest":
                        return "requester";
                    case "MedicationStatement":
                        return "source";
                    case "MedicinalProduct":
                        break;
                    case "MedicinalProductAuthorization":
                        break;
                    case "MedicinalProductContraindication":
                        break;
                    case "MedicinalProductIndication":
                        break;
                    case "MedicinalProductIngredient":
                        break;
                    case "MedicinalProductInteraction":
                        break;
                    case "MedicinalProductManufactured":
                        break;
                    case "MedicinalProductPackaged":
                        break;
                    case "MedicinalProductPharmaceutical":
                        break;
                    case "MedicinalProductUndesirableEffect":
                        break;
                    case "MessageDefinition":
                        break;
                    case "MessageHeader":
                        break;
                    case "MolecularSequence":
                        break;
                    case "NamingSystem":
                        break;
                    case "NutritionOrder":
                        return "provider";
                    case "Observation":
                        return "performer";
                    case "ObservationDefinition":
                        break;
                    case "OperationDefinition":
                        break;
                    case "OperationOutcome":
                        break;
                    case "Organization":
                        break;
                    case "OrganizationAffiliation":
                        break;
                    case "Patient":
                        return "general-practitioner";
                    case "PaymentNotice":
                        return "provider";
                    case "PaymentReconciliation":
                        return "requestor";
                    case "Person":
                        return "practitioner";
                    case "PlanDefinition":
                        break;
                    case "Practitioner":
                        return "{def}";
                    case "PractitionerRole":
                        return "practitioner";
                    case "Procedure":
                        return "performer";
                    case "Provenance":
                        return "agent";
                    case "Questionnaire":
                        break;
                    case "QuestionnaireResponse":
                        break;
                    case "RelatedPerson":
                        break;
                    case "RequestGroup":
                        break;
                    case "ResearchDefinition":
                        break;
                    case "ResearchElementDefinition":
                        break;
                    case "ResearchStudy":
                        return "principalinvestigator";
                    case "ResearchSubject":
                        break;
                    case "RiskAssessment":
                        return "performer";
                    case "RiskEvidenceSynthesis":
                        break;
                    case "Schedule":
                        return "actor";
                    case "SearchParameter":
                        break;
                    case "ServiceRequest":
                        break;
                    case "Slot":
                        break;
                    case "Specimen":
                        return "collector";
                    case "SpecimenDefinition":
                        break;
                    case "StructureDefinition":
                        break;
                    case "StructureMap":
                        break;
                    case "Subscription":
                        break;
                    case "Substance":
                        break;
                    case "SubstanceNucleicAcid":
                        break;
                    case "SubstancePolymer":
                        break;
                    case "SubstanceProtein":
                        break;
                    case "SubstanceReferenceInformation":
                        break;
                    case "SubstanceSourceMaterial":
                        break;
                    case "SubstanceSpecification":
                        break;
                    case "SupplyDelivery":
                        break;
                    case "SupplyRequest":
                        return "requester";
                    case "Task":
                        break;
                    case "TerminologyCapabilities":
                        break;
                    case "TestReport":
                        break;
                    case "TestScript":
                        break;
                    case "ValueSet":
                        break;
                    case "VerificationResult":
                        break;
                    case "VisionPrescription":
                        return "prescriber";
                }
                break;
            case "RelatedPerson":
                switch (dataType) {
                    case "Account":
                        break;
                    case "ActivityDefinition":
                        break;
                    case "AdverseEvent":
                        return "recorder";
                    case "AllergyIntolerance":
                        return "asserter";
                    case "Appointment":
                        return "actor";
                    case "AppointmentResponse":
                        return "actor";
                    case "AuditEvent":
                        break;
                    case "Basic":
                        return "author";
                    case "Binary":
                        break;
                    case "BiologicallyDerivedProduct":
                        break;
                    case "BodyStructure":
                        break;
                    case "Bundle":
                        break;
                    case "CapabilityStatement":
                        break;
                    case "CarePlan":
                        return "performer";
                    case "CareTeam":
                        return "participant";
                    case "CatalogEntry":
                        break;
                    case "ChargeItem":
                        break;
                    case "ChargeItemDefinition":
                        break;
                    case "Claim":
                        return "payee";
                    case "ClaimResponse":
                        break;
                    case "ClinicalImpression":
                        break;
                    case "CodeSystem":
                        break;
                    case "Communication":
                        break;
                    case "CommunicationRequest":
                        break;
                    case "CompartmentDefinition":
                        break;
                    case "Composition":
                        return "author";
                    case "ConceptMap":
                        break;
                    case "Condition":
                        return "asserter";
                    case "Consent":
                        break;
                    case "Contract":
                        break;
                    case "Coverage":
                        break;
                    case "CoverageEligibilityRequest":
                        break;
                    case "CoverageEligibilityResponse":
                        break;
                    case "DetectedIssue":
                        break;
                    case "Device":
                        break;
                    case "DeviceDefinition":
                        break;
                    case "DeviceMetric":
                        break;
                    case "DeviceRequest":
                        break;
                    case "DeviceUseStatement":
                        break;
                    case "DiagnosticReport":
                        break;
                    case "DocumentManifest":
                        break;
                    case "DocumentReference":
                        return "author";
                    case "EffectEvidenceSynthesis":
                        break;
                    case "Encounter":
                        return "participant";
                    case "Endpoint":
                        break;
                    case "EnrollmentRequest":
                        break;
                    case "EnrollmentResponse":
                        break;
                    case "EpisodeOfCare":
                        break;
                    case "EventDefinition":
                        break;
                    case "Evidence":
                        break;
                    case "EvidenceVariable":
                        break;
                    case "ExampleScenario":
                        break;
                    case "ExplanationOfBenefit":
                        return "payee";
                    case "FamilyMemberHistory":
                        break;
                    case "Flag":
                        break;
                    case "Goal":
                        break;
                    case "GraphDefinition":
                        break;
                    case "Group":
                        break;
                    case "GuidanceResponse":
                        break;
                    case "HealthcareService":
                        break;
                    case "ImagingStudy":
                        break;
                    case "Immunization":
                        break;
                    case "ImmunizationEvaluation":
                        break;
                    case "ImmunizationRecommendation":
                        break;
                    case "ImplementationGuide":
                        break;
                    case "InsurancePlan":
                        break;
                    case "Invoice":
                        return "recipient";
                    case "Library":
                        break;
                    case "Linkage":
                        break;
                    case "List":
                        break;
                    case "Location":
                        break;
                    case "Measure":
                        break;
                    case "MeasureReport":
                        break;
                    case "Media":
                        break;
                    case "Medication":
                        break;
                    case "MedicationAdministration":
                        return "performer";
                    case "MedicationDispense":
                        break;
                    case "MedicationKnowledge":
                        break;
                    case "MedicationRequest":
                        break;
                    case "MedicationStatement":
                        return "source";
                    case "MedicinalProduct":
                        break;
                    case "MedicinalProductAuthorization":
                        break;
                    case "MedicinalProductContraindication":
                        break;
                    case "MedicinalProductIndication":
                        break;
                    case "MedicinalProductIngredient":
                        break;
                    case "MedicinalProductInteraction":
                        break;
                    case "MedicinalProductManufactured":
                        break;
                    case "MedicinalProductPackaged":
                        break;
                    case "MedicinalProductPharmaceutical":
                        break;
                    case "MedicinalProductUndesirableEffect":
                        break;
                    case "MessageDefinition":
                        break;
                    case "MessageHeader":
                        break;
                    case "MolecularSequence":
                        break;
                    case "NamingSystem":
                        break;
                    case "NutritionOrder":
                        break;
                    case "Observation":
                        return "performer";
                    case "ObservationDefinition":
                        break;
                    case "OperationDefinition":
                        break;
                    case "OperationOutcome":
                        break;
                    case "Organization":
                        break;
                    case "OrganizationAffiliation":
                        break;
                    case "Patient":
                        return "link";
                    case "PaymentNotice":
                        break;
                    case "PaymentReconciliation":
                        break;
                    case "Person":
                        return "link";
                    case "PlanDefinition":
                        break;
                    case "Practitioner":
                        break;
                    case "PractitionerRole":
                        break;
                    case "Procedure":
                        return "performer";
                    case "Provenance":
                        return "agent";
                    case "Questionnaire":
                        break;
                    case "QuestionnaireResponse":
                        break;
                    case "RelatedPerson":
                        return "{def}";
                    case "RequestGroup":
                        return "participant";
                    case "ResearchDefinition":
                        break;
                    case "ResearchElementDefinition":
                        break;
                    case "ResearchStudy":
                        break;
                    case "ResearchSubject":
                        break;
                    case "RiskAssessment":
                        break;
                    case "RiskEvidenceSynthesis":
                        break;
                    case "Schedule":
                        return "actor";
                    case "SearchParameter":
                        break;
                    case "ServiceRequest":
                        return "performer";
                    case "Slot":
                        break;
                    case "Specimen":
                        break;
                    case "SpecimenDefinition":
                        break;
                    case "StructureDefinition":
                        break;
                    case "StructureMap":
                        break;
                    case "Subscription":
                        break;
                    case "Substance":
                        break;
                    case "SubstanceNucleicAcid":
                        break;
                    case "SubstancePolymer":
                        break;
                    case "SubstanceProtein":
                        break;
                    case "SubstanceReferenceInformation":
                        break;
                    case "SubstanceSourceMaterial":
                        break;
                    case "SubstanceSpecification":
                        break;
                    case "SupplyDelivery":
                        break;
                    case "SupplyRequest":
                        return "requester";
                    case "Task":
                        break;
                    case "TerminologyCapabilities":
                        break;
                    case "TestReport":
                        break;
                    case "TestScript":
                        break;
                    case "ValueSet":
                        break;
                    case "VerificationResult":
                        break;
                    case "VisionPrescription":
                        break;
                }
                break;
        }

        return null;
    }
}
