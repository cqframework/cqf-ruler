package org.opencds.cqf.ruler.utility;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;

public class OperationsTest {

	private static final String dateValid = "2022-01-01";
	private static final String dateValidAfter = "2022-06-01";
	private static final String dateInvalid = "bad-date";

	private static final RequestDetails requestDetails = new ServletRequestDetails();
	{
		requestDetails.addParameter("dateValid", new String[] { dateValid });
		requestDetails.addParameter("dateAfter", new String[] { dateValidAfter });
		requestDetails.addParameter("dateMultiple", new String[] { dateValid, dateValidAfter });
		requestDetails.addParameter("dateInvalid", new String[] { dateInvalid });
		requestDetails.addParameter("dateNull", new String[] { null });
		requestDetails.addParameter("dateEmpty", new String[] { "" });
	}

	// TODO - add more coverage

	@Test
	public void operationValidateDateValid() {
		Operations.validateDate("dateValid", dateValid);
	}

	@Test
	public void operationValidateDateInvalid() {
		assertThrows(IllegalArgumentException.class, () -> {
			Operations.validateDate("dateInvalid", dateInvalid);
		});
	}

	@Test
	public void operationValidateDateNull() {
		assertThrows(NullPointerException.class, () -> {
			Operations.validateDate("dateNull", null);
		});
	}

	@Test
	public void operationValidateDateEmpty() {
		assertThrows(IllegalArgumentException.class, () -> {
			Operations.validateDate("dateEmpty", "");
		});
	}

	@Test
	public void operationValidateSingularDateValid() {
		Operations.validateSingularDate(requestDetails, "dateValid");
	}

	@Test
	public void operationValidateSingularDateInvalid() {
		assertThrows(IllegalArgumentException.class, () -> {
			Operations.validateSingularDate(requestDetails, "dateInvalid");
		});
	}

	@Test
	public void operationValidateSingularDateNull() {
		assertThrows(NullPointerException.class, () -> {
			Operations.validateSingularDate(requestDetails, "dateNull");
		});
	}

	@Test
	public void operationValidateSingularDateEmpty() {
		assertThrows(IllegalArgumentException.class, () -> {
			Operations.validateSingularDate(requestDetails, "dateEmpty");
		});
	}

	@Test
	public void operationValidateSingularDateMissing() {
		assertThrows(IllegalArgumentException.class, () -> {
			Operations.validateSingularDate(requestDetails, "dateMissing");
		});
	}

	@Test
	public void operationValidateSingularDateMultiple() {
		assertThrows(IllegalArgumentException.class, () -> {
			Operations.validateSingularDate(requestDetails, "dateMultiple");
		});
	}

	@Test
	public void operationValidatePeriodValid() {
		Operations.validatePeriod(requestDetails, "dateValid", "dateAfter");
	}

	@Test
	public void operationValidatePeriodEndBeforeStart() {
		assertThrows(IllegalArgumentException.class, () -> {
			Operations.validatePeriod(requestDetails, "dateAfter", "dateValid");
		});
	}

}
