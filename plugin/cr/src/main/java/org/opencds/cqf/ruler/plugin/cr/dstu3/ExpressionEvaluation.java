package org.opencds.cqf.ruler.plugin.cr.dstu3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.Type;
import org.opencds.cqf.cql.engine.debug.DebugMap;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.ruler.plugin.cr.CrProperties;
import org.opencds.cqf.ruler.plugin.cr.utilities.LibraryUtilities;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.cql.common.provider.EvaluationProviderFactory;
import ca.uhn.fhir.cql.common.provider.LibraryContentProvider;
import ca.uhn.fhir.cql.common.provider.LibraryResolutionProvider;
import ca.uhn.fhir.cql.dstu3.helper.LibraryHelper;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class ExpressionEvaluation implements LibraryUtilities {
    
    @Autowired
    private EvaluationProviderFactory providerFactory;
    @Autowired
    private LibraryResolutionProvider<Library> libraryResolutionProvider;
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

        LibraryLoader libraryLoader = this.libraryHelper.createLibraryLoader(this.libraryResolutionProvider);

        org.cqframework.cql.elm.execution.Library library = this.translateLibrary(source, getLibraryManager(this.libraryResolutionProvider), modelManager);
        Context context = new Context(library);
        context.setDebugMap(getDebugMap());
        context.setParameter(null, instance.fhirType(), instance);
        context.setParameter(null, "%context", instance);
        context.setExpressionCaching(true);
        context.registerLibraryLoader(libraryLoader);
        context.setContextValue("Patient", patientId);

        context.registerDataProvider("http://hl7.org/fhir", this.providerFactory.createDataProvider("FHIR", fhirVersion, requestDetails));
        return context.resolveExpressionRef("Expression").evaluate(context);
    }

    private Iterable<Reference> getLibraryReferences(DomainResource instance) {
        List<Reference> references = new ArrayList<>();

        if (instance.hasContained()) {
            for (Resource resource : instance.getContained()) {
                if (resource instanceof Library) {
                    resource.setId(resource.getIdElement().getIdPart().replace("#", ""));
                    this.libraryResolutionProvider.update((Library) resource);
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

    private LibraryManager getLibraryManager(LibraryResolutionProvider<org.hl7.fhir.dstu3.model.Library> provider) {
		List<org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider> contentProviders = Collections.singletonList(new LibraryContentProvider<org.hl7.fhir.dstu3.model.Library, Attachment>(
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
