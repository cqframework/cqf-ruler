package org.opencds.cqf.r4.providers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Triple;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.elm.tracking.TrackBack;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.common.evaluation.EvaluationProviderFactory;
import org.opencds.cqf.common.evaluation.LibraryLoader;
import org.opencds.cqf.common.helpers.DateHelper;
import org.opencds.cqf.common.helpers.TranslatorHelper;
import org.opencds.cqf.common.helpers.UsingHelper;
import org.opencds.cqf.common.providers.LibraryResolutionProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.r4.helpers.CanonicalHelper;
import org.opencds.cqf.r4.helpers.FhirMeasureBundler;
import org.opencds.cqf.r4.helpers.LibraryHelper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;

/**
 * Created by Bryn on 1/16/2017.
 */
public class CqlExecutionProvider {
    private EvaluationProviderFactory providerFactory;
    private LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> libraryResourceProvider;
    private FhirContext context;

    public CqlExecutionProvider(LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> libraryResourceProvider,
            EvaluationProviderFactory providerFactory, FhirContext context) {
        this.providerFactory = providerFactory;
        this.libraryResourceProvider = libraryResourceProvider;
        this.context = context;
    }

    private LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> getLibraryResourceProvider() {
        return this.libraryResourceProvider;
    }

    private List<CanonicalType> cleanReferences(List<CanonicalType> references) {
        List<CanonicalType> cleanRefs = new ArrayList<>();
        List<CanonicalType> noDupes = new ArrayList<>();

        for (CanonicalType reference : references) {
            boolean dup = false;
            for (CanonicalType ref : noDupes) {
                if (ref.equalsDeep(reference)) {
                    dup = true;
                }
            }
            if (!dup) {
                noDupes.add(reference);
            }
        }
        for (CanonicalType reference : noDupes) {
            cleanRefs.add(new CanonicalType(reference.getValue().replace("#", "")));
        }
        return cleanRefs;
    }

    private Iterable<CanonicalType> getLibraryReferences(DomainResource instance) {
        List<CanonicalType> references = new ArrayList<>();

        if (instance.hasContained()) {
            for (Resource resource : instance.getContained()) {
                if (resource instanceof Library) {
                    resource.setId(resource.getIdElement().getIdPart().replace("#", ""));
                    getLibraryResourceProvider().update((Library) resource);
                    // getLibraryLoader().putLibrary(resource.getIdElement().getIdPart(),
                    // getLibraryLoader().toElmLibrary((Library) resource));
                }
            }
        }

        if (instance instanceof ActivityDefinition) {
            references.addAll(((ActivityDefinition) instance).getLibrary());
        }

        else if (instance instanceof PlanDefinition) {
            references.addAll(((PlanDefinition) instance).getLibrary());
        }

        else if (instance instanceof Measure) {
            references.addAll(((Measure) instance).getLibrary());
        }

        for (Extension extension : instance
                .getExtensionsByUrl("http://hl7.org/fhir/StructureDefinition/cqif-library")) {
            Type value = extension.getValue();

            if (value instanceof CanonicalType) {
                references.add((CanonicalType) value);
            }

            else {
                throw new RuntimeException("Library extension does not have a value of type reference");
            }
        }

        return cleanReferences(references);
    }

    private String buildIncludes(Iterable<CanonicalType> references) {
        StringBuilder builder = new StringBuilder();
        for (CanonicalType reference : references) {

            if (builder.length() > 0) {
                builder.append(" ");
            }

            builder.append("include ");

            // TODO: This assumes the libraries resource id is the same as the library name,
            // need to work this out better
            Library lib = this.libraryResourceProvider.resolveLibraryById(CanonicalHelper.getId(reference));
            if (lib.hasName()) {
                builder.append(lib.getName());
            } else {
                throw new RuntimeException("Library name unknown");
            }

            if (reference.hasValue() && reference.getValue().split("\\|").length > 1) {
                builder.append(" version '");
                builder.append(reference.getValue().split("\\|")[1]);
                builder.append("'");
            }

            builder.append(" called ");
            builder.append(lib.getName());
        }

        return builder.toString();
    }

    /* Evaluates the given CQL expression in the context of the given resource */
    /*
     * If the resource has a library extension, or a library element, that library
     * is loaded into the context for the expression
     */
    public Object evaluateInContext(DomainResource instance, String cql, String patientId) {
        Iterable<CanonicalType> libraries = getLibraryReferences(instance);

        String fhirVersion = this.context.getVersion().getVersion().getFhirVersionString();

        String source = String.format(
                "library LocalLibrary using FHIR version '" + fhirVersion + "' include FHIRHelpers version '"+ fhirVersion +"' called FHIRHelpers %s parameter %s %s parameter \"%%context\" %s define Expression: %s",
                buildIncludes(libraries), instance.fhirType(), instance.fhirType(), instance.fhirType(), cql);

        LibraryLoader libraryLoader = LibraryHelper.createLibraryLoader(this.getLibraryResourceProvider());

        org.cqframework.cql.elm.execution.Library library = TranslatorHelper.translateLibrary(source,
                libraryLoader.getLibraryManager(), libraryLoader.getModelManager());

        // resolve execution context
        Context context = setupContext(instance, patientId, libraryLoader, library);
        return context.resolveExpressionRef("Expression").evaluate(context);
    }

    public Object evaluateInContext(DomainResource instance, String cql, String patientId, Boolean aliasedExpression) {
        Iterable<CanonicalType> libraries = getLibraryReferences(instance);
        if (aliasedExpression) {
            Object result = null;
            for (CanonicalType reference : libraries) {
                Library lib = this.libraryResourceProvider.resolveLibraryById(CanonicalHelper.getId(reference));
                if (lib == null) {
                    throw new RuntimeException("Library with id " + reference.getIdBase() + "not found");
                }
                LibraryLoader libraryLoader = LibraryHelper.createLibraryLoader(this.getLibraryResourceProvider());
                // resolve primary library
                org.cqframework.cql.elm.execution.Library library = LibraryHelper.resolveLibraryById(lib.getId(),
                        libraryLoader, this.libraryResourceProvider);

                // resolve execution context
                Context context = setupContext(instance, patientId, libraryLoader, library);
                result = context.resolveExpressionRef(cql).evaluate(context);
                if (result != null) {
                    return result;
                }
            }
            throw new RuntimeException("Could not find Expression in Referenced Libraries");
        } else {
            return evaluateInContext(instance, cql, patientId);
        }
    }

    private Context setupContext(DomainResource instance, String patientId, LibraryLoader libraryLoader,
            org.cqframework.cql.elm.execution.Library library) {
        // Provide the instance as the value of the '%context' parameter, as well as the
        // value of a parameter named the same as the resource
        // This enables expressions to access the resource by root, as well as through
        // the %context attribute
        Context context = new Context(library);
        context.setParameter(null, instance.fhirType(), instance);
        context.setParameter(null, "%context", instance);
        context.setExpressionCaching(true);
        context.registerLibraryLoader(libraryLoader);
        context.setContextValue("Patient", patientId);
        context.registerDataProvider("http://hl7.org/fhir", this.providerFactory.createDataProvider("FHIR", this.context.getVersion().getVersion().getFhirVersionString()));
        return context;
    }

    @SuppressWarnings("unchecked")
    @Operation(name = "$cql")
    public Bundle evaluate(@OperationParam(name = "code") String code,
            @OperationParam(name = "patientId") String patientId,
            @OperationParam(name = "periodStart") String periodStart,
            @OperationParam(name = "periodEnd") String periodEnd,
            @OperationParam(name = "productLine") String productLine,
            @OperationParam(name = "terminologyServiceUri") String terminologyServiceUri,
            @OperationParam(name = "terminologyUser") String terminologyUser,
            @OperationParam(name = "terminologyPass") String terminologyPass,
            @OperationParam(name = "context") String contextParam,
            @OperationParam(name = "executionResults") String executionResults,
            @OperationParam(name = "parameters") Parameters parameters) {

        if (patientId == null && contextParam != null && contextParam.equals("Patient")) {
            throw new IllegalArgumentException("Must specify a patientId when executing in Patient context.");
        }

        CqlTranslator translator;
        FhirMeasureBundler bundler = new FhirMeasureBundler();

        LibraryLoader libraryLoader = LibraryHelper.createLibraryLoader(this.getLibraryResourceProvider());

        List<Resource> results = new ArrayList<>();

        try {
            translator = TranslatorHelper.getTranslator(code, libraryLoader.getLibraryManager(),
                    libraryLoader.getModelManager());

            if (translator.getErrors().size() > 0) {
                for (CqlTranslatorException cte : translator.getErrors()) {
                    Parameters result = new Parameters();
                    TrackBack tb = cte.getLocator();
                    if (tb != null) {
                        String location = String.format("[%d:%d]", tb.getStartLine(), tb.getStartChar());
                        result.addParameter().setName("location").setValue(new StringType(location));
                    }

                    result.setId("Error");
                    result.addParameter().setName("error").setValue(new StringType(cte.getMessage()));
                    results.add(result);
                }

                return bundler.bundle(results);
            }
        } catch (IllegalArgumentException e) {
            Parameters result = new Parameters();
            result.setId("Error");
            result.addParameter().setName("error").setValue(new StringType(e.getMessage()));
            results.add(result);
            return bundler.bundle(results);
        }

        Map<String, List<Integer>> locations = getLocations(translator.getTranslatedLibrary().getLibrary());

        org.cqframework.cql.elm.execution.Library library = TranslatorHelper.translateLibrary(translator);
        Context context = new Context(library);
        context.registerLibraryLoader(libraryLoader);

        List<Triple<String, String, String>> usingDefs = UsingHelper.getUsingUrlAndVersion(library.getUsings());

        if (usingDefs.size() > 1) {
            throw new IllegalArgumentException(
                    "Evaluation of Measure using multiple Models is not supported at this time.");
        }

        // If there are no Usings, there is probably not any place the Terminology
        // actually used so I think the assumption that at least one provider exists is
        // ok.
        TerminologyProvider terminologyProvider = null;
        if (usingDefs.size() > 0) {
            // Creates a terminology provider based on the first using statement. This
            // assumes the terminology
            // server matches the FHIR version of the CQL.
            terminologyProvider = this.providerFactory.createTerminologyProvider(usingDefs.get(0).getLeft(),
                    usingDefs.get(0).getMiddle(), terminologyServiceUri, terminologyUser, terminologyPass);
            context.registerTerminologyProvider(terminologyProvider);
        }

        for (Triple<String, String, String> def : usingDefs) {
            DataProvider dataProvider = this.providerFactory.createDataProvider(def.getLeft(), def.getMiddle(),
                    terminologyProvider);
            context.registerDataProvider(def.getRight(), dataProvider);
        }

        if (parameters != null) {
            for (Parameters.ParametersParameterComponent pc : parameters.getParameter()) {
                context.setParameter(library.getLocalId(), pc.getName(), pc.getValue());
            }
        }

        if (periodStart != null && periodEnd != null) {
            // resolve the measurement period
            Interval measurementPeriod = new Interval(DateHelper.resolveRequestDate(periodStart, true), true,
                    DateHelper.resolveRequestDate(periodEnd, false), true);

            context.setParameter(null, "Measurement Period",
                    new Interval(DateTime.fromJavaDate((Date) measurementPeriod.getStart()), true,
                            DateTime.fromJavaDate((Date) measurementPeriod.getEnd()), true));
        }

        if (productLine != null) {
            context.setParameter(null, "Product Line", productLine);
        }

        context.setExpressionCaching(true);
        if (library.getStatements() != null) {
            for (org.cqframework.cql.elm.execution.ExpressionDef def : library.getStatements().getDef()) {
                context.enterContext(def.getContext());
                if (patientId != null && !patientId.isEmpty()) {
                    context.setContextValue(context.getCurrentContext(), patientId);
                } else {
                    context.setContextValue(context.getCurrentContext(), "null");
                }
                Parameters result = new Parameters();

                try {
                    result.setId(def.getName());
                    String location = String.format("[%d:%d]", locations.get(def.getName()).get(0),
                            locations.get(def.getName()).get(1));
                    result.addParameter().setName("location").setValue(new StringType(location));

                    Object res = def instanceof org.cqframework.cql.elm.execution.FunctionDef
                            ? "Definition successfully validated"
                            : def.getExpression().evaluate(context);

                    if (res == null) {
                        result.addParameter().setName("value").setValue(new StringType("null"));
                    } else if (res instanceof List<?>) {
                        if (((List<?>) res).size() > 0 && ((List<?>) res).get(0) instanceof Resource) {
                            if (executionResults != null && executionResults.equals("Summary")) {
                                result.addParameter().setName("value")
                                        .setValue(new StringType(((Resource) ((List<?>) res).get(0)).getIdElement()
                                                .getResourceType() + "/"
                                                + ((Resource) ((List<?>) res).get(0)).getIdElement().getIdPart()));
                            } else {
                                result.addParameter().setName("value").setResource(bundler.bundle((Iterable<Resource>) res));
                            }
                        } else {
                            result.addParameter().setName("value").setValue(new StringType(res.toString()));
                        }
                    } else if (res instanceof Iterable) {
                        result.addParameter().setName("value").setResource(bundler.bundle((Iterable<Resource>) res));
                    } else if (res instanceof Resource) {
                        if (executionResults != null && executionResults.equals("Summary")) {
                            result.addParameter().setName("value")
                                    .setValue(new StringType(((Resource) res).getIdElement().getResourceType() + "/"
                                            + ((Resource) res).getIdElement().getIdPart()));
                        } else {
                            result.addParameter().setName("value").setResource((Resource) res);
                        }
                    } else {
                        result.addParameter().setName("value").setValue(new StringType(res.toString()));
                    }

                    result.addParameter().setName("resultType").setValue(new StringType(resolveType(res)));
                } catch (RuntimeException re) {
                    re.printStackTrace();

                    String message = re.getMessage() != null ? re.getMessage() : re.getClass().getName();
                    result.addParameter().setName("error").setValue(new StringType(message));
                }
                results.add(result);
            }
        }

        return bundler.bundle(results);
    }

    private Map<String, List<Integer>> getLocations(org.hl7.elm.r1.Library library) {
        Map<String, List<Integer>> locations = new HashMap<>();

        if (library.getStatements() == null)
            return locations;

        for (org.hl7.elm.r1.ExpressionDef def : library.getStatements().getDef()) {
            int startLine = def.getTrackbacks().isEmpty() ? 0 : def.getTrackbacks().get(0).getStartLine();
            int startChar = def.getTrackbacks().isEmpty() ? 0 : def.getTrackbacks().get(0).getStartChar();
            List<Integer> loc = Arrays.asList(startLine, startChar);
            locations.put(def.getName(), loc);
        }

        return locations;
    }

    private String resolveType(Object result) {
        String type = result == null ? "Null" : result.getClass().getSimpleName();
        switch (type) {
            case "BigDecimal":
                return "Decimal";
            case "ArrayList":
                return "List";
            case "FhirBundleCursor":
                return "Retrieve";
        }
        return type;
    }
}
