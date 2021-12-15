package org.opencds.cqf.ruler.plugin.sdc.r4;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.plugin.sdc.SDCProperties;
import org.opencds.cqf.ruler.plugin.utility.ClientUtilities;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class ExtractProvider implements OperationProvider, ClientUtilities {

	@Autowired
	private FhirContext myFhirContext;

	@Autowired
	private SDCProperties mySdcProperties;

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

		String questionnaireCanonical = questionnaireResponse.getQuestionnaire();
		if (questionnaireCanonical == null || questionnaireCanonical.isEmpty()) {
			throw new IllegalArgumentException(
					"The QuestionnaireResponse must have the source Questionnaire specified to do extraction");
		}
		Map<String, Coding> questionnaireCodeMap = getQuestionnaireCodeMap(questionnaireCanonical);

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
		obs.setCode(new CodeableConcept().addCoding(questionnaireCodeMap.get(linkId)));
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
		String url = mySdcProperties.getExtract().getEndpoint();
		if (null == url || url.length() < 1) {
			throw new IllegalArgumentException(
					"Unable to transmit observation bundle.  No observation.endpoint defined in sdc properties.");
		}
		String user = mySdcProperties.getExtract().getUsername();
		String password = mySdcProperties.getExtract().getPassword();

		IGenericClient client = this.createClient(myFhirContext, url);
		this.registerBasicAuth(client, user, password);
		Bundle outcomeBundle = client.transaction().withBundle(observationsBundle).execute();
		return outcomeBundle;
	}

	private Map<String, Coding> getQuestionnaireCodeMap(String questionnaireUrl) {
		String url = mySdcProperties.getExtract().getEndpoint();
		if (null == url || url.length() < 1) {
			throw new IllegalArgumentException(
					"Unable to GET Questionnaire.  No observation.endpoint defined in sdc properties.");
		}
		String user = mySdcProperties.getExtract().getUsername();
		String password = mySdcProperties.getExtract().getPassword();

		IGenericClient client = this.createClient(myFhirContext, url);
		this.registerBasicAuth(client, user, password);
		Bundle results = (Bundle) client.search().forResource(Questionnaire.class)
				.where(Questionnaire.URL.matches().value(questionnaireUrl)).execute();

		Questionnaire questionnaire = (Questionnaire) results.getEntry().get(0).getResource();

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
