package org.opencds.cqf.ruler.cdshooks.r4.epic;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import com.google.gson.Gson;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.opencds.cqf.ruler.cdshooks.r4.CdsHooksUtil;
import org.opencds.cqf.ruler.cdshooks.request.CdsHooksRequest;

public class MUSCR4CdsHooksRequestHelper extends AbstractCdsHooksRequestHelper {

	private final EpicLogging logging;
	private String mrn;

	public MUSCR4CdsHooksRequestHelper(CdsHooksRequest request, String serviceId, String serverAddress, EpicLogging logging) {
		super(FhirContext.forR4Cached(), request, serviceId, serverAddress);
		this.logging = logging;
	}

	public String getMrn() {
		return this.mrn;
	}

	@Override
	public Endpoint getRemoteDataEndpoint() {
		if (getRequest().fhirServer != null && !getRequest().fhirServer.equals(getServerAddress())) {
			var ep = new Endpoint().setAddress(getRequest().fhirServer);
			if (getRequest().fhirAuthorization != null) {
				ep.addHeader(String.format("Authorization: %s %s",
					getRequest().fhirAuthorization.tokenType,
					getRequest().fhirAuthorization.accessToken)
				);
			}
			return ep;
		} else {
			return new Endpoint().setAddress(getServerAddress());
		}
	}

	@Override
	public Parameters getDraftOrdersParameters() {
		return getDraftOrdersJson() == null ? null : CdsHooksUtil.getParameters(getDraftOrdersJson());
	}

	@Override
	public Bundle getDraftOrdersBundle() {
		return getDraftOrdersJson() == null ? null : getFhirContext().newJsonParser().parseResource(
			Bundle.class, new Gson().toJson(getDraftOrdersJson()));
	}

	@Override
	public Bundle getPrefetchBundle() {
		var data = CdsHooksUtil.getPrefetchResources(getRequest());
		// Use prefetch resources if provided otherwise use MCL
		if (data == null) {
			var configResolver = new MUSCModuleConfigurationResolver(getFhirContext(), getRemoteDataEndpoint(), getRequest());
			data = configResolver.getPrefetchBundle();
			logging.logMclQueryPerformanceResult(configResolver.getQueryResultMap());
		}

		if (getDraftOrdersJson() != null) {
			// Add non-request resources (e.g. referenced Medications) to bundle
			CdsHooksUtil.addNonRequestResourcesFromContextToDataBundle(getDraftOrdersJson(), data);
		}

		// Get the patient MRN for logging
		var patient = BundleUtil.toListOfResourcesOfType(
			getFhirContext(), data, Patient.class).stream().findFirst().orElseThrow();
		if (patient.hasIdentifier()) {
			var mrn = patient.getIdentifier().stream().filter(
				identifier -> identifier.hasType() && identifier.getType().hasText()
					&& identifier.getType().getText().equals("EPICMRN")).findFirst();
			mrn.ifPresent(identifier -> this.mrn = identifier.getValue());
		}

		return data;
	}

	@Override
	public BooleanType useServerData() {
		return new BooleanType(false);
	}
}
