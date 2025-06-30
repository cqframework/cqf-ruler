package org.opencds.cqf.ruler.plugin.cdshooks.r4;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cdshooks.r4.epic.QueryTokenSubstitution;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

public class QueryTokenSubstitutionTests {

	@Test
	void testPatientIdSubstitution() {
		var patientId = UUID.randomUUID().toString();
		var urls = Arrays.asList("Patient/{{context.patientId}}", "MedicationRequest?patient={{context.patientId}}");
		var qts = new QueryTokenSubstitution(patientId, urls);
		var result = qts.substituteTokens();
		var expected = Arrays.asList("Patient/" + patientId, "MedicationRequest?patient=" + patientId);

		Assertions.assertIterableEquals(expected, result);
	}

	@Test
	void testDateSubstitution() {
		var patientId = UUID.randomUUID().toString();

		// Days
		var urls = Arrays.asList("Observation?category=laboratory&date=ge{{today() - 365 days}}",
			"MedicationRequest?date=ge{{today() - 396 days}}&status=active,completed,stopped&category=community&intent=order&_include=MedicationRequest:medication");
		var qts = new QueryTokenSubstitution(patientId, urls);
		var result = qts.substituteTokens();

		var todayMinus365Days = getTodayMinusX(null, null, 365);
		var todayMinus396Days = getTodayMinusX(null, null, 396);
		var expected = Arrays.asList("Observation?category=laboratory&date=ge" + todayMinus365Days,
			"MedicationRequest?date=ge" + todayMinus396Days + "&status=active,completed,stopped&category=community&intent=order&_include=MedicationRequest:medication");

		Assertions.assertIterableEquals(expected, result);

		// Months (and days)
		urls = Arrays.asList("Observation?category=laboratory&date=ge{{today() - 12 months}}",
			"MedicationRequest?date=ge{{today() - 13 months - 1 days}}&status=active,completed,stopped&category=community&intent=order&_include=MedicationRequest:medication");
		qts = new QueryTokenSubstitution(patientId, urls);
		result = qts.substituteTokens();

		var todayMinus12Months = getTodayMinusX(null, 12, null);
		var todayMinus13MonthsMinus1Day = getTodayMinusX(null, 13, 1);
		expected = Arrays.asList("Observation?category=laboratory&date=ge" + todayMinus12Months,
			"MedicationRequest?date=ge" + todayMinus13MonthsMinus1Day + "&status=active,completed,stopped&category=community&intent=order&_include=MedicationRequest:medication");

		Assertions.assertIterableEquals(expected, result);

		// Years (and months and days)
		urls = Arrays.asList("Observation?category=laboratory&date=ge{{today() - 1 year}}",
			"MedicationRequest?date=ge{{today() - 1 year - 1 month - 1 day}}&status=active,completed,stopped&category=community&intent=order&_include=MedicationRequest:medication");
		qts = new QueryTokenSubstitution(patientId, urls);
		result = qts.substituteTokens();

		var todayMinus1Year = getTodayMinusX(1, null, null);
		var todayMinus1YearMinus1MonthMinus1Day = getTodayMinusX(1, 1, 1);
		expected = Arrays.asList("Observation?category=laboratory&date=ge" + todayMinus1Year,
			"MedicationRequest?date=ge" + todayMinus1YearMinus1MonthMinus1Day + "&status=active,completed,stopped&category=community&intent=order&_include=MedicationRequest:medication");

		Assertions.assertIterableEquals(expected, result);
	}

	private String getTodayMinusX(Integer years, Integer months, Integer days) {
		LocalDate result = LocalDate.now();

		if (years != null) {
			result = result.minusYears(years);
		}

		if (months != null) {
			result = result.minusMonths(months);
		}

		if (days != null) {
			result = result.minusDays(days);
		}

		return QueryTokenSubstitution.DATE_FORMAT.format(result);
	}
}
