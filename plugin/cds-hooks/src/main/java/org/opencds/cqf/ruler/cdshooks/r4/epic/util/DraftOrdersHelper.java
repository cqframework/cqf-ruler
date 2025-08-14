package org.opencds.cqf.ruler.cdshooks.r4.epic.util;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.ruler.cdshooks.r4.CdsHooksUtil;
import org.opencds.cqf.ruler.cdshooks.request.CdsHooksRequest;

import java.util.stream.Collectors;

public class DraftOrdersHelper {

	private final FhirContext fhirContext = FhirContext.forR4();

	private final JsonObject draftOrdersJson;
	private Bundle draftOrdersBundle;
	private Parameters draftOrdersParameters;
	private CodeableConcept draftOrdersCodeableConcept;

	public DraftOrdersHelper(CdsHooksRequest request) {
		if (request instanceof CdsHooksRequest.OrderSelect) {
			this.draftOrdersJson = ((CdsHooksRequest.OrderSelect) request).context.draftOrders;
		} else if (request instanceof CdsHooksRequest.OrderSign) {
			this.draftOrdersJson = ((CdsHooksRequest.OrderSign) request).context.draftOrders;
		} else {
			this.draftOrdersJson = null;
		}
		getDraftOrdersParameters();
		getDraftOrdersBundle();
		getDraftOrderMedicationCodeableConcept();
	}

	public JsonObject getDraftOrdersJson() {
		return this.draftOrdersJson;
	}

	public Parameters getDraftOrdersParameters() {
		if (this.draftOrdersParameters == null) {
			this.draftOrdersParameters = draftOrdersJson == null ? null : CdsHooksUtil.getParameters(draftOrdersJson);
		}
		return this.draftOrdersParameters;
	}

	public Bundle getDraftOrdersBundle() {
		if (this.draftOrdersBundle == null) {
			this.draftOrdersBundle = draftOrdersJson == null ? null : fhirContext.newJsonParser().parseResource(
				Bundle.class, new Gson().toJson(draftOrdersJson));
		}
		return this.draftOrdersBundle;
	}

	public CodeableConcept getDraftOrderMedicationCodeableConcept() {
		if (this.draftOrdersCodeableConcept == null) {
			var draftOrdersBundle = getDraftOrdersBundle();
			var medReqs = BundleUtil.toListOfResourcesOfType(fhirContext, draftOrdersBundle, MedicationRequest.class);
			// Assuming that only be a single MedicationRequest resource is present
			var medReq = medReqs.get(0);
			var meds = BundleUtil.toListOfResourcesOfType(fhirContext, draftOrdersBundle, Medication.class);
			if (medReq.hasMedicationReference()) {
				// Making an assumption that the draftOrders bundle will contain the Medication resource - should be safe for EPIC
				var match = meds.stream().filter(
					med -> medReq.getMedicationReference().getReference().endsWith(med.getIdPart())).findFirst();
				if (match.isPresent() && match.get().hasCode()) {
					this.draftOrdersCodeableConcept = match.get().getCode();
				}
			} else if (medReq.hasMedicationCodeableConcept() && medReq.getMedicationCodeableConcept().hasCoding()) {
				this.draftOrdersCodeableConcept = medReq.getMedicationCodeableConcept();
			}
		}
		return this.draftOrdersCodeableConcept;
	}

	public String canonicalizeConcept() {
		if (this.draftOrdersCodeableConcept == null) {
			return "NO-MED";
		}
		if (!this.draftOrdersCodeableConcept.hasCoding()) {
			return "NO-MED";
		}
		return draftOrdersCodeableConcept.getCoding().stream()
			.filter(coding -> coding.hasSystem() && coding.hasCode())
			.map(coding -> coding.getCode() + "|" + coding.getSystem())
			.sorted()
			.collect(Collectors.joining(","));
	}
}
