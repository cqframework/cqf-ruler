package org.opencds.cqf.ruler.utility;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.tracking.TrackBack;

public class Translators {

	private Translators() {}

	public static String errorsToString(Iterable<CqlTranslatorException> exceptions) {
		ArrayList<String> errors = new ArrayList<>();
		for (CqlTranslatorException error : exceptions) {
			TrackBack tb = error.getLocator();
			String lines = tb == null ? "[n/a]"
				: String.format("%s[%d:%d, %d:%d]",
				(tb.getLibrary() != null ? tb.getLibrary().getId()
					+ (tb.getLibrary().getVersion() != null ? ("-" + tb.getLibrary().getVersion()) : "")
					: ""),
				tb.getStartLine(), tb.getStartChar(), tb.getEndLine(), tb.getEndChar());
			errors.add(lines + error.getMessage());
		}

		return errors.toString();
	}

	public static CqlTranslator getTranslator(String cql, LibraryManager libraryManager, ModelManager modelManager) {
		return getTranslator(new ByteArrayInputStream(cql.getBytes(StandardCharsets.UTF_8)), libraryManager,
			modelManager);
	}

	public static List<CqlTranslator.Options> generateDefaultTranslatorOptions() {
		ArrayList<CqlTranslator.Options> options = new ArrayList<>();
		options.add(CqlTranslator.Options.EnableAnnotations);
		options.add(CqlTranslator.Options.EnableLocators);
		options.add(CqlTranslator.Options.DisableListDemotion);
		options.add(CqlTranslator.Options.DisableListPromotion);
		options.add(CqlTranslator.Options.DisableMethodInvocation);
		return options;
	}

	public static CqlTranslator getTranslator(InputStream cqlStream, LibraryManager libraryManager,
													ModelManager modelManager) {

		CqlTranslator translator;
		try {
			List<CqlTranslator.Options> options = generateDefaultTranslatorOptions();
			translator = CqlTranslator.fromStream(cqlStream, modelManager, libraryManager,
				options.toArray(new CqlTranslator.Options[options.size()]));
		} catch (IOException e) {
			throw new IllegalArgumentException(
				String.format("Errors occurred translating library: %s", e.getMessage()));
		}

		return translator;
	}

	public static CqlTranslatorOptions getTranslatorOptions() {
		CqlTranslatorOptions cqlTranslatorOptions = new CqlTranslatorOptions();
		cqlTranslatorOptions.getOptions().addAll(generateDefaultTranslatorOptions());
		cqlTranslatorOptions.setAnalyzeDataRequirements(true);
		cqlTranslatorOptions.setCollapseDataRequirements(true);
		return cqlTranslatorOptions;
	}
}
