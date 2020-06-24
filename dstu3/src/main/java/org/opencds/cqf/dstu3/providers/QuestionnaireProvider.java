package org.opencds.cqf.dstu3.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.*;
import org.opencds.cqf.common.config.HapiProperties;

import java.util.Date;

import static org.opencds.cqf.common.helpers.ClientHelper.getClient;

public class QuestionnaireProvider {

    private FhirContext fhirContext;
    public QuestionnaireProvider(FhirContext fhirContext){
        this.fhirContext = fhirContext;
    }

    @Operation(name = "$extract", idempotent = false, type = QuestionnaireResponse.class)
    public Bundle extractObservationFromQuestionnaireResponse(@OperationParam(name = "questionnaireResponse") QuestionnaireResponse questionnaireResponse) {
        if(questionnaireResponse != null) {
            Bundle observationsFromQuestionnaireResponse = createObservationBundle(questionnaireResponse);
            Bundle returnBundle = sendObservationBundle(observationsFromQuestionnaireResponse);
            return returnBundle;
        }
        return createErrorBundle("The QuestionnaireResponse was null.");
    }

    private Bundle createErrorBundle(String errorMessage){
        Bundle errorBundle = new Bundle();
        Identifier bundleId = new Identifier();
        bundleId.setValue("Error in QuestionnaireResponse/$extract  " + errorMessage);
        errorBundle.setType(Bundle.BundleType.MESSAGE);
        errorBundle.setIdentifier(bundleId);
        return errorBundle;
    }

    private Bundle createObservationBundle(QuestionnaireResponse questionnaireResponse){
        Bundle newBundle = new Bundle();
        Date authored = questionnaireResponse.getAuthored();

        Identifier bundleId = new Identifier();
        bundleId.setValue("QuestionnaireResponse/" + questionnaireResponse.getIdElement().getIdPart());
        newBundle.setType(Bundle.BundleType.TRANSACTION);
        newBundle.setIdentifier(bundleId);

        questionnaireResponse.getItem().stream().forEach(item ->{
            newBundle.addEntry(extractItem(item, authored, questionnaireResponse));
        });
        return newBundle;
    }

    private Bundle.BundleEntryComponent extractItem(QuestionnaireResponse.QuestionnaireResponseItemComponent item, Date authored, QuestionnaireResponse questionnaireResponse){
        Observation obs = new Observation();
        obs.setEffective(new DateTimeType(authored));
        obs.setStatus(Observation.ObservationStatus.FINAL);
        obs.setSubject(questionnaireResponse.getSubject());
        Coding qrCoding = new Coding();
        qrCoding.setCode("74465-6");
        qrCoding.setDisplay("Questionnaire response Document");
        obs.setCode(new CodeableConcept().addCoding(qrCoding));
        obs.setId(questionnaireResponse.getIdElement().getIdPart() + "." + item.getLinkId());
        switch(item.getAnswer().get(0).getValue().fhirType()){
            case "string":
                obs.setValue(new StringType(item.getAnswer().get(0).getValueStringType().getValue()));
                break;
            case "Coding":
                obs.setValue(new CodeableConcept().addCoding(item.getAnswer().get(0).getValueCoding()));
                break;
        }
        Bundle.BundleEntryRequestComponent berc = new Bundle.BundleEntryRequestComponent();
        berc.setMethod(Bundle.HTTPVerb.PUT);
        berc.setUrl("Observation/" + obs.getId());

        Bundle.BundleEntryComponent bec = new Bundle.BundleEntryComponent();
        bec.setResource(obs);
        bec.setRequest(berc);
        return bec;
    }

    private Bundle sendObservationBundle(Bundle observationsBundle){
        String url = HapiProperties.getObservationEndpoint();
        if(null != url && url.length() > 0) {
            String user = HapiProperties.getObservationUserName();
            String password = HapiProperties.getObservationPassword();

            IGenericClient client = getClient(fhirContext, url, user, password);
            Bundle outcomeBundle = client.transaction()
                    .withBundle(observationsBundle)
                    .execute();
            return outcomeBundle;
        }
        return createErrorBundle("The observation.endpoint in hapi.properties was not set.");    }
}
