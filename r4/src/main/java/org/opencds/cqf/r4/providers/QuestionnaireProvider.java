package org.opencds.cqf.r4.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.*;
import org.opencds.cqf.common.config.HapiProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import static org.opencds.cqf.common.helpers.ClientHelper.getClient;

@Component
public class QuestionnaireProvider {

    private FhirContext fhirContext;

    @Inject
    public QuestionnaireProvider(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    @Operation(name = "$extract", idempotent = false, type = QuestionnaireResponse.class)
    public Bundle extractObservationFromQuestionnaireResponse(
            @OperationParam(name = "questionnaireResponse") QuestionnaireResponse questionnaireResponse) {
        if (questionnaireResponse == null) {
            throw new IllegalArgumentException(
                    "Unable to perform operation $extract.  The QuestionnaireResponse was null");
        }
        Bundle observationsFromQuestionnaireResponse = createObservationBundle(questionnaireResponse);
        Bundle returnBundle = sendObservationBundle(observationsFromQuestionnaireResponse);
        return returnBundle;
    }

    private Bundle createObservationBundle(QuestionnaireResponse questionnaireResponse) {
        Bundle newBundle = new Bundle();
        Date authored = questionnaireResponse.getAuthored();

        Identifier bundleId = new Identifier();
        bundleId.setValue("QuestionnaireResponse/" + questionnaireResponse.getIdElement().getIdPart());
        newBundle.setType(Bundle.BundleType.TRANSACTION);
        newBundle.setIdentifier(bundleId);
        Map<String, Coding> questionnaireCodeMap = getQuestionnaireCodeMap(questionnaireResponse.getQuestionnaire());

        questionnaireResponse.getItem().stream().forEach(item -> {
            processItems(item, authored, questionnaireResponse, newBundle, questionnaireCodeMap);
        });
        return newBundle;
    }

    private void processItems(QuestionnaireResponse.QuestionnaireResponseItemComponent item, Date authored,
            QuestionnaireResponse questionnaireResponse, Bundle newBundle, Map<String, Coding> questionnaireCodeMap) {
        if (item.hasAnswer()) {
            item.getAnswer().forEach(answer -> {
                Bundle.BundleEntryComponent newBundleEntryComponent = createObservationFromItemAnswer(answer,
                        item.getLinkId(), authored, questionnaireResponse, questionnaireCodeMap);
                if (null != newBundleEntryComponent) {
                    newBundle.addEntry(newBundleEntryComponent);
                }
                if (answer.hasItem()) {
                    answer.getItem().forEach(answerItem -> {
                        processItems(answerItem, authored, questionnaireResponse, newBundle, questionnaireCodeMap);
                    });
                }
            });
        }
        if (item.hasItem()) {
            item.getItem().forEach(itemItem -> {
                processItems(itemItem, authored, questionnaireResponse, newBundle, questionnaireCodeMap);
            });
        }
    }

    private Bundle.BundleEntryComponent createObservationFromItemAnswer(
            QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent answer, String linkId, Date authored,
            QuestionnaireResponse questionnaireResponse, Map<String, Coding> questionnaireCodeMap) {
        Observation obs = new Observation();
        obs.setEffective(new DateTimeType(authored));
        obs.setStatus(Observation.ObservationStatus.FINAL);
        obs.setSubject(questionnaireResponse.getSubject());
        Coding qrCategoryCoding = new Coding();
        qrCategoryCoding.setCode("survey");
        qrCategoryCoding.setSystem("http://hl7.org/fhir/observation-category");
        obs.setCategory(Collections.singletonList(new CodeableConcept().addCoding(qrCategoryCoding)));
        obs.setCode(new CodeableConcept().addCoding((Coding) questionnaireCodeMap.get(linkId)));
        obs.setId("qr" + questionnaireResponse.getIdElement().getIdPart() + "." + linkId);
        switch (answer.getValue().fhirType()) {
            case "string":
                obs.setValue(new StringType(answer.getValueStringType().getValue()));
                break;
            case "Coding":
                obs.setValue(new CodeableConcept().addCoding(answer.getValueCoding()));
                break;
            case "boolean":
                obs.setValue(new BooleanType(answer.getValueBooleanType().booleanValue()));
                break;
        }
        Reference questionnaireResponseReference = new Reference();
        questionnaireResponseReference
                .setReference("QuestionnaireResponse" + "/" + questionnaireResponse.getIdElement().getIdPart());
        obs.setDerivedFrom(Collections.singletonList(questionnaireResponseReference));
        Extension linkIdExtension = new Extension();
        linkIdExtension.setUrl("http://hl7.org/fhir/uv/sdc/StructureDefinition/derivedFromLinkId");
        Extension innerLinkIdExtension = new Extension();
        innerLinkIdExtension.setUrl("text");
        innerLinkIdExtension.setValue(new StringType(linkId));
        linkIdExtension.setExtension(Collections.singletonList(innerLinkIdExtension));
        obs.addExtension(linkIdExtension);
        Bundle.BundleEntryRequestComponent berc = new Bundle.BundleEntryRequestComponent();
        berc.setMethod(Bundle.HTTPVerb.PUT);
        berc.setUrl("Observation/" + obs.getId());

        Bundle.BundleEntryComponent bec = new Bundle.BundleEntryComponent();
        bec.setResource(obs);
        bec.setRequest(berc);
        return bec;
    }

    private Bundle sendObservationBundle(Bundle observationsBundle) throws IllegalArgumentException {
        String url = HapiProperties.getQuestionnaireResponseExtractEndpoint();
        if (null == url || url.length() < 1) {
            throw new IllegalArgumentException(
                    "Unable to transmit observation bundle.  No observation.endpoint defined in hapi.properties.");
        }
        String user = HapiProperties.getQuestionnaireResponseExtractUserName();
        String password = HapiProperties.getQuestionnaireResponseExtractPassword();

        IGenericClient client = getClient(fhirContext, url, user, password);
        Bundle outcomeBundle = client.transaction().withBundle(observationsBundle).execute();
        return outcomeBundle;
    }

    private Map<String, Coding> getQuestionnaireCodeMap(String questionnaireUrl) {
        String url = HapiProperties.getQuestionnaireResponseExtractEndpoint();
        if (null == url || url.length() < 1) {
            throw new IllegalArgumentException(
                    "Unable to GET Questionnaire.  No observation.endpoint defined in hapi.properties.");
        }
        String user = HapiProperties.getQuestionnaireResponseExtractUserName();
        String password = HapiProperties.getQuestionnaireResponseExtractPassword();

        IGenericClient client = getClient(fhirContext, url, user, password);
        Questionnaire questionnaire = client.read().resource(Questionnaire.class).withUrl(questionnaireUrl).execute();

        return createCodeMap(questionnaire);
    }

    // this is based on "if a questionnaire.item has items then this item is a
    // header and will not have a specific code to be used with an answer"
    private Map<String, Coding> createCodeMap(Questionnaire questionnaire) {
        Map<String, Coding> questionnaireCodeMap = new HashMap<>();
        questionnaire.getItem().forEach((item) -> {
            processQuestionnaireItems(item, questionnaireCodeMap);
        });
        return questionnaireCodeMap;
    }

    private void processQuestionnaireItems(Questionnaire.QuestionnaireItemComponent item,
            Map<String, Coding> questionnaireCodeMap) {
        if (item.hasItem()) {
            item.getItem().forEach(qItem -> {
                processQuestionnaireItems(qItem, questionnaireCodeMap);
            });
        } else {
            questionnaireCodeMap.put(item.getLinkId(), item.getCodeFirstRep());
        }
    }
}
