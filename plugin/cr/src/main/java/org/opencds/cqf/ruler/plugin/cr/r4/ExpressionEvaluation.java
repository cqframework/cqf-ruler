package org.opencds.cqf.ruler.plugin.cr.r4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.cql.engine.debug.DebugMap;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.ruler.plugin.cr.CrProperties;
import org.opencds.cqf.ruler.plugin.cr.r4.utilities.CanonicalUtilities;
import org.opencds.cqf.ruler.plugin.cr.utilities.LibraryUtilities;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.cql.common.provider.EvaluationProviderFactory;
import ca.uhn.fhir.cql.common.provider.LibraryContentProvider;
import ca.uhn.fhir.cql.common.provider.LibraryResolutionProvider;
import ca.uhn.fhir.cql.r4.helper.LibraryHelper;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class ExpressionEvaluation implements LibraryUtilities, CanonicalUtilities {
    
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
    private CrProperties crProperties;

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

        LibraryLoader libraryLoader = this.libraryHelper.createLibraryLoader(this.libraryResourceProvider);

        org.cqframework.cql.elm.execution.Library library = this.translateLibrary(source, getLibraryManager(this.libraryResourceProvider), modelManager);

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
                LibraryLoader libraryLoader = this.libraryHelper.createLibraryLoader(this.libraryResourceProvider);
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

    

    private Iterable<CanonicalType> getLibraryReferences(DomainResource instance) {
        List<CanonicalType> references = new ArrayList<>();

        if (instance.hasContained()) {
            for (Resource resource : instance.getContained()) {
                if (resource instanceof Library) {
                    resource.setId(resource.getIdElement().getIdPart().replace("#", ""));
                    this.libraryResourceProvider.update((Library) resource);
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

    private LibraryManager getLibraryManager(LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> provider) {
		List<org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider> contentProviders = Collections.singletonList(new LibraryContentProvider<org.hl7.fhir.r4.model.Library, Attachment>(
			provider, x -> x.getContent(), x -> x.getContentType(), x -> x.getData()));
            
        LibraryManager libraryManager = new LibraryManager(modelManager);
        for (org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider contentProvider : contentProviders) {
            libraryManager.getLibrarySourceLoader().registerProvider(contentProvider);
        }

        return libraryManager;
    }

    public DebugMap getDebugMap() {
        DebugMap debugMap = new DebugMap();
        if (crProperties.getCql_debug_enabled()) {
            debugMap.setIsLoggingEnabled(true);
        }
        return debugMap;
    }
}
