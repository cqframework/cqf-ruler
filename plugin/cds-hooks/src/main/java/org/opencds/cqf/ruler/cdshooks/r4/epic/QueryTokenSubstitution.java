package org.opencds.cqf.ruler.cdshooks.r4.epic;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class QueryTokenSubstitution {
	private final String patientId;
	private final List<String> urls;

	public static final Pattern DATE_TOKEN = Pattern.compile(
			"\\{\\{\\s*today\\(\\)"
				+ "((?:\\s*-\\s*\\d+\\s*(?:years?|months?|days?))+\\s*)}}",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern UNIT_PART = Pattern.compile(
		"\\s*-\\s*(\\d+)\\s*(years?|months?|days?)",
		Pattern.CASE_INSENSITIVE);
	public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	public QueryTokenSubstitution(String patientId, List<String> urls) {
		this.patientId = patientId;
		this.urls = urls;
	}

	public List<String> substituteTokens() {
		final var today = LocalDate.now();

		return urls.stream()
			.map(url -> {
				var result = url.replace("{{context.patientId}}", patientId);
				result = DATE_TOKEN.matcher(result).replaceAll(mr -> {
					var adjusted = today;
					var part = UNIT_PART.matcher(mr.group(1));

					while (part.find()) {
						long amount = Long.parseLong(part.group(1));
						switch (part.group(2).toLowerCase()) {
							case "day":   // fall through
							case "days":
								adjusted = adjusted.minusDays(amount);
								break;
							case "month":
							case "months":
								adjusted = adjusted.minusMonths(amount);
								break;
							case "year":
							case "years":
								adjusted = adjusted.minusYears(amount);
								break;
						}
					}
					return DATE_FORMAT.format(adjusted);
				});
				return result;
			})
			.collect(Collectors.toList());
	}
}
