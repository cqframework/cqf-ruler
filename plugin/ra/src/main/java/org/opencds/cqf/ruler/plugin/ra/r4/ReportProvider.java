package org.opencds.cqf.ruler.plugin.ra.r4;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.plugin.ra.RAProperties;
import org.opencds.cqf.ruler.plugin.utility.IdUtilities;
import org.opencds.cqf.ruler.plugin.utility.ResolutionUtilities;
import org.opencds.cqf.ruler.plugin.utility.TypeUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;

public class ReportProvider implements OperationProvider, TypeUtilities, ResolutionUtilities {

    @Autowired
    private FhirContext myFhirContext;

    @Autowired
    private RAProperties myRaProperties;

    @Autowired
    private DaoRegistry ourRegistry;

    private static final Logger ourLog = LoggerFactory.getLogger(ReportProvider.class);

    /**
     * Implements the <a href="https://build.fhir.org/ig/HL7/davinci-ra/OperationDefinition-report.html">$report</a> operation found in the <a href="https://build.fhir.org/ig/HL7/davinci-ra/index.html">Da Vinci Risk Adjustment IG</a>.
     * 
     * @return a Parameters with Bundles of MeasureReports and evaluatedResource Resources
     */
    @Description(shortDefinition = "$report", value = "Implements the <a href=\"https://build.fhir.org/ig/HL7/davinci-ra/OperationDefinition-report.html\">$report</a> operation found in the <a href=\"https://build.fhir.org/ig/HL7/davinci-ra/index.html\">Da Vinci Risk Adjustment IG</a>.")
    @Operation(name = "$report", idempotent = true, type = MeasureReport.class)
    public Parameters report(
        @OperationParam(name = "periodStart", min = 1, max = 1) String periodStart,
        @OperationParam(name = "periodEnd", min = 1, max = 1) String periodEnd,
        @OperationParam(name = "subject", min = 1, max = 1) String subject) throws FHIRException { 
      Period period = validateParamaters(periodStart, periodEnd, subject);

      Parameters result = initializeParametersResult(subject);

      List<Reference> patients = getPatientListFromSubject(subject);

      return result;
    }




        // 
        // 
               
        // (getPatientListFromSubject(subject))
        //     .forEach(
        //         patientSubject -> {
        //             Parameters.ParametersParameterComponent patientParameter = patientReport(periodStartDate, periodEndDate, patientSubject.getReference());
        //             returnParams.addParameter(patientParameter);
        //         }
        //     );

        // return returnParams;
  //  }

  private Period validateParamaters(String periodStart, String periodEnd, String subject) {
    if (periodStart == null) {
        throw new IllegalArgumentException("Parameter 'periodStart' is required.");
    }    
    if (periodEnd == null) {
        throw new IllegalArgumentException("Parameter 'periodEnd' is required.");
    }    
    Date periodStartDate = resolveRequestDate(periodStart, true);
    Date periodEndDate = resolveRequestDate(periodEnd, false);
    if (periodStartDate.after(periodEndDate)) {
        throw new IllegalArgumentException("Parameter 'periodStart' must be before 'periodEnd'.");
    }

    if (subject == null) {
        throw new IllegalArgumentException("Parameter 'subject' is required.");
    }
    if (!subject.startsWith("Patient/") && !subject.startsWith("Group/")) {
        throw new IllegalArgumentException("Parameter 'subject' must be in the format 'Patient/[id]' or 'Group/[id]'.");
    }

    return new Period().setStart(periodStartDate).setEnd(periodEndDate);
  }

  private Parameters initializeParametersResult(String subject) {
    Parameters returnParams = new Parameters();

    returnParams.setId(subject.replace("/", "-") + "-report");

    return returnParams;
  }

  private void ensurePatient(String patientRef) {
    IBaseResource patient = resolveById(ourRegistry, Patient.class, patientRef);
    if (patient == null) {      
      throw new RuntimeException("Could not find Patient: " + patientRef);
    }
  }

  //TODO: replace this with version from base structures
  private List<Reference> getPatientListFromSubject(String subject) {
    List<Reference> patientList = null;

    if (subject.startsWith("Patient/")) {
      ensurePatient(subject);
      Reference patientReference = new Reference(subject);
      patientList = new ArrayList<Reference>();
      patientList.add(patientReference);
    } else if (subject.startsWith("Group/")) {
        patientList = getPatientListFromGroup(subject);
    } else {
      ourLog.info(String.format("Subject member was not a Patient or a Group, so skipping. \n%s", subject));
    }

    return patientList;
  }

  //TODO: replace this with version from base structures
  private List<Reference> getPatientListFromGroup(String subjectGroupId){
    List<Reference> patientList = new ArrayList<>();

    IBaseResource baseGroup = resolveById(ourRegistry, Group.class, subjectGroupId);
    if (baseGroup == null) {
        throw new RuntimeException("Could not find Group: " + subjectGroupId);
    }

    Group group = (Group)baseGroup;       
    group.getMember().forEach(member -> {     
        Reference reference = member.getEntity();
        if (reference.getReferenceElement().getResourceType().equals("Patient")) {
            ensurePatient(reference.getReference());
            patientList.add(reference);
        } else if (reference.getReferenceElement().getResourceType().equals("Group")) {
            patientList.addAll(getPatientListFromGroup(reference.getReference()));
        } else {
          ourLog.info(String.format("Group member was not a Patient or a Group, so skipping. \n%s", reference.getReference()));
        }
    });

    return patientList;
  }


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
