package org.opencds.cqf.ruler.plugin.ra.r4;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;

import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.plugin.ra.RAProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;

public class ReportProvider implements OperationProvider {

    @Autowired
    private FhirContext myFhirContext;

    @Autowired
    private RAProperties myRaProperties;

    private static final Logger ourLog = LoggerFactory.getLogger(ReportProvider.class);

    /**
     * Implements the $hello-world operation found in the <a href="https://www.hl7.org/fhir/clinicalreasoning-module.html">FHIR CR Module</a>
     * 
     * @return a greeting
     */
    @Description(shortDefinition = "returns a greeting", value = "Implements the $hello-world operation found in the <a href=\"https://www.hl7.org/fhir/clinicalreasoning-module.html\">FHIR CR Module</a>")
    @Operation(name = "$report", idempotent = true, type = MeasureReport.class)
    public Parameters report(
        @OperationParam(name = "periodStart", min = 1, max = 1) String periodStart,
        @OperationParam(name = "periodEnd", min = 1, max = 1) String periodEnd,
        @OperationParam(name = "subject", min = 1, max = 1) String subject) throws FHIRException { 
      return null;
    }

    // @Operation(name = "$report", idempotent = true, type = MeasureReport.class)
    // public Parameter report(@OperationParam(name = "periodStart", min = 1, max = 1) String periodStart,
    //                                  @OperationParam(name = "periodEnd", min = 1, max = 1) String periodEnd,
    //                                  @OperationParam(name = "subject", min = 1, max = 1) String subject) throws FHIRException { 
        
        // validateParamaters(periodStart, periodEnd, subject);

        // Parameters returnParams = new Parameters();
        // returnParams.setId(subject.replace("/", "-") + "-report");
               
        // (getPatientListFromSubject(subject))
        //     .forEach(
        //         patientSubject -> {
        //             Parameters.ParametersParameterComponent patientParameter = patientReport(periodStartDate, periodEndDate, patientSubject.getReference());
        //             returnParams.addParameter(patientParameter);
        //         }
        //     );

        // return returnParams;
  //  }

  //  private void validateParamaters(String periodStart, String periodEnd, String subject) {
        // if (periodStart == null) {
        //     throw new IllegalArgumentException("Parameter 'periodStart' is required.");
        // }    
        // if (periodEnd == null) {
        //     throw new IllegalArgumentException("Parameter 'periodEnd' is required.");
        // }    
        // Date periodStartDate = DateHelper.resolveRequestDate(periodStart, true);
        // Date periodEndDate = DateHelper.resolveRequestDate(periodEnd, false);
        // if (periodStartDate.after(periodEndDate)) {
        //     throw new IllegalArgumentException("Parameter 'periodStart' must be before 'periodEnd'.");
        // }
 
        // if (subject == null) {
        //     throw new IllegalArgumentException("Parameter 'subject' is required.");
        // }
        // if (!subject.startsWith("Patient/") && !subject.startsWith("Group/")) {
        //     throw new IllegalArgumentException("Parameter 'subject' must be in the format 'Patient/[id]' or 'Group/[id]'.");
        // }
  //  }

   // private static String PATIENT_REPORT_PROFILE_URL = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-measurereport-bundle";

    // private Parameters.ParametersParameterComponent patientReport(Date periodStart, Date periodEnd, String subject) {
    //     Patient patient = ensurePatient(subject);
    //     final Map<IIdType, IAnyResource> patientResources = new HashMap<>();
    //     patientResources.put(patient.getIdElement(), patient);

    //     SearchParameterMap theParams = SearchParameterMap.newSynchronous();
    //     ReferenceParam subjectParam = new ReferenceParam(subject);
    //     theParams.add("subject", subjectParam);

    //     Bundle patientReportBundle = new Bundle();
    //         patientReportBundle.setMeta(new Meta().addProfile(PATIENT_REPORT_PROFILE_URL));
    //         patientReportBundle.setType(Bundle.BundleType.COLLECTION);
    //         patientReportBundle.setTimestamp(new Date());
    //         patientReportBundle.setId(subject.replace("/", "-") + "-report");
    //         patientReportBundle.setIdentifier(new Identifier().setSystem("urn:ietf:rfc:3986").setValue("urn:uuid:" + UUID.randomUUID().toString()));

    //     IFhirResourceDao<MeasureReport> measureReportDao = this.registry.getResourceDao(MeasureReport.class);
    //     measureReportDao.search(theParams).getAllResources().forEach(baseResource -> {
    //         MeasureReport measureReport = (MeasureReport)baseResource;        

    //         if (measureReport.getPeriod().getEnd().before(periodStart) || measureReport.getPeriod().getStart().after(periodEnd)) {
    //             return;
    //         }           
            
    //         patientReportBundle.addEntry(
    //             new Bundle.BundleEntryComponent()
    //                 .setResource(measureReport)
    //                 .setFullUrl(getFullUrl(measureReport.fhirType(), measureReport.getIdElement().getIdPart()))
    //         );

    //         List<IAnyResource> resources;
    //         resources = addEvaluatedResources(measureReport);
    //         resources.forEach(resource -> {
    //             patientResources.putIfAbsent(resource.getIdElement(), resource);
    //         });
    //     });

    //     patientResources.entrySet().forEach(resource -> {
    //         patientReportBundle.addEntry(
    //             new Bundle.BundleEntryComponent()
    //                 .setResource((Resource) resource.getValue())
    //                 .setFullUrl(getFullUrl(resource.getValue().fhirType(), resource.getValue().getIdElement().getIdPart()))
    //         );
    //     });

    //     Parameters.ParametersParameterComponent patientParameter = new Parameters.ParametersParameterComponent();
    //         patientParameter.setResource(patientReportBundle);
    //         patientParameter.setId(subject.replace("/", "-") + "-report");
    //         patientParameter.setName("return");
    //     return patientParameter;
    // }

    // private Patient ensurePatient(String patient) {
    //     String patientId = patient.replace("Patient/", "");
    //     IFhirResourceDao<Patient> patientDao = this.registry.getResourceDao(Patient.class);
    //     Patient patientResource = patientDao.read(new IdType(patientId));
    //     if (patientResource == null) {
    //         throw new RuntimeException("Could not find Patient: " + patientId);
    //     }
    //     return patientResource;
    // }

    // private List<Reference> getPatientListFromSubject(String subject) {
    //     List<Reference> patientList = null;
    //     if (subject.startsWith("Patient/")) {
    //         Reference patientReference = new Reference(subject);
    //         patientList = new ArrayList<Reference>();
    //         patientList.add(patientReference);
    //     } else if (subject.startsWith("Group/")) {
    //         patientList = getPatientListFromGroup(subject);
    //     } else {
    //         logger.info(String.format("Subject member was not a Patient or a Group, so skipping. \n%s", subject));
    //     }
    //     return patientList;
    // }

    // private List<Reference> getPatientListFromGroup(String subjectGroupRef){
    //     List<Reference> patientList = new ArrayList<>();
    //     IBaseResource baseGroup = this.registry.getResourceDao("Group").read(new IdType(subjectGroupRef));
    //     if (baseGroup == null) {
    //         throw new RuntimeException("Could not find Group/" + subjectGroupRef);
    //     }
    //     Group group = (Group)baseGroup;       
    //     group.getMember().forEach(member -> {     
    //         Reference reference = member.getEntity();
    //         if (reference.getReferenceElement().getResourceType().equals("Patient")) {
    //             patientList.add(reference);
    //         } else if (reference.getReferenceElement().getResourceType().equals("Group")) {
    //             patientList.addAll(getPatientListFromGroup(reference.getReference()));
    //         } else {
    //             logger.info(String.format("Group member was not a Patient or a Group, so skipping. \n%s", reference.getReference()));
    //         }
    //     });
    //     return patientList;
    // }
}
