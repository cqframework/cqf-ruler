package org.opencds.cqf.ruler.cr.repo;

import java.util.HashMap;
import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IIdType;

import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class RequestDetailsCloner {

	private RequestDetailsCloner() {
	}

	static DetailsBuilder startWith(RequestDetails theDetails) {
		var newDetails = new RulerRequestDetails(theDetails);
		newDetails.setRequestType(RequestTypeEnum.POST);
		newDetails.setOperation(null);
		newDetails.setResource(null);
		newDetails.setParameters(new HashMap<>());
		newDetails.setResourceName(null);
		newDetails.setCompartmentName(null);

		return new DetailsBuilder(newDetails);
	}

	static class DetailsBuilder {
		private final RulerRequestDetails myDetails;

		DetailsBuilder(RulerRequestDetails theDetails) {
			myDetails = theDetails;
		}

		DetailsBuilder addHeaders(Map<String, String> theHeaders) {
			if (theHeaders != null) {
				for (var entry : theHeaders.entrySet()) {
					myDetails.addHeader(entry.getKey(), entry.getValue());
				}
			}

			return this;
		}

		DetailsBuilder setParameters(IBaseParameters theParameters) {
			myDetails.setResource(theParameters);

			return this;
		}

		DetailsBuilder setParameters(Map<String, String[]> theParameters) {
			myDetails.setParameters(theParameters);

			return this;
		}

		DetailsBuilder withRestOperationType(RequestTypeEnum theType) {
			myDetails.setRequestType(theType);

			return this;
		}

		DetailsBuilder setOperation(String theOperation) {
			myDetails.setOperation(theOperation);

			return this;
		}

		DetailsBuilder setResourceType(String theResourceName) {
			myDetails.setResourceName(theResourceName);

			return this;
		}

		DetailsBuilder setId(IIdType theId) {
			myDetails.setId(theId);

			return this;
		}

		RulerRequestDetails create() {
			return myDetails;
		}
	}
}
