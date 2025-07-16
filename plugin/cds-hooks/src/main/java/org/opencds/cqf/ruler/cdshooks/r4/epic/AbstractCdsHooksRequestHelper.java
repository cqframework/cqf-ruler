package org.opencds.cqf.ruler.cdshooks.r4.epic;

import ca.uhn.fhir.context.FhirContext;
import com.google.gson.JsonObject;
import org.hl7.fhir.instance.model.api.IBaseBooleanDatatype;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.ruler.cdshooks.request.CdsHooksRequest;

public abstract class AbstractCdsHooksRequestHelper {
	private final FhirContext fhirContext;
	private final CdsHooksRequest request;
	private final String serviceId;
	private final String serverAddress;

	private final String patientId;
	private final JsonObject draftOrdersJson;

	public AbstractCdsHooksRequestHelper(FhirContext fhirContext, CdsHooksRequest request, String serviceId, String serverAddress) {
		this.fhirContext = fhirContext;
		this.request = request;
		this.serviceId = serviceId;
		this.serverAddress = serverAddress;

		if (request instanceof CdsHooksRequest.OrderSelect) {
			this.patientId = ((CdsHooksRequest.OrderSelect) request).context.patientId;
			this.draftOrdersJson = ((CdsHooksRequest.OrderSelect) request).context.draftOrders;
		} else if (request instanceof CdsHooksRequest.OrderSign) {
			this.patientId = ((CdsHooksRequest.OrderSign) request).context.patientId;
			this.draftOrdersJson = ((CdsHooksRequest.OrderSign) request).context.draftOrders;
		} else {
			this.patientId = request.context.patientId;
			this.draftOrdersJson = null;
		}
	}

	public FhirContext getFhirContext() {
		return fhirContext;
	}
	public CdsHooksRequest getRequest() {
		return this.request;
	}
	public String getServiceId() {
		return this.serviceId;
	}
	public String getServerAddress() {
		return this.serverAddress;
	}
	public String getPatientId() {
		return this.patientId;
	}
	public JsonObject getDraftOrdersJson() {
		return this.draftOrdersJson;
	}

	public abstract IBaseResource getRemoteDataEndpoint();
	public abstract IBaseParameters getDraftOrdersParameters();
	public abstract IBaseBundle getDraftOrdersBundle();
	public abstract IBaseBundle getPrefetchBundle();
	public abstract IBaseBooleanDatatype useServerData();
}
