package org.opencds.cqf.ruler.plugin.cpg.r4.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Triple;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.tracking.TrackBack;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Attachment;
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
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.debug.DebugMap;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.ruler.plugin.cpg.utilities.CanonicalUtilities;
import org.opencds.cqf.ruler.plugin.cpg.utilities.ExecutionUtilities;
import org.opencds.cqf.ruler.plugin.cpg.utilities.LibraryUtilities;
import org.opencds.cqf.ruler.plugin.utility.OperatorUtilities;

import ca.uhn.fhir.cql.common.provider.EvaluationProviderFactory;
import ca.uhn.fhir.cql.common.provider.LibraryContentProvider;

import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.plugin.cpg.CpgProperties;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.cql.common.provider.LibraryResolutionProvider;
import ca.uhn.fhir.cql.r4.helper.LibraryHelper;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;

/**
 * Created by Bryn on 1/16/2017.
 */
public class CqlExecutionProvider implements OperationProvider, OperatorUtilities, LibraryUtilities, CanonicalUtilities, ExecutionUtilities {

    @Autowired
    private EvaluationProviderFactory providerFactory;
    @Autowired
    private LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> libraryResourceProvider;
    @Autowired
    private FhirContext fhirContext;
    @Autowired
    private LibraryHelper libraryHelper;
    @Autowired
    private ModelManager modelManager;
    @Autowired
    private CpgProperties cpgProperties;

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

    private String buildIncludes(Iterable<CanonicalType> references, RequestDetails requestDetails) {
        StringBuilder builder = new StringBuilder();
        for (CanonicalType reference : references) {

            if (builder.length() > 0) {
                builder.append(" ");
            }

            builder.append("include ");

            // TODO: This assumes the libraries resource id is the same as the library name,
            // need to work this out better
            Library lib = this.libraryResourceProvider.resolveLibraryById(this.getId(reference), requestDetails);
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
    public Object evaluateInContext(DomainResource instance, String cql, String patientId, RequestDetails requestDetails) {
        Iterable<CanonicalType> libraries = getLibraryReferences(instance);

        String fhirVersion = this.fhirContext.getVersion().getVersion().getFhirVersionString();

        String source = String.format(
                "library LocalLibrary using FHIR version '" + fhirVersion + "' include FHIRHelpers version '"+ fhirVersion +"' called FHIRHelpers %s parameter %s %s parameter \"%%context\" %s define Expression: %s",
                buildIncludes(libraries, requestDetails), instance.fhirType(), instance.fhirType(), instance.fhirType(), cql);

        LibraryLoader libraryLoader = this.libraryHelper.createLibraryLoader(this.getLibraryResourceProvider());

        org.cqframework.cql.elm.execution.Library library = this.translateLibrary(source, getLibraryManager(this.getLibraryResourceProvider()), modelManager);

        // resolve execution context
        Context context = setupContext(instance, patientId, libraryLoader, library, requestDetails);
        return context.resolveExpressionRef("Expression").evaluate(context);
    }

    public Object evaluateInContext(DomainResource instance, String cql, String patientId, Boolean aliasedExpression, RequestDetails requestDetails) {
        Iterable<CanonicalType> libraries = getLibraryReferences(instance);
        if (aliasedExpression) {
            Object result = null;
            for (CanonicalType reference : libraries) {
                Library lib = this.libraryResourceProvider.resolveLibraryById(this.getId(reference), requestDetails);
                if (lib == null) {
                    throw new RuntimeException("Library with id " + reference.getIdBase() + "not found");
                }
                LibraryLoader libraryLoader = this.libraryHelper.createLibraryLoader(this.getLibraryResourceProvider());
                // resolve primary library
                org.cqframework.cql.elm.execution.Library library = this.libraryHelper.resolveLibraryById(lib.getId(),
                        libraryLoader, this.libraryResourceProvider, requestDetails);

                // resolve execution context
                Context context = setupContext(instance, patientId, libraryLoader, library, requestDetails);
                result = context.resolveExpressionRef(cql).evaluate(context);
                if (result != null) {
                    return result;
                }
            }
            throw new RuntimeException("Could not find Expression in Referenced Libraries");
        } else {
            return evaluateInContext(instance, cql, patientId, requestDetails);
        }
    }

    private Context setupContext(DomainResource instance, String patientId, LibraryLoader libraryLoader,
            org.cqframework.cql.elm.execution.Library library, RequestDetails requestDetails) {
        // Provide the instance as the value of the '%context' parameter, as well as the
        // value of a parameter named the same as the resource
        // This enables expressions to access the resource by root, as well as through
        // the %context attribute
        Context context = new Context(library);
        context.setDebugMap(getDebugMap());
        context.setParameter(null, instance.fhirType(), instance);
        context.setParameter(null, "%context", instance);
        context.setExpressionCaching(true);
        context.registerLibraryLoader(libraryLoader);
        context.setContextValue("Patient", patientId);
        context.registerDataProvider("http://hl7.org/fhir", this.providerFactory.createDataProvider("FHIR", this.fhirContext.getVersion().getVersion().getFhirVersionString(), requestDetails));
        return context;
    }

    @SuppressWarnings("unchecked")
    @Operation(name = "$cql")
    public Bundle evaluate(RequestDetails theRequest,
            @OperationParam(name = "code") String code,
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

        LibraryLoader libraryLoader = this.libraryHelper.createLibraryLoader(this.getLibraryResourceProvider());

        List<IBaseResource> results = new ArrayList<>();

        try {
            translator = this.getTranslator(code, getLibraryManager(this.getLibraryResourceProvider()), modelManager);

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

                return (Bundle) this.bundle(results, fhirContext, theRequest.getFhirServerBase());
            }
        } catch (IllegalArgumentException e) {
            Parameters result = new Parameters();
            result.setId("Error");
            result.addParameter().setName("error").setValue(new StringType(e.getMessage()));
            results.add(result);
            return (Bundle) this.bundle(results, fhirContext, theRequest.getFhirServerBase());
        }

        Map<String, List<Integer>> locations = getLocations(translator.getTranslatedLibrary().getLibrary());

        org.cqframework.cql.elm.execution.Library library = this.translateLibrary(translator);
        Context context = new Context(library);
        context.setDebugMap(getDebugMap());
        context.registerLibraryLoader(libraryLoader);

        List<Triple<String, String, String>> usingDefs = this.getUsingUrlAndVersion(library.getUsings());

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
                    terminologyProvider, theRequest);
            context.registerDataProvider(def.getRight(), dataProvider);
        }

        if (parameters != null) {
            for (Parameters.ParametersParameterComponent pc : parameters.getParameter()) {
                context.setParameter(library.getLocalId(), pc.getName(), pc.getValue());
            }
        }

        if (periodStart != null && periodEnd != null) {
            // resolve the measurement period
            Interval measurementPeriod = new Interval(this.resolveRequestDate(periodStart, true), true,
                    this.resolveRequestDate(periodEnd, false), true);

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
                                result.addParameter().setName("value").setResource((Bundle) this.bundle((Iterable<IBaseResource>) res, fhirContext, theRequest.getFhirServerBase()));
                            }
                        } else {
                            result.addParameter().setName("value").setValue(new StringType(res.toString()));
                        }
                    } else if (res instanceof Iterable) {
                        result.addParameter().setName("value").setResource((Bundle) this.bundle((Iterable<IBaseResource>) res, fhirContext, theRequest.getFhirServerBase()));
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

        return (Bundle) this.bundle(results, fhirContext, theRequest.getFhirServerBase());
    }

    private LibraryManager getLibraryManager(LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> provider) {
		List<org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider> contentProviders = Collections.singletonList(new LibraryContentProvider<org.hl7.fhir.r4.model.Library, Attachment>(
			provider, x -> x.getContent(), x -> x.getContentType(), x -> x.getData()));
            
        LibraryManager libraryManager = new LibraryManager(modelManager);
        for (org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider contentProvider : contentProviders) {
            libraryManager.getLibrarySourceLoader().registerProvider(contentProvider);
        }

        return libraryManager;
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

    public DebugMap getDebugMap() {
        DebugMap debugMap = new DebugMap();
        if (cpgProperties.getCql_debug_enabled()) {
            debugMap.setIsLoggingEnabled(true);
        }
        return debugMap;
    }
}
