package org.opencds.cqf.ruler.utility;

import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import org.junit.jupiter.api.Test;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;

class OperationsTest {

	private static final String dateValid = "2022-01-01";
	private static final String dateValidAfter = "2022-06-01";
	private static final String dateInvalid = "bad-date";

	private static final RequestDetails requestDetails = new ServletRequestDetails();
	static {
		requestDetails.setRequestType(RequestTypeEnum.GET);
		requestDetails.addParameter("dateValid", new String[] { dateValid });
		requestDetails.addParameter("dateAfter", new String[] { dateValidAfter });
		requestDetails.addParameter("dateMultiple", new String[] { dateValid, dateValidAfter });
		requestDetails.addParameter("dateInvalid", new String[] { dateInvalid });
		requestDetails.addParameter("dateNull", new String[] { null });
		requestDetails.addParameter("dateEmpty", new String[] { "" });
	}

	// TODO - add more coverage

	@Test
	void operationValidateDateValid() {
		Operations.validateDate("dateValid", dateValid);
	}

	@Test
	void operationValidateDateInvalid() {
		assertThrows(IllegalArgumentException.class,
			() -> Operations.validateDate("dateInvalid", dateInvalid));
	}

	@Test
	void operationValidateDateNull() {
		assertThrows(NullPointerException.class,
			() -> Operations.validateDate("dateNull", null));
	}

	@Test
	void operationValidateDateEmpty() {
		assertThrows(IllegalArgumentException.class,
			() -> Operations.validateDate("dateEmpty", ""));
	}

	@Test
	void operationValidateSingularDateValid() {
		Operations.validateSingularDate(requestDetails, "dateValid");
	}

	@Test
	void operationValidateSingularDateInvalid() {
		assertThrows(IllegalArgumentException.class,
			() -> Operations.validateSingularDate(requestDetails, "dateInvalid"));
	}

	@Test
	void operationValidateSingularDateNull() {
		assertThrows(NullPointerException.class,
			() -> Operations.validateSingularDate(requestDetails, "dateNull"));
	}

	@Test
	void operationValidateSingularDateEmpty() {
		assertThrows(IllegalArgumentException.class,
			() -> Operations.validateSingularDate(requestDetails, "dateEmpty"));
	}

	@Test
	void operationValidateSingularDateMissing() {
		assertThrows(IllegalArgumentException.class,
			() -> Operations.validateSingularDate(requestDetails, "dateMissing"));
	}

	@Test
	void operationValidateSingularDateMultiple() {
		assertThrows(IllegalArgumentException.class,
			() -> Operations.validateSingularDate(requestDetails, "dateMultiple"));
	}

	@Test
	void operationValidatePeriodValid() {
		Operations.validatePeriod(requestDetails, "dateValid", "dateAfter");
	}

	@Test
	void operationValidatePeriodEndBeforeStart() {
		assertThrows(DataFormatException.class,
			() -> Operations.validatePeriod(requestDetails, "dateAfter", "dateValid"));
	}

}
