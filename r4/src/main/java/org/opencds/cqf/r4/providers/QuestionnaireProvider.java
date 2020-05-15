package org.opencds.cqf.r4.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleUtil;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.opencds.cqf.common.config.HapiProperties;
import org.opencds.cqf.utilities.BundleUtils;

import static org.opencds.cqf.common.helpers.ClientHelper.getClient;

public class QuestionnaireProvider {

    private FhirContext fhirContext;
    public QuestionnaireProvider(FhirContext fhirContext){
        this.fhirContext = fhirContext;
    }

    @Operation(name = "$extract", idempotent = false, type = QuestionnaireResponse.class)
    public Observation extractObservationFromQuestionnaireResponse(@OperationParam(name = "questionnaireResponse") QuestionnaireResponse questionnaireResponse) {
        Observation newObs = new Observation();
        newObs.setId("123456");
        Bundle observationsFromQuestionnaireResponse = createObservationBundle(questionnaireResponse);
        Bundle returnBundle = sendObservationBundle(observationsFromQuestionnaireResponse);
        return newObs;
    }

    private Bundle createObservationBundle(QuestionnaireResponse questionnaireResponse){
        Bundle newBundle = new Bundle();

        QuestionnaireResponse.QuestionnaireResponseItemComponent item = questionnaireResponse.getItem().get(0);
        String answer = item.getAnswer().get(0).getValueStringType().getValue();
        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL);
 //       obs.setCode();
//        obs.setValue() =
/*
            Bundle bundleToPost = new Bundle();
            for (StringOrListParam params : valuesets.getValuesAsQueryTokens()) {
                for (StringParam valuesetId : params.getValuesAsQueryTokens()) {
                    bundleToPost.addEntry()
                            .setRequest(new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.PUT)
                                    .setUrl("ValueSet/" + valuesetId.getValue()))
                            .setResource(resolveValueSet(client, valuesetId.getValue()));
                }
            }

 */

        Identifier bundleId = new Identifier();
        bundleId.setValue(questionnaireResponse.getId());
        newBundle.setType(Bundle.BundleType.TRANSACTION);
        newBundle.setIdentifier(bundleId);

        Bundle.BundleEntryRequestComponent berc = new Bundle.BundleEntryRequestComponent();
        berc.setMethod(Bundle.HTTPVerb.PUT);
        berc.setUrl("Observation/65austin");

        Bundle.BundleEntryComponent bec = new Bundle.BundleEntryComponent();
        bec.setResource(obs);
        bec.setRequest(berc);

        newBundle.addEntry(bec);
        return newBundle;
    }

    private Bundle sendObservationBundle(Bundle observationsBundle){
        String url = HapiProperties.getObservationEndpoint();
        String user = HapiProperties.getObservationUserName();
        String password = HapiProperties.getObservationPassword();

        IGenericClient client = getClient(fhirContext, url, user, password);
Bundle testBundle = new Bundle();
IParser iParser = fhirContext.newJsonParser();
        Bundle outcomeBundle = client.transaction()
                .withBundle(observationsBundle)
                .execute();
        return outcomeBundle;
/*
        Bundle result = new Bundle();
        try {
            result = client.operation().onServer()
                    .named("$process-reference-list")
                    .withParameter(Parameters.class, "reference-list", referenceList)
                    .encodedJson()
                    .returnResourceType(Bundle.class)
                    .execute();
        }catch(Exception ex){
            ex.printStackTrace();
        }
*/
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
