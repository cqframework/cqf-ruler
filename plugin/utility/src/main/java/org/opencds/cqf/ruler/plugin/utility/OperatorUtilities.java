package org.opencds.cqf.ruler.plugin.utility;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This interface provides utilities for implementing FHIR operators.
 */
public interface OperatorUtilities extends ResolutionUtilities {
   
    public static final Logger ourLog = LoggerFactory.getLogger(OperatorUtilities.class);
    
    /**
     * This function converts a string representation of a FHIR period date to a java.util.Date.
     * 
     * @param date the date to convert
     * @param start whether the date is the start of a period
     * @return the FHIR period date as a java.util.Date type
     */
    public default Date resolveRequestDate(String date, boolean start) {
        // split it up - support dashes or slashes
        String[] dissect = date.contains("-") ? date.split("-") : date.split("/");
        List<Integer> dateVals = new ArrayList<>();
        for (String dateElement : dissect) {
            dateVals.add(Integer.parseInt(dateElement));
        }

        if (dateVals.isEmpty()) {
            throw new IllegalArgumentException("Invalid date");
        }

        // for now support dates up to day precision
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.setTimeZone(TimeZone.getDefault());
        calendar.set(Calendar.YEAR, dateVals.get(0));
        if (dateVals.size() > 1) {
            // java.util.Date months are zero based, hence the negative 1 -- 2014-01 == February 2014
            calendar.set(Calendar.MONTH, dateVals.get(1) - 1);
        }
        if (dateVals.size() > 2)
            calendar.set(Calendar.DAY_OF_MONTH, dateVals.get(2));
        else {
            if (start) {
                calendar.set(Calendar.DAY_OF_MONTH, 1);
            }
            else {
                // get last day of month for end period
                calendar.add(Calendar.MONTH, 1);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.add(Calendar.DATE, -1);
            }
        }
        return calendar.getTime();
    }

    /**
     * This function returns a fullUrl for a resource.
     * 
     * @param serverAddress the address of the server
     * @param fhirType the type of the resource
     * @param elementId the id of the resource
     * @return the FHIR period date as a java.util.Date type
     */
    public default String getFullUrl(String serverAddress, String fhirType, String elementId) {
        String fullUrl = String.format("%s%s/%s", serverAddress, fhirType, elementId);
        return fullUrl;
    }

    //This has some issues
    // public default <ResourceType extends IBaseResource> void ensurePatient(String patientRef, DaoRegistry theRegistry, Class<ResourceType> theResourceType) {
    //     IBaseResource patient = resolveById(theRegistry, theResourceType, patientRef);
    //     if (patient == null) {      
    //     throw new RuntimeException("Could not find Patient: " + patientRef);
    //     }
    // }
    
    //   //TODO: replace this with version from base structures
    //   public default <ResourceType extends IBaseResource> List<Reference> getPatientListFromSubject(String theSubject, DaoRegistry theRegistry, Class<ResourceType> thePatientType, Class<ResourceType> theGroupType) {
    //     List<Reference> patientList = null;

    //     if (theSubject.startsWith("Patient/")) {
    //         ensurePatient(theSubject, theRegistry, thePatientType);
    //         patientList = new ArrayList<Reference>();
    //         Reference patientReference = new Reference().setReference(theSubject);          
    //         patientList.add(patientReference);
    //     } else if (theSubject.startsWith("Group/")) {
    //         patientList = getPatientListFromGroup(theSubject, theRegistry, thePatientType, theGroupType);
    //     } else {
    //         ourLog.info(String.format("Subject member was not a Patient or a Group, so skipping. \n%s", theSubject));
    //     }

    //     return patientList;
    //   }
    
    //   //TODO: replace this with version from base structures
    //   public default <ResourceType extends IBaseResource> List<Reference> getPatientListFromGroup(String subjectGroupId, DaoRegistry theRegistry, Class<ResourceType> thePatientType, Class<ResourceType> theGroupType){
    //     List<Reference> patientList = new ArrayList<>();
    
    //     IBaseResource baseGroup = resolveById(theRegistry, theGroupType, subjectGroupId);
    //     if (baseGroup == null) {
    //         throw new RuntimeException("Could not find Group: " + subjectGroupId);
    //     }
    
    //     Group group = (Group)baseGroup;       
    //     group.getMember().forEach(member -> {     
    //         Reference reference = member.getEntity();
    //         if (reference.getReference().getValue().startsWith("/Patient")) {
    //             ensurePatient(reference.getReference().getValue(), theRegistry, thePatientType);
    //             patientList.add(reference);
    //         } else if (reference.getReference().getValue().startsWith("/Group")) {
    //             patientList.addAll(getPatientListFromGroup(reference.getReference().getValue(), theRegistry, thePatientType, theGroupType));
    //         } else {
    //           ourLog.info(String.format("Group member was not a Patient or a Group, so skipping. \n%s", reference.getReference()));
    //         }
    //     });
    
    //     return patientList;
    //   }
}
