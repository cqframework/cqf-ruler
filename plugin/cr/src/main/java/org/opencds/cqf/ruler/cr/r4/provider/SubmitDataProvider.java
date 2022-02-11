package org.opencds.cqf.ruler.cr.r4.provider;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class SubmitDataProvider extends DaoRegistryOperationProvider {

	/**
	 * Implements the <a href="http://hl7.org/fhir/R4/measure-operation-submit-data.html">$submit-data</a> operation found in the <a href="http://hl7.org/fhir/R4/clinicalreasoning-module.html">FHIR Clinical Reasoning Module</a>.
	 * 
	 * The submitted MeasureReport and Resources will be saved to the local server.
	 * A Bundle reporting the result of the transaction will be returned.
	 * 
	 * @param theRequestDetails generally auto-populated by the HAPI server
	 *                          framework.
	 * @param theId             the Id of the Measure to submit data for
	 * @param theReport         the MeasureReport to be submitted
	 * @param theResources      the resources to be submitted
	 * @return Bundle the transaction result
	 */
	@Description(shortDefinition = "$submit-data", value = "Implements the <a href=\"http://hl7.org/fhir/R4/measure-operation-submit-data.html\">$submit-data</a> operation found in the <a href=\"http://hl7.org/fhir/R4/clinicalreasoning-module.html\">FHIR Clinical Reasoning Module</a>.")
	@Operation(name = "$submit-data", idempotent = true, type = Measure.class)
	public Bundle submitData(RequestDetails theRequestDetails,
			@IdParam IdType theId,
			@OperationParam(name = "measureReport", min = 1, max = 1) MeasureReport theReport,
			@OperationParam(name = "resource") List<IBaseResource> theResources) {
		/*
		 * TODO - resource validation using $data-requirements operation (params are the
		 * provided id and the measurement period from the MeasureReport)
		 * 
		 * TODO - profile validation ... not sure how that would work ... (get
		 * StructureDefinition from URL or must it be stored in Ruler?)
		 */

		Bundle transactionBundle = new Bundle()
				.setType(Bundle.BundleType.TRANSACTION)
				.addEntry(createEntry(theReport));

		if (theResources != null) {
			for (IBaseResource res : theResources) {
				// Unpack nested Bundles
				if (res instanceof Bundle) {
					Bundle nestedBundle = (Bundle) res;
					for (Bundle.BundleEntryComponent entry : nestedBundle.getEntry()) {
						transactionBundle.addEntry(createEntry(entry.getResource()));
					}
				} else {
					transactionBundle.addEntry(createEntry(res));
				}
			}
		}

		return transaction(transactionBundle, theRequestDetails);
	}

	private Bundle.BundleEntryComponent createEntry(IBaseResource theResource) {
		return new Bundle.BundleEntryComponent()
			.setResource((Resource)theResource)
			.setRequest(createRequest(theResource));
	}

	private Bundle.BundleEntryRequestComponent createRequest(IBaseResource theResource) {
		Bundle.BundleEntryRequestComponent request = new Bundle.BundleEntryRequestComponent();
		if (theResource.getIdElement().hasValue()) {
			request
				.setMethod(Bundle.HTTPVerb.PUT)
				.setUrl(theResource.getIdElement().getValue());
		} else {
			request
				.setMethod(Bundle.HTTPVerb.POST)
				.setUrl(theResource.fhirType());
		}

		return request;
	}
}
