package org.opencds.cqf.r4.evaluation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Triple;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.opencds.cqf.common.evaluation.EvaluationProviderFactory;
import org.opencds.cqf.common.helpers.DateHelper;
import org.opencds.cqf.common.helpers.UsingHelper;
import org.opencds.cqf.common.providers.LibraryResolutionProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.debug.DebugMap;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.r4.helpers.LibraryHelper;

public class MeasureEvaluationSeeder {
    private final EvaluationProviderFactory providerFactory;
    private final LibraryLoader libraryLoader;
    private final LibraryResolutionProvider<Library> libraryResourceProvider;

    private boolean enableExpressionCaching;
    private boolean debugMode;
    private String terminologyProviderSource;
    private String terminologyProviderUser;
    private String terminologyProviderPassword;
    private Collection<ContextParameter> contextParameters;
    private Interval measurementPeriod;

    public MeasureEvaluationSeeder(
            EvaluationProviderFactory providerFactory,
            LibraryLoader libraryLoader,
            LibraryResolutionProvider<Library> libraryResourceProvider) {
        this.providerFactory = providerFactory;
        this.libraryLoader = libraryLoader;
        this.libraryResourceProvider = libraryResourceProvider;
    }

    public MeasureEvaluationSeeder usingDefaults() {
        this.debugMode = true;
        this.enableExpressionCaching = true;
        this.contextParameters = new ArrayList<>();

        return this;
    }

    public MeasureEvaluationSeeder withMeasurementPeriod(String periodStart, String periodEnd) {
        this.measurementPeriod = createMeasurePeriod(periodStart, periodEnd);
        ContextParameter parameter = new ContextParameter(null, "Measurement Period", convertDateToDateTime(this.measurementPeriod));
        this.contextParameters.add(parameter);

        return this;
    }

    public MeasureEvaluationSeeder disableDebugLogging() {
        this.debugMode = false;

        return this;
    }

    public MeasureEvaluationSeeder disableExpressionCaching() {
        this.enableExpressionCaching = false;

        return this;
    }

    public MeasureEvaluationSeeder withTerminologyProvider(String source, String user, String password) {
        this.terminologyProviderSource = source;
        this.terminologyProviderUser = user;
        this.terminologyProviderPassword = password;

        return this;
    }

    public MeasureEvaluationSeeder withContextParameter(String libraryName, String name, String value) {
        ContextParameter parameter = new ContextParameter(libraryName, name, value);
        contextParameters.add(parameter);

        return this;
    }

    public MeasureEvaluationSeeder withProductLine(String productLine) {
        ContextParameter parameter = new ContextParameter(null, "Product Line", productLine);
        contextParameters.add(parameter);

        return this;
    }

    public MeasureEvaluationSeed create(Measure measure) {
        LibraryHelper.loadLibraries(measure, this.libraryLoader, this.libraryResourceProvider);

        // resolve primary library
        org.cqframework.cql.elm.execution.Library library =
                LibraryHelper.resolvePrimaryLibrary(measure, this.libraryLoader, this.libraryResourceProvider);

        List<Triple<String, String, String>> usingDefs = UsingHelper.getUsingUrlAndVersion(library.getUsings());

        if (usingDefs.size() > 1) {
            throw new IllegalArgumentException(
                    "Evaluation of Measure using multiple Models is not supported at this time.");
        }

        TerminologyProvider terminologyProvider = createTerminologyProvider(usingDefs);
        LinkedHashMap<Triple<String, String, String>, DataProvider> dataProviders = createDataProviders(usingDefs, terminologyProvider);
        Context context = createContext(library, dataProviders, terminologyProvider);

        List<Map.Entry<Triple<String, String, String>, DataProvider>> dataProviderList = new ArrayList<>(dataProviders.entrySet());
        DataProvider lastDataProvider = dataProviderList.get(dataProviderList.size() - 1).getValue();

        return new MeasureEvaluationSeed(measure, context, this.measurementPeriod, lastDataProvider);
    }

    private Context createContext(
            org.cqframework.cql.elm.execution.Library library,
            Map<Triple<String, String, String>, DataProvider> dataProviders,
            TerminologyProvider terminologyProvider) {
        Context context = new Context(library);
        context.registerLibraryLoader(libraryLoader);

        if (!dataProviders.isEmpty()) {
            context.registerTerminologyProvider(terminologyProvider);
        }

        for (Map.Entry<Triple<String, String, String>, DataProvider> dataProviderEntry : dataProviders.entrySet()) {
            Triple<String, String, String> usingDef = dataProviderEntry.getKey();
            DataProvider dataProvider = dataProviderEntry.getValue();

            context.registerDataProvider(usingDef.getRight(), dataProvider);
        }

        for (ContextParameter parameter : contextParameters) {
            context.setParameter(parameter.getLibraryName(), parameter.getName(), parameter.getValue());
        }

        context.setExpressionCaching(enableExpressionCaching);

        DebugMap debugMap = new DebugMap();
        debugMap.setIsLoggingEnabled(debugMode);
        context.setDebugMap(debugMap);

        return context;
    }

    private static Interval createMeasurePeriod(String periodStart, String periodEnd) {
        return new Interval(DateHelper.resolveRequestDate(periodStart, true), true,
                            DateHelper.resolveRequestDate(periodEnd, false), true);
    }

    private static Interval convertDateToDateTime(Interval dateInterval) {
        return new Interval(DateTime.fromJavaDate((Date) dateInterval.getStart()), true,
                            DateTime.fromJavaDate((Date) dateInterval.getEnd()), true);
    }

    private LinkedHashMap<Triple<String, String, String>, DataProvider> createDataProviders(
            Iterable<Triple<String, String, String>> usingDefs,
            TerminologyProvider terminologyProvider) {
        LinkedHashMap<Triple<String, String, String>, DataProvider> dataProviders = new LinkedHashMap<>();

        for (Triple<String, String, String> def : usingDefs) {
            dataProviders.put(def, this.providerFactory.createDataProvider(def.getLeft(), def.getMiddle(), terminologyProvider));
        }

        return dataProviders;
    }

    private TerminologyProvider createTerminologyProvider(List<Triple<String, String, String>> usingDefs) {
        // If there are no Usings, there is probably not any place the Terminology
        // actually used so I think the assumption that at least one provider exists is
        // ok.
        TerminologyProvider terminologyProvider = null;
        if (!usingDefs.isEmpty()) {
            // Creates a terminology provider based on the first using statement. This
            // assumes the terminology
            // server matches the FHIR version of the CQL.
            terminologyProvider = this.providerFactory.createTerminologyProvider(
                    usingDefs.get(0).getLeft(),
                    usingDefs.get(0).getMiddle(),
                    terminologyProviderSource,
                    terminologyProviderUser,
                    terminologyProviderPassword);
        }

        return terminologyProvider;
    }

}
