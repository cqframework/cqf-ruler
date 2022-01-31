package org.opencds.cqf.ruler.utility;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.rest.api.server.RequestDetails;

/**
 * This class provides utilities for implementing FHIR operations
 */
public class Operations {

	private Operations() {
	}

	public static final Pattern PATIENT_OR_GROUP_REFERENCE = Pattern
			.compile("(Patient|Group)\\/[A-Za-z0-9\\-\\.]{1,64}");

	public static final Pattern FHIR_DATE = Pattern
			.compile("-?[0-9]{4}(-(0[1-9]|1[0-2])(-(0[0-9]|[1-2][0-9]|3[0-1]))?)?");

	Logger ourLog = LoggerFactory.getLogger(Operations.class);

	/**
	 * This function converts a string representation of a FHIR period date to a
	 * java.util.Date.
	 * 
	 * @param date  the date to convert
	 * @param start whether the date is the start of a period
	 * @return the FHIR period date as a java.util.Date type
	 */
	public static Date resolveRequestDate(String date, boolean start) {
		// split it up - support dashes or slashes
		String[] dissect = date.contains("-") ? date.split("-") : date.split("/");
		List<Integer> dateVals = new ArrayList<>();
		for (String dateElement : dissect) {
			dateVals.add(Integer.parseInt(dateElement));
		}

		if (dateVals.isEmpty()) {
			throw new IllegalArgumentException("Invalid date");
		}

		// for now support dates up to day precision
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.setTimeZone(TimeZone.getDefault());
		calendar.set(Calendar.YEAR, dateVals.get(0));
		if (dateVals.size() > 1) {
			// java.util.Date months are zero based, hence the negative 1 -- 2014-01 ==
			// February 2014
			calendar.set(Calendar.MONTH, dateVals.get(1) - 1);
		}
		if (dateVals.size() > 2)
			calendar.set(Calendar.DAY_OF_MONTH, dateVals.get(2));
		else {
			if (start) {
				calendar.set(Calendar.DAY_OF_MONTH, 1);
			} else {
				// get last day of month for end period
				calendar.add(Calendar.MONTH, 1);
				calendar.set(Calendar.DAY_OF_MONTH, 1);
				calendar.add(Calendar.DATE, -1);
			}
		}
		return calendar.getTime();
	}

	/**
	 * This function returns a fullUrl for a resource.
	 * 
	 * @param serverAddress the address of the server
	 * @param fhirType      the type of the resource
	 * @param elementId     the id of the resource
	 * @return the FHIR period date as a java.util.Date type
	 */
	public static String getFullUrl(String serverAddress, String fhirType, String elementId) {
		String fullUrl = String.format("%s%s/%s", serverAddress + (serverAddress.endsWith("/") ? "" : "/"), fhirType,
				elementId);
		return fullUrl;
	}

	/**
	 * This function validates a string as a representation of a FHIR date.
	 * 
	 * @param theParameter the name of the parameter
	 * @param theValue     the value of the parameter
	 */
	public static void validateDate(String theParameter, String theValue) {
		checkArgument(FHIR_DATE.matcher(theValue).matches(), "Parameter '%s' must be a valid FHIR date.",
				theParameter);
	}

	/**
	 * This function validates a parameter as a string representation of a FHIR
	 * date.
	 * Precondition: the parameter has one and only one value.
	 * 
	 * @param theRequestDetails metadata about the current request being processed.
	 *                          Generally auto-populated by the HAPI FHIR server
	 *                          framework.
	 * @param theParameter      the name of the parameter
	 */
	public static void validateSingularDate(RequestDetails theRequestDetails, String theParameter) {
		validateCardinality(theRequestDetails, theParameter, 1, 1);
		validateDate(theParameter, theRequestDetails.getParameters().get(theParameter)[0]);
	}

	/**
	 * This function validates the min and max cardinality of a parameter.
	 * 
	 * @param theRequestDetails metadata about the current request being processed.
	 *                          Generally auto-populated by the HAPI FHIR server
	 *                          framework.
	 * @param theParameter      the name of the parameter
	 * @param min               the minimum number of values for the
	 *                          parameter
	 * @param max               the maximum number of values for the
	 *                          parameter
	 */
	public static void validateCardinality(RequestDetails theRequestDetails, String theParameter, int min, int max) {
		validateCardinality(theRequestDetails, theParameter, min);
		int count = theRequestDetails.getParameters().get(theParameter).length;
		checkArgument(count <= max, "Parameter '%s' must be provided a maximum of %s time(s).", theParameter,
				String.valueOf(max));
	}

	/**
	 * This function validates the min cardinality of a parameter.
	 * 
	 * @param theRequestDetails metadata about the current request being processed.
	 *                          Generally auto-populated by the HAPI FHIR server
	 *                          framework.
	 * @param theParameter      the name of the parameter
	 * @param min               the minimum number of values for the
	 *                          parameter
	 */
	public static void validateCardinality(RequestDetails theRequestDetails, String theParameter, int min) {
		if (min <= 0) {
			return;
		}
		int count = theRequestDetails.getParameters().get(theParameter).length;
		checkArgument(count >= min, "Parameter '%s' must be provided a minimum of %s time(s).", theParameter,
				String.valueOf(min));
	}

	/**
	 * This function validates the parameters are valid string representations of
	 * FHIR dates and that they are a valid interval.
	 * This includes that the start value is before the end value.
	 * Precondition: the start and end parameters have one and only one value.
	 * 
	 * @param theRequestDetails metadata about the current request being processed.
	 *                          Generally auto-populated by the HAPI FHIR server
	 *                          framework.
	 * @param theStartParameter the name of the start parameter
	 * @param theEndParameter   the name of the end parameter
	 *                          parameter
	 */
	public static void validatePeriod(RequestDetails theRequestDetails, String theStartParameter,
			String theEndParameter) {
		validateSingularDate(theRequestDetails, theStartParameter);
		validateSingularDate(theRequestDetails, theEndParameter);
		checkArgument(
				Operations.resolveRequestDate(theRequestDetails.getParameters().get(theStartParameter)[0], true)
						.before(Operations.resolveRequestDate(theRequestDetails.getParameters().get(theEndParameter)[0],
								false)),
				"Parameter '%s' must be before parameter '%s'.", theStartParameter, theEndParameter);
	}

	/**
	 * This function validates the value provided for a parameter matches a
	 * specified regex pattern.
	 * 
	 * @param theParameter the name of the parameter
	 * @param theValue     the value of the parameter
	 * @param thePattern   the regex pattern to match
	 */
	public static void validatePattern(String theParameter, String theValue, Pattern thePattern) {
		checkArgument(thePattern.matcher(theValue).matches(), "Parameter '%s' must match the pattern: %s", theParameter,
				thePattern);

	}

	/**
	 * This function validates the value of a named parameter matches a specified
	 * regex pattern.
	 * 
	 * @param theRequestDetails metadata about the current request being processed.
	 *                          Generally auto-populated by the HAPI FHIR server
	 *                          framework.
	 * @param theParameter      the name of the parameter
	 * @param thePattern        the regex pattern to match
	 */
	public static void validatePattern(RequestDetails theRequestDetails, String theParameter, Pattern thePattern) {
		String[] potentialValue = theRequestDetails.getParameters().get(theParameter);
		if (potentialValue.length == 0) {
			return;
		}
		validatePattern(theParameter, potentialValue[0], thePattern);

	}

	/**
	 * This function validates a parameter is included exclusive to a set of other
	 * parameters.
	 * Exclusivity is only enforced if the exclusive parameter has at least one
	 * value.
	 * 
	 * @param theRequestDetails     metadata about the current request being
	 *                              processed.
	 *                              Generally auto-populated by the HAPI FHIR server
	 *                              framework.
	 * @param theParameter          the name of the parameter that is exclusive
	 * @param theExcludedParameters the set of parameter(s) that are required to be
	 *                              excluded
	 */
	public static void validateExclusive(RequestDetails theRequestDetails, String theParameter,
			String... theExcludedParameters) {
		String[] potentialValue = theRequestDetails.getParameters().get(theParameter);
		if (potentialValue.length == 0) {
			return;
		}
		for (String excludedParameter : theExcludedParameters) {
			checkArgument(theRequestDetails.getParameters().get(excludedParameter).length > 0,
					"Parameter '%s' cannot be included with parameter '%s'.", excludedParameter, theParameter);
		}
	}

	/**
	 * This function validates a set of parameters are included with the use of a
	 * parameter.
	 * Inclusivity is only enforced if the inclusive parameter has at least one
	 * value.
	 * 
	 * @param theRequestDetails     metadata about the current request being
	 *                              processed.
	 *                              Generally auto-populated by the HAPI FHIR server
	 *                              framework.
	 * @param theParameter          the name of the parameter that is inclusive
	 * @param theIncludedParameters the set of parameter(s) that are required to be
	 *                              included
	 */
	public static void validateInclusive(RequestDetails theRequestDetails, String theParameter,
			String... theIncludedParameters) {
		String[] potentialValue = theRequestDetails.getParameters().get(theParameter);
		if (potentialValue.length == 0) {
			return;
		}
		for (String includedParameter : theIncludedParameters) {
			checkArgument(theRequestDetails.getParameters().get(includedParameter).length < 1,
					"Parameter '%s' must be included with parameter '%s'.", includedParameter, theParameter);
		}
	}

	/**
	 * This function validates that one and only one of two parameters has a
	 * value.
	 * It is an exception that neither of the parameters has a value.
	 * It is also an exception that both of the parameters has a value.
	 * 
	 * @param theRequestDetails metadata about the current request being
	 *                          processed.
	 *                          Generally auto-populated by the HAPI FHIR server
	 *                          framework.
	 * @param theLeftParameter  one of the two parameters
	 * @param theRightParameter the other of the two parameters
	 */
	public static void validateExclusiveOr(RequestDetails theRequestDetails, String theLeftParameter,
			String theRightParameter) {
		checkArgument(
				theRequestDetails.getParameters().get(theLeftParameter).length > 0
						^ theRequestDetails.getParameters().get(theRightParameter).length > 0,
				"Either parameter '%s' or parameter '%s' must be included, but not both.", theLeftParameter,
				theRightParameter);
	}

	/**
	 * This function validates that at least one of a set of parameters has a value.
	 * It is an exception that none of the parameters has a value.
	 * It is not an exception that some or all of the parameters have a value.
	 * 
	 * @param theRequestDetails metadata about the current request being
	 *                          processed.
	 *                          Generally auto-populated by the HAPI FHIR server
	 *                          framework.
	 * @param theParameters     the set of parameters to validate
	 */
	public static void validateAtLeastOne(RequestDetails theRequestDetails, String... theParameters) {
		for (String includedParameter : theParameters) {
			if (theRequestDetails.getParameters().get(includedParameter).length > 1) {
				return;
			}
		}

		throw new IllegalArgumentException(String
				.format("At least one of the following parameters must be included: '%s'.", (Object[]) theParameters));

	}
}
