package org.opencds.cqf.ruler.cdshooks.r4.epic.util;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Patient;
import org.opencds.cqf.ruler.cdshooks.r4.CdsHooksUtil;
import org.opencds.cqf.ruler.cdshooks.r4.epic.EpicLogging;
import org.opencds.cqf.ruler.cdshooks.r4.epic.EpicModuleConfigurationResolver;
import org.opencds.cqf.ruler.cdshooks.request.CdsHooksRequest;

public class CdsHooksRequestHelper {

	private final FhirContext fhirContext = FhirContext.forR4();

	private final EpicLogging logging;

	private final CdsHooksRequest request;
	private final String serviceId;
	private final String patientId;
	private final DraftOrdersHelper draftOrdersHelper;
	private final Endpoint remoteDataEndpoint;
	private String mrn;

	public CdsHooksRequestHelper(CdsHooksRequest request, String serviceId, String serverAddress, EpicLogging logging) {
		this.request = request;
		this.serviceId = serviceId;
		this.logging = logging;

		if (request instanceof CdsHooksRequest.OrderSelect) {
			this.patientId = ((CdsHooksRequest.OrderSelect) request).context.patientId;
		} else if (request instanceof CdsHooksRequest.OrderSign) {
			this.patientId = ((CdsHooksRequest.OrderSign) request).context.patientId;
		} else {
			this.patientId = request.context.patientId;
		}

		this.draftOrdersHelper = new DraftOrdersHelper(request);

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

	public DraftOrdersHelper getDraftOrdersHelper() {
		return this.draftOrdersHelper;
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

		if (draftOrdersHelper.getDraftOrdersJson() != null) {
			// Add non-request resources (e.g. referenced Medications) to bundle
			CdsHooksUtil.addNonRequestResourcesFromContextToDataBundle(draftOrdersHelper.getDraftOrdersJson(), data);
		}

		// Get the patient MRN for logging
		var patient = BundleUtil.toListOfResourcesOfType(
			fhirContext, data, Patient.class).stream().findFirst().orElseThrow();
		if (patient.hasIdentifier()) {
			var mrn = patient.getIdentifier().stream().filter(
				identifier -> identifier.hasType() && identifier.getType().hasText()
					&& identifier.getType().getText().equals("EPICMRN")).findFirst();
			mrn.ifPresent(identifier -> this.mrn = identifier.getValue());
			logging.logInfo("Resolved prefetch for patient (MRN): " + this.mrn);
		}

		return data;
	}

	public String getCacheKey() {
		return this.patientId + "|" + draftOrdersHelper.canonicalizeConcept();
	}
}
