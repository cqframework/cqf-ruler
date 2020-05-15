package org.opencds.cqf.r4.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleUtil;
import com.github.dnault.xmlpatch.repackaged.org.jaxen.util.SingletonList;
import org.hl7.fhir.r4.model.*;
import org.opencds.cqf.common.config.HapiProperties;
import org.opencds.cqf.utilities.BundleUtils;

import java.util.List;

import static org.opencds.cqf.common.helpers.ClientHelper.getClient;

public class QuestionnaireProvider {

    private FhirContext fhirContext;
    public QuestionnaireProvider(FhirContext fhirContext){
        this.fhirContext = fhirContext;
    }

    @Operation(name = "$extract", idempotent = false, type = QuestionnaireResponse.class)
    public Bundle extractObservationFromQuestionnaireResponse(@OperationParam(name = "questionnaireResponse") QuestionnaireResponse questionnaireResponse) {
        Bundle observationsFromQuestionnaireResponse = createObservationBundle(questionnaireResponse);
        Bundle returnBundle = sendObservationBundle(observationsFromQuestionnaireResponse);
        return returnBundle;
    }

    private Bundle createObservationBundle(QuestionnaireResponse questionnaireResponse){
        Bundle newBundle = new Bundle();

        Identifier bundleId = new Identifier();
        bundleId.setValue(questionnaireResponse.getId());
        newBundle.setType(Bundle.BundleType.TRANSACTION);
        newBundle.setIdentifier(bundleId);

        questionnaireResponse.getItem().stream().forEach(item ->{
            newBundle.addEntry(extractItem(item, questionnaireResponse.getQuestionnaire()));
        });
        return newBundle;
    }

    //TODO - ids need work; add encounter; what more??
    private Bundle.BundleEntryComponent extractItem(QuestionnaireResponse.QuestionnaireResponseItemComponent item, String questionnaire){
        //       obs.setCode();
//        obs.setValue() =

        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL);
        obs.setId(item.getLinkId());
//        obs.setDerivedFrom(new SingletonList("QuestionnaireResponse." + questionnaire + "." + item.getLinkId()));
        obs.setValue(new StringType(item.getText() + "::" + item.getAnswer().get(0).getValueStringType().getValue()));

        Bundle.BundleEntryRequestComponent berc = new Bundle.BundleEntryRequestComponent();
        berc.setMethod(Bundle.HTTPVerb.PUT);
        berc.setUrl("Observation/" + item.getId() + "." + item.getLinkId());

        Bundle.BundleEntryComponent bec = new Bundle.BundleEntryComponent();
        bec.setResource(obs);
        bec.setRequest(berc);
        return bec;
    }

    private Bundle sendObservationBundle(Bundle observationsBundle){
        String url = HapiProperties.getObservationEndpoint();
        String user = HapiProperties.getObservationUserName();
        String password = HapiProperties.getObservationPassword();

        IGenericClient client = getClient(fhirContext, url, user, password);
        Bundle outcomeBundle = client.transaction()
                .withBundle(observationsBundle)
                .execute();
        return outcomeBundle;
    }

    /**
     * How do we store the QuestionnaireResponse Id??
     *
     *     var obxs = [];
     *     for(var i=0, iLen=values.length; i<iLen; i++) {
     *       var obx = {
     *         "resourceType": "Observation",
     *         "status": "final",
     *         "code": {
     *           "coding": item.codeList,
     *           "text": item.question
     *         },
     *
     *         "encounter":"",//reference - QuestionnaireResponse  or Questionnaire https://www.hl7.org/fhir/valueset-resource-types.html
     *         "effective": USE "meta": {
     *           "lastUpdated": "2020-05-14T08:33:31.845-06:00",
     *         },
     *
     *
     *
     *
     *       };
     *       this._addVersionTag(obx);
     *       if (setId) {
     *         obx.id = this._getUniqueId(item.questionCode);
     *       }
     *       if (!item.header) {
     *         obx[values[i].key] = values[i].val;
     *       }
     *       obxs.push(obx);
     *     }
     *     return obxs;
     */



}
