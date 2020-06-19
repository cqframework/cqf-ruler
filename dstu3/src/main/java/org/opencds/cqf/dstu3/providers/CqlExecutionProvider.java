package org.opencds.cqf.dstu3.providers;

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
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.Type;
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
import org.opencds.cqf.dstu3.helpers.FhirMeasureBundler;
import org.opencds.cqf.dstu3.helpers.LibraryHelper;

import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;

/**
 * Created by Bryn on 1/16/2017.
 */
public class CqlExecutionProvider {
    private EvaluationProviderFactory providerFactory;
    private LibraryResolutionProvider<Library> libraryResolutionProvider;

    public CqlExecutionProvider(LibraryResolutionProvider<Library> libraryResolutionProvider,
            EvaluationProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
        this.libraryResolutionProvider = libraryResolutionProvider;
    }

    private LibraryResolutionProvider<Library> getLibraryResourceProvider() {
        return this.libraryResolutionProvider;
    }

    private List<Reference> cleanReferences(List<Reference> references) {
        List<Reference> cleanRefs = new ArrayList<>();
        List<Reference> noDupes = new ArrayList<>();

        for (Reference reference : references) {
            boolean dup = false;
            for (Reference ref : noDupes) {
                if (ref.equalsDeep(reference)) {
                    dup = true;
                }
            }
            if (!dup) {
                noDupes.add(reference);
            }
        }
        for (Reference reference : noDupes) {
            cleanRefs.add(new Reference(new IdType(reference.getReferenceElement().getResourceType(),
                    reference.getReferenceElement().getIdPart().replace("#", ""),
                    reference.getReferenceElement().getVersionIdPart())));
        }
        return cleanRefs;
    }

    private Iterable<Reference> getLibraryReferences(DomainResource instance) {
        List<Reference> references = new ArrayList<>();

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

            if (value instanceof Reference) {
                references.add((Reference) value);
            }

            else {
                throw new RuntimeException("Library extension does not have a value of type reference");
            }
        }

        return cleanReferences(references);
    }

    private String buildIncludes(Iterable<Reference> references) {
        StringBuilder builder = new StringBuilder();
        for (Reference reference : references) {

            if (builder.length() > 0) {
                builder.append(" ");
            }

            builder.append("include ");

            // TODO: This assumes the libraries resource id is the same as the library name,
            // need to work this out better
            builder.append(reference.getReferenceElement().getIdPart());

            if (reference.getReferenceElement().getVersionIdPart() != null) {
                builder.append(" version '");
                builder.append(reference.getReferenceElement().getVersionIdPart());
                builder.append("'");
            }

            builder.append(" called ");
            builder.append(reference.getReferenceElement().getIdPart());
        }

        return builder.toString();
    }

    /* Evaluates the given CQL expression in the context of the given resource */
    /*
     * If the resource has a library extension, or a library element, that library
     * is loaded into the context for the expression
     */
    public Object evaluateInContext(DomainResource instance, String cql, String patientId) {
        Iterable<Reference> libraries = getLibraryReferences(instance);
        //String fhirVersion = this.context.getVersion().getVersion().getFhirVersionString();
        String fhirVersion = "3.0.0";

        // Provide the instance as the value of the '%context' parameter, as well as the
        // value of a parameter named the same as the resource
        // This enables expressions to access the resource by root, as well as through
        // the %context attribute
        String source = String.format(
                "library LocalLibrary using FHIR version '"+ fhirVersion + "' include FHIRHelpers version '"+ fhirVersion + "' called FHIRHelpers %s parameter %s %s parameter \"%%context\" %s define Expression: %s",
                buildIncludes(libraries), instance.fhirType(), instance.fhirType(), instance.fhirType(), cql);
        // String source = String.format("library LocalLibrary using FHIR version '1.8'
        // include FHIRHelpers version '1.8' called FHIRHelpers %s parameter %s %s
        // parameter \"%%context\" %s define Expression: %s",
        // buildIncludes(libraries), instance.fhirType(), instance.fhirType(),
        // instance.fhirType(), cql);

        LibraryLoader libraryLoader = LibraryHelper.createLibraryLoader(this.libraryResolutionProvider);

        org.cqframework.cql.elm.execution.Library library = TranslatorHelper.translateLibrary(source,
                libraryLoader.getLibraryManager(), libraryLoader.getModelManager());
        Context context = new Context(library);
        context.setParameter(null, instance.fhirType(), instance);
        context.setParameter(null, "%context", instance);
        context.setExpressionCaching(true);
        context.registerLibraryLoader(libraryLoader);
        context.setContextValue("Patient", patientId);

        context.registerDataProvider("http://hl7.org/fhir", this.providerFactory.createDataProvider("FHIR", fhirVersion));
        return context.resolveExpressionRef("Expression").evaluate(context);
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
                            result.addParameter().setName("value").setResource(bundler.bundle((Iterable<Resource>) res));
                        } else {
                            result.addParameter().setName("value").setValue(new StringType(res.toString()));
                        }
                    } else if (res instanceof Iterable) {
                        result.addParameter().setName("value").setResource(bundler.bundle((Iterable<Resource>) res));
                    } else if (res instanceof Resource) {
                        result.addParameter().setName("value").setResource((Resource) res);
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
