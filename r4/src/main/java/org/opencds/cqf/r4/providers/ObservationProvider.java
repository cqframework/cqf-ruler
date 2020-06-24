package org.opencds.cqf.r4.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Observation;

public class ObservationProvider {

    private FhirContext fhirContext;

    public ObservationProvider(FhirContext fhirContext){
        this.fhirContext = fhirContext;
    }


    @Operation(name = "$transform", idempotent = false, type = Observation.class)
    public Bundle transformObservations(@OperationParam(name = "observations") Bundle observations,
                                                                 @OperationParam(name = "inputCodeSystem") String inputCodeSystem,
                                                                 @OperationParam(name = "outputCodeSystem")String outputCodeSystem) {
        if(null == observations || null == inputCodeSystem || null == outputCodeSystem) {
            throw new IllegalArgumentException("Unable to perform operation Observation$transform.  One of the  parameters was null");
        }
        ConceptMap conceptMapIn = new ConceptMap();
        conceptMapIn.setUrl(inputCodeSystem);
        conceptMapIn.setStatus(Enumerations.PublicationStatus.ACTIVE);

        ConceptMap conceptMapOut = new ConceptMap();
        conceptMapOut.setUrl(outputCodeSystem);
        conceptMapOut.setStatus(Enumerations.PublicationStatus.ACTIVE);


//        Bundle observationsFromQuestionnaireResponse = createObservationBundle(questionnaireResponse);
        Bundle returnBundle = new Bundle();//sendObservationBundle(observationsFromQuestionnaireResponse);
        return returnBundle;
    }
}
