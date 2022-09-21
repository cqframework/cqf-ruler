package org.opencds.cqf.ruler.cr;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.fhir.r4.FhirLibrarySourceProvider;
import org.opencds.cqf.ruler.cql.utility.Translators;

public class CqlBuilder {

	private final StringBuilder stringBuilder;

	private CqlBuilder(String fhirVersion) {
		this.stringBuilder = new StringBuilder();
		this.stringBuilder.append(cql(fhirVersion));
	}

	public static CqlBuilder newCqlLibrary(String fhirVersion) {
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
		String cql = this.stringBuilder.toString();
		validate(cql);

		return cql;
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

	private void validate(String cql) {
		ModelManager modelManager = new ModelManager();
		LibraryManager libraryManager = new LibraryManager(modelManager);
		libraryManager.getLibrarySourceLoader().registerProvider(new FhirLibrarySourceProvider());
		CqlTranslator translator;
		try {
			translator = CqlTranslator.fromStream(new ByteArrayInputStream(cql.getBytes()), modelManager, libraryManager);
		} catch (IOException e) {
			throw new CqlCompilerException("Error validating cql", e);
		}

		if (translator.getErrors().size() > 0) {
			throw new CqlCompilerException("Error validating cql: " + Translators.errorsToString(translator.getErrors()));
		}
	}

}
