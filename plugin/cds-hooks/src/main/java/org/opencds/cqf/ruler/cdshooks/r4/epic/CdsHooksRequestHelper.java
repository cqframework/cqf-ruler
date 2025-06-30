package org.opencds.cqf.ruler.cdshooks.r4.epic;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.opencds.cqf.ruler.cdshooks.r4.CdsHooksUtil;
import org.opencds.cqf.ruler.cdshooks.request.CdsHooksRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CdsHooksRequestHelper {

	private final FhirContext fhirContext = FhirContext.forR4();

	private final EpicLogging logging;

	private final CdsHooksRequest request;
	private final String serviceId;
	private final String patientId;
	private final JsonObject draftOrders;
	private final Endpoint remoteDataEndpoint;
	private String mrn;

	public CdsHooksRequestHelper(CdsHooksRequest request, String serviceId, String serverAddress, EpicLogging logging) {
		this.request = request;
		this.serviceId = serviceId;
		this.logging = logging;

		if (request instanceof CdsHooksRequest.OrderSelect) {
			this.patientId = ((CdsHooksRequest.OrderSelect) request).context.patientId;
			this.draftOrders = ((CdsHooksRequest.OrderSelect) request).context.draftOrders;
		} else if (request instanceof CdsHooksRequest.OrderSign) {
			this.patientId = ((CdsHooksRequest.OrderSign) request).context.patientId;
			this.draftOrders = ((CdsHooksRequest.OrderSign) request).context.draftOrders;
		} else {
			this.patientId = request.context.patientId;
			this.draftOrders = null;
		}

		if (request.fhirServer != null && !request.fhirServer.equals(serverAddress)) {
			var ep = new Endpoint().setAddress(request.fhirServer);
			if (request.fhirAuthorization != null) {
				ep.addHeader(String.format("Authorization: %s %s",
					request.fhirAuthorization.tokenType,
					request.fhirAuthorization.accessToken)
				);
			}
			this.remoteDataEndpoint = ep;
		} else {
			this.remoteDataEndpoint = new Endpoint().setAddress(serverAddress);
		}
	}

	public CdsHooksRequest getRequest() {
		return this.request;
	}

	public String getServiceId() {
		return this.serviceId;
	}

	public String getPatientId() {
		return this.patientId;
	}

	public String getMrn() {
		return this.mrn;
	}

	public Parameters getDraftOrdersParameters() {
		return draftOrders == null ? null : CdsHooksUtil.getParameters(draftOrders);
	}

	public Bundle getDraftOrdersBundle() {
		return draftOrders == null ? null : fhirContext.newJsonParser().parseResource(
			Bundle.class, new Gson().toJson(draftOrders));
	}

	public List<String> getDraftOrderMedicationCodes() {
		var medCodes = new ArrayList<String>();
		var draftOrdersBundle = getDraftOrdersBundle();
		var medReqs = BundleUtil.toListOfResourcesOfType(fhirContext, draftOrdersBundle, MedicationRequest.class);
		var meds = BundleUtil.toListOfResourcesOfType(fhirContext, draftOrdersBundle, Medication.class);
		for (var medReq : medReqs) {
			if (medReq.hasMedicationReference()) {
				// Making an assumption that the draftOrders bundle will contain the Medication resource - should be safe for EPIC
				var match = meds.stream().filter(
					med -> medReq.getMedicationReference().getReference().endsWith(med.getIdPart())).findFirst();
				if (match.isPresent() && match.get().hasCode() && match.get().getCode().hasCoding()) {
					medCodes.addAll(match.get().getCode().getCoding().stream()
						.map(Coding::getCode).collect(Collectors.toList()));
				}
			} else if (medReq.hasMedicationCodeableConcept() && medReq.getMedicationCodeableConcept().hasCoding()) {
				medCodes.addAll(medReq.getMedicationCodeableConcept().getCoding().stream()
					.map(Coding::getCode).collect(Collectors.toList()));
			}
		}
		return medCodes;
	}

	public BooleanType useServerData() {
		return new BooleanType(false);
	}

	public Bundle getPrefetchBundle() {
		var data = CdsHooksUtil.getPrefetchResources(request);
		// Use prefetch resources if provided otherwise use MCL
		if (data == null) {
			var configResolver = new EpicModuleConfigurationResolver(fhirContext, remoteDataEndpoint, request);
			data = configResolver.getPrefetchBundle();
			logging.logMclQueryPerformanceResult(configResolver.getQueryResultMap());
		}

		if (draftOrders != null) {
			// Add non-request resources (e.g. referenced Medications) to bundle
			CdsHooksUtil.addNonRequestResourcesFromContextToDataBundle(draftOrders, data);
		}

		// Get the patient MRN for logging
		var patient = BundleUtil.toListOfResourcesOfType(
			fhirContext, data, Patient.class).stream().findFirst().orElseThrow();
		if (patient.hasIdentifier()) {
			var mrn = patient.getIdentifier().stream().filter(
				identifier -> identifier.hasType() && identifier.getType().hasText()
					&& identifier.getType().getText().equals("EPICMRN")).findFirst();
			mrn.ifPresent(identifier -> this.mrn = identifier.getValue());
		}

		return data;
	}
}
