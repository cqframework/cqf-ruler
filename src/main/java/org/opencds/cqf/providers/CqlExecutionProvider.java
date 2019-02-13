package org.opencds.cqf.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;

import org.hl7.fhir.dstu3.model.*;

import org.opencds.cqf.config.STU3LibraryLoader;
import org.opencds.cqf.config.STU3LibrarySourceProvider;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.terminology.fhir.FhirTerminologyProvider;
import org.opencds.cqf.helpers.FhirMeasureBundler;
import org.opencds.cqf.helpers.LibraryHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Bryn on 1/16/2017.
 */
public class CqlExecutionProvider {
    private JpaDataProvider provider;

    public CqlExecutionProvider(JpaDataProvider provider) {
        this.provider = provider;
    }

    private ModelManager modelManager;
    private ModelManager getModelManager() {
        if (modelManager == null) {
            modelManager = new ModelManager();
        }
        return modelManager;
    }

    private LibraryManager libraryManager;
    private LibraryManager getLibraryManager() {
        if (libraryManager == null) {
            libraryManager = new LibraryManager(getModelManager());
            libraryManager.getLibrarySourceLoader().clearProviders();
            libraryManager.getLibrarySourceLoader().registerProvider(getLibrarySourceProvider());
        }
        return libraryManager;
    }

    private STU3LibraryLoader libraryLoader;
    private STU3LibraryLoader getLibraryLoader() {
        if (libraryLoader == null) {
            libraryLoader = new STU3LibraryLoader(getLibraryResourceProvider(), getLibraryManager(), getModelManager());
        }
        return libraryLoader;
    }

    private STU3LibrarySourceProvider librarySourceProvider;
    private STU3LibrarySourceProvider getLibrarySourceProvider() {
        if (librarySourceProvider == null) {
            librarySourceProvider = new STU3LibrarySourceProvider(getLibraryResourceProvider());
        }
        return librarySourceProvider;
    }

    private LibraryResourceProvider getLibraryResourceProvider() {
        return (LibraryResourceProvider)provider.resolveResourceProvider("Library");
    }

    private List<Reference> cleanReferences(List<Reference> references) {
        List<Reference> cleanRefs = new ArrayList<>();
        List<Reference> noDupes = new ArrayList<>();

        for (Reference reference : references) {
            boolean dup = false;
            for (Reference ref : noDupes) {
                if (ref.equalsDeep(reference))
                {
                    dup = true;
                }
            }
            if (!dup) {
                noDupes.add(reference);
            }
        }
        for (Reference reference : noDupes) {
            cleanRefs.add(
                    new Reference(
                            new IdType(
                                    reference.getReferenceElement().getResourceType(),
                                    reference.getReferenceElement().getIdPart().replace("#", ""),
                                    reference.getReferenceElement().getVersionIdPart()
                            )
                    )
            );
        }
        return cleanRefs;
    }

    private Iterable<Reference> getLibraryReferences(DomainResource instance) {
        List<Reference> references = new ArrayList<>();

        if (instance.hasContained()) {
            for (Resource resource : instance.getContained()) {
                if (resource instanceof Library) {
                    resource.setId(resource.getIdElement().getIdPart().replace("#", ""));
                    getLibraryResourceProvider().getDao().update((Library) resource);
//                    getLibraryLoader().putLibrary(resource.getIdElement().getIdPart(), getLibraryLoader().toElmLibrary((Library) resource));
                }
            }
        }

        if (instance instanceof ActivityDefinition) {
            references.addAll(((ActivityDefinition)instance).getLibrary());
        }

        else if (instance instanceof PlanDefinition) {
            references.addAll(((PlanDefinition)instance).getLibrary());
        }

        else if (instance instanceof Measure) {
            references.addAll(((Measure)instance).getLibrary());
        }

        for (Extension extension : instance.getExtensionsByUrl("http://hl7.org/fhir/StructureDefinition/cqif-library"))
        {
            Type value = extension.getValue();

            if (value instanceof Reference) {
                references.add((Reference)value);
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

            // TODO: This assumes the libraries resource id is the same as the library name, need to work this out better
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
    /* If the resource has a library extension, or a library element, that library is loaded into the context for the expression */
    public Object evaluateInContext(DomainResource instance, String cql, String patientId) {
        Iterable<Reference> libraries = getLibraryReferences(instance);

        // Provide the instance as the value of the '%context' parameter, as well as the value of a parameter named the same as the resource
        // This enables expressions to access the resource by root, as well as through the %context attribute
        String source = String.format("library LocalLibrary using FHIR version '3.0.0' include FHIRHelpers version '3.0.0' called FHIRHelpers %s parameter %s %s parameter \"%%context\" %s define Expression: %s",
                buildIncludes(libraries), instance.fhirType(), instance.fhirType(), instance.fhirType(), cql);
//        String source = String.format("library LocalLibrary using FHIR version '1.8' include FHIRHelpers version '1.8' called FHIRHelpers %s parameter %s %s parameter \"%%context\" %s define Expression: %s",
//                buildIncludes(libraries), instance.fhirType(), instance.fhirType(), instance.fhirType(), cql);

        org.cqframework.cql.elm.execution.Library library = LibraryHelper.translateLibrary(source, getLibraryManager(), getModelManager());
        Context context = new Context(library);
        context.setParameter(null, instance.fhirType(), instance);
        context.setParameter(null, "%context", instance);
        context.setExpressionCaching(true);
        context.registerLibraryLoader(getLibraryLoader());
        context.setContextValue("Patient", patientId);
        context.registerDataProvider("http://hl7.org/fhir", provider);
        return context.resolveExpressionRef("Expression").evaluate(context);
    }

    @Operation(name = "$cql")
    public Bundle evaluate(
            @OperationParam(name= "code") String code,
            @OperationParam(name= "patientId") String patientId,
            @OperationParam(name= "terminologyServiceUri") String terminologyServiceUri,
            @OperationParam(name= "terminologyUser") String terminologyUser,
            @OperationParam(name= "terminologyPass") String terminologyPass,
            @OperationParam(name= "parameters") Parameters parameters
    )
    {   
        CqlTranslator translator;
        FhirMeasureBundler bundler = new FhirMeasureBundler();

        try {
            translator = LibraryHelper.getTranslator(code, getLibraryManager(), getModelManager());
        }
        catch (IllegalArgumentException iae) {
            Parameters result = new Parameters();
            result.setId("translation-error");
            result.addParameter().setName("value").setValue(new StringType(iae.getMessage()));
            return bundler.bundle(Arrays.asList(result));
        }

        Map<String, List<Integer>> locations = getLocations(translator.getTranslatedLibrary().getLibrary());

        org.cqframework.cql.elm.execution.Library library = LibraryHelper.translateLibrary(translator);
        Context context = new Context(library);

        FhirContext fhirContext = provider.getFhirContext();
        fhirContext.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        provider.setFhirContext(fhirContext);

        if (terminologyServiceUri != null) {
            // TODO: Change to cache-value-sets
            FhirTerminologyProvider terminologyProvider = new FhirTerminologyProvider()
                    .withBasicAuth(terminologyUser, terminologyPass)
                    .setEndpoint(terminologyServiceUri, false);

            provider.setTerminologyProvider(terminologyProvider);
            // provider.setSearchUsingPOST(true);
            provider.setExpandValueSets(true);
            context.registerTerminologyProvider(provider.getTerminologyProvider());
        }

        context.registerDataProvider("http://hl7.org/fhir", provider);
        context.registerLibraryLoader(getLibraryLoader());

        if (parameters != null){
            for (org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent pc : parameters.getParameter())
            {
                context.setParameter(library.getLocalId(), pc.getName(), pc.getValue());
            }    
        }

        List<Resource> results = new ArrayList<>();
        for (org.cqframework.cql.elm.execution.ExpressionDef def : library.getStatements().getDef()) {
            context.enterContext(def.getContext());
            if (patientId != null && !patientId.isEmpty()) {
                context.setContextValue(context.getCurrentContext(), patientId);
            }
            else {
                context.setContextValue(context.getCurrentContext(), "null");
            }
            Parameters result = new Parameters();

            try {
                result.setId(def.getName());
                String location = String.format("[%d:%d]", locations.get(def.getName()).get(0), locations.get(def.getName()).get(1));
                result.addParameter().setName("location").setValue(new StringType(location));

                Object res = def instanceof org.cqframework.cql.elm.execution.FunctionDef ? "Definition successfully validated" : def.getExpression().evaluate(context);

                if (res == null) {
                    result.addParameter().setName("value").setValue(new StringType("null"));
                }
                else if (res instanceof List) {
                    if (((List) res).size() > 0 && ((List) res).get(0) instanceof Resource) {
                        result.addParameter().setName("value").setResource(bundler.bundle((Iterable)res));
                    }
                    else {
                        result.addParameter().setName("value").setValue(new StringType(res.toString()));
                    }
                }                
                else if (res instanceof Iterable) {
                    result.addParameter().setName("value").setResource(bundler.bundle((Iterable)res));
                }
                else if (res instanceof Resource) {
                    result.addParameter().setName("value").setResource((Resource)res);
                }
                else {
                    result.addParameter().setName("value").setValue(new StringType(res.toString()));
                }

                result.addParameter().setName("resultType").setValue(new StringType(resolveType(res)));
            }
            catch (RuntimeException re) {
                result.addParameter().setName("error").setValue(new StringType(re.getMessage()));
                re.printStackTrace();
            }
            results.add(result);
        }

        return bundler.bundle(results);
    }

    private  Map<String, List<Integer>>  getLocations(org.hl7.elm.r1.Library library) {
        Map<String, List<Integer>> locations = new HashMap<>();

        if (library.getStatements() == null) return locations;

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
            case "BigDecimal": return "Decimal";
            case "ArrayList": return "List";
            case "FhirBundleCursor": return "Retrieve";
        }
        return type;
    }
}
