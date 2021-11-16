package org.opencds.cqf.ruler.plugin.ra.r4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.hibernate.cfg.NotYetImplementedException;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DetectedIssue;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.cql.common.provider.LibraryResolutionProvider;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.rp.r4.MeasureResourceProvider;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.IVersionSpecificBundleFactory;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.plugin.ra.RAProperties;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;

public class ReportProvider implements OperationProvider {

    @Autowired
    private FhirContext myFhirContext;

    @Autowired
    private RAProperties myRaProperties;

    private static final Logger logger = LoggerFactory.getLogger(ReportProvider.class);

    @Operation(name = "$report", idempotent = true, type = MeasureReport.class)
    public Parameter report(@OperationParam(name = "periodStart", min = 1, max = 1) String periodStart,
                                     @OperationParam(name = "periodEnd", min = 1, max = 1) String periodEnd,
                                     @OperationParam(name = "subject", min = 1, max = 1) String subject) throws FHIRException { 
        
        validateParamaters(periodStart, periodEnd, subject);

        Parameters returnParams = new Parameters();
        returnParams.setId(subject.replace("/", "-") + "-report");
               
        (getPatientListFromSubject(subject))
            .forEach(
                patientSubject -> {
                    Parameters.ParametersParameterComponent patientParameter = patientReport(periodStartDate, periodEndDate, patientSubject.getReference());
                    returnParams.addParameter(patientParameter);
                }
            );

        return returnParams;
    }

    private void validateParamaters(String periodStart, String periodEnd, String subject) {
        if (periodStart == null) {
            throw new IllegalArgumentException("Parameter 'periodStart' is required.");
        }    
        if (periodEnd == null) {
            throw new IllegalArgumentException("Parameter 'periodEnd' is required.");
        }    
        Date periodStartDate = DateHelper.resolveRequestDate(periodStart, true);
        Date periodEndDate = DateHelper.resolveRequestDate(periodEnd, false);
        if (periodStartDate.after(periodEndDate)) {
            throw new IllegalArgumentException("Parameter 'periodStart' must be before 'periodEnd'.");
        }
 
        if (subject == null) {
            throw new IllegalArgumentException("Parameter 'subject' is required.");
        }
        if (!subject.startsWith("Patient/") && !subject.startsWith("Group/")) {
            throw new IllegalArgumentException("Parameter 'subject' must be in the format 'Patient/[id]' or 'Group/[id]'.");
        }
    }

    private static String PATIENT_REPORT_PROFILE_URL = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-measurereport-bundle";

    private Parameters.ParametersParameterComponent patientReport(Date periodStart, Date periodEnd, String subject) {
        Patient patient = ensurePatient(subject);
        final Map<IIdType, IAnyResource> patientResources = new HashMap<>();
        patientResources.put(patient.getIdElement(), patient);

        SearchParameterMap theParams = SearchParameterMap.newSynchronous();
        ReferenceParam subjectParam = new ReferenceParam(subject);
        theParams.add("subject", subjectParam);

        Bundle patientReportBundle = new Bundle();
            patientReportBundle.setMeta(new Meta().addProfile(PATIENT_REPORT_PROFILE_URL));
            patientReportBundle.setType(Bundle.BundleType.COLLECTION);
            patientReportBundle.setTimestamp(new Date());
            patientReportBundle.setId(subject.replace("/", "-") + "-report");
            patientReportBundle.setIdentifier(new Identifier().setSystem("urn:ietf:rfc:3986").setValue("urn:uuid:" + UUID.randomUUID().toString()));

        IFhirResourceDao<MeasureReport> measureReportDao = this.registry.getResourceDao(MeasureReport.class);
        measureReportDao.search(theParams).getAllResources().forEach(baseResource -> {
            MeasureReport measureReport = (MeasureReport)baseResource;        

            if (measureReport.getPeriod().getEnd().before(periodStart) || measureReport.getPeriod().getStart().after(periodEnd)) {
                return;
            }           
            
            patientReportBundle.addEntry(
                new Bundle.BundleEntryComponent()
                    .setResource(measureReport)
                    .setFullUrl(getFullUrl(measureReport.fhirType(), measureReport.getIdElement().getIdPart()))
            );

            List<IAnyResource> resources;
            resources = addEvaluatedResources(measureReport);
            resources.forEach(resource -> {
                patientResources.putIfAbsent(resource.getIdElement(), resource);
            });
        });

        patientResources.entrySet().forEach(resource -> {
            patientReportBundle.addEntry(
                new Bundle.BundleEntryComponent()
                    .setResource((Resource) resource.getValue())
                    .setFullUrl(getFullUrl(resource.getValue().fhirType(), resource.getValue().getIdElement().getIdPart()))
            );
        });

        Parameters.ParametersParameterComponent patientParameter = new Parameters.ParametersParameterComponent();
            patientParameter.setResource(patientReportBundle);
            patientParameter.setId(subject.replace("/", "-") + "-report");
            patientParameter.setName("return");
        return patientParameter;
    }

    private Patient ensurePatient(String patient) {
        String patientId = patient.replace("Patient/", "");
        IFhirResourceDao<Patient> patientDao = this.registry.getResourceDao(Patient.class);
        Patient patientResource = patientDao.read(new IdType(patientId));
        if (patientResource == null) {
            throw new RuntimeException("Could not find Patient: " + patientId);
        }
        return patientResource;
    }

    private List<Reference> getPatientListFromSubject(String subject) {
        List<Reference> patientList = null;
        if (subject.startsWith("Patient/")) {
            Reference patientReference = new Reference(subject);
            patientList = new ArrayList<Reference>();
            patientList.add(patientReference);
        } else if (subject.startsWith("Group/")) {
            patientList = getPatientListFromGroup(subject);
        } else {
            logger.info(String.format("Subject member was not a Patient or a Group, so skipping. \n%s", subject));
        }
        return patientList;
    }

    private List<Reference> getPatientListFromGroup(String subjectGroupRef){
        List<Reference> patientList = new ArrayList<>();
        IBaseResource baseGroup = this.registry.getResourceDao("Group").read(new IdType(subjectGroupRef));
        if (baseGroup == null) {
            throw new RuntimeException("Could not find Group/" + subjectGroupRef);
        }
        Group group = (Group)baseGroup;       
        group.getMember().forEach(member -> {     
            Reference reference = member.getEntity();
            if (reference.getReferenceElement().getResourceType().equals("Patient")) {
                patientList.add(reference);
            } else if (reference.getReferenceElement().getResourceType().equals("Group")) {
                patientList.addAll(getPatientListFromGroup(reference.getReference()));
            } else {
                logger.info(String.format("Group member was not a Patient or a Group, so skipping. \n%s", reference.getReference()));
            }
        });
        return patientList;
    }
}
