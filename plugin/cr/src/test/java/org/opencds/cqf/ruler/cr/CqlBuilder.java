package org.opencds.cqf.ruler.cr;

public class CqlBuilder {

	private final StringBuilder stringBuilder;

	private CqlBuilder(String fhirVersion) {
		this.stringBuilder = new StringBuilder();
		this.stringBuilder.append(cql(fhirVersion));
	}

	public static CqlBuilder newCql(String fhirVersion) {
		return new CqlBuilder(fhirVersion);
	}

	public CqlBuilder addExpression(String name, String expression) {
		this.stringBuilder.append("\n");
		this.stringBuilder.append(cqlExpression(name, expression));
		return this;
	}

	public CqlBuilder addSdeRace() {
		addExpression(sdeRace(), sdeRaceExpression());
		return this;
	}

	public String build() {
		return this.stringBuilder.toString();
	}

	private String cql(String fhirVersion) {
		return libraryHeader(fhirVersion) + measurementPeriod() + patientContext();
	}

	private String libraryHeader(String fhirVersion) {
		return String.format(
				"library Test version '1.0.0'\n\nusing FHIR version '%1$s'\ninclude FHIRHelpers version '%1$s'\n\n",
				fhirVersion);
	}

	private String measurementPeriod() {
		return "parameter \"Measurement Period\" Interval<Date> default Interval[@2019-01-01, @2019-12-31]\n\n";
	}

	private String patientContext() {
		return "context Patient\n";
	}

	private String sdeRace() {
		return "SDE Race";
	}

	private String sdeRaceExpression() {
		return "  (flatten (\n" +
		"    Patient.extension Extension\n" +
		"      where Extension.url = 'http://hl7.org/fhir/us/core/StructureDefinition/us-core-race'\n" +
		"        return Extension.extension\n" +
		"  )) E\n" +
		"    where E.url = 'ombCategory'\n" +
		"      or E.url = 'detailed'\n" +
		"    return E.value as Coding\n\n";
	}

	private String cqlExpression(String name, String expression) {
		return "define \"" + name + "\":\n" + expression;
	}

}
