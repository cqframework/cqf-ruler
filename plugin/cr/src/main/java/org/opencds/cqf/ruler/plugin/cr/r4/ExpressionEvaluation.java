package org.opencds.cqf.ruler.plugin.cr.r4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.debug.DebugMap;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.evaluator.cql2elm.content.InMemoryLibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.ruler.plugin.cql.CqlProperties;
import org.opencds.cqf.ruler.plugin.cql.JpaDataProviderFactory;
import org.opencds.cqf.ruler.plugin.cql.JpaFhirDalFactory;
import org.opencds.cqf.ruler.plugin.cql.JpaLibraryContentProviderFactory;
import org.opencds.cqf.ruler.plugin.cql.JpaTerminologyProviderFactory;
import org.opencds.cqf.ruler.plugin.cql.LibraryLoaderFactory;
import org.opencds.cqf.ruler.plugin.cr.r4.utilities.CanonicalUtilities;
import org.opencds.cqf.ruler.plugin.cr.utilities.LibraryUtilities;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class ExpressionEvaluation implements LibraryUtilities, CanonicalUtilities {

    @Autowired
    private LibraryLoaderFactory libraryLoaderFactory;
    @Autowired
    private JpaLibraryContentProviderFactory jpaLibraryContentProviderFactory;
    @Autowired
    private FhirContext fhirContext;
    @Autowired
    private JpaDataProviderFactory jpaDataProviderFactory;
	 @Autowired
	 private JpaTerminologyProviderFactory jpaTerminologyProviderFactory;
    @Autowired
    private JpaFhirDalFactory jpaFhirDalFactory;
    @Autowired
    private CqlProperties cqlProperties;

    /* Evaluates the given CQL expression in the context of the given resource */
    /*
     * If the resource has a library extension, or a library element, that library
     * is loaded into the context for the expression
     */
    public Object evaluateInContext(DomainResource instance, String cql, String patientId, RequestDetails theRequest) {
        Context context = setupContext(instance, cql, patientId, theRequest);
        return context.resolveExpressionRef("Expression").evaluate(context);
    }

    public Object evaluateInContext(DomainResource instance, String cql, String patientId, Boolean aliasedExpression, RequestDetails theRequest) {
        Context context = setupContext(instance, cql, patientId, theRequest);
        return context.resolveExpressionRef(cql).evaluate(context);
    }

    private Context setupContext(DomainResource instance, String cql, String patientId, RequestDetails theRequest) {
        Iterable<CanonicalType> libraries = getLibraryReferences(instance, theRequest);

        String fhirVersion = this.fhirContext.getVersion().getVersion().getFhirVersionString();

        String source = String.format(
                "library LocalLibrary using FHIR version '" + fhirVersion + "' include FHIRHelpers version '"+ fhirVersion +"' called FHIRHelpers %s parameter %s %s parameter \"%%context\" %s define Expression: %s",
                buildIncludes(libraries, theRequest), instance.fhirType(), instance.fhirType(), instance.fhirType(), cql);


			LibraryLoader libraryLoader = libraryLoaderFactory.create(
				new ArrayList<LibraryContentProvider>(
					Arrays.asList(
							jpaLibraryContentProviderFactory.create(theRequest), 
							new InMemoryLibraryContentProvider(Arrays.asList(source)
				)))
			);
        // resolve execution context
        return setupContext(instance, patientId, libraryLoader, theRequest);
    }  

    private Iterable<CanonicalType> getLibraryReferences(DomainResource instance, RequestDetails theRequest) {
        List<CanonicalType> references = new ArrayList<>();

        if (instance.hasContained()) {
            for (Resource resource : instance.getContained()) {
                if (resource instanceof Library) {
                    resource.setId(resource.getIdElement().getIdPart().replace("#", ""));
                    this.jpaFhirDalFactory.create(theRequest).update((Library) resource);
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

    private String buildIncludes(Iterable<CanonicalType> references, RequestDetails theRequest) {
        StringBuilder builder = new StringBuilder();
        for (CanonicalType reference : references) {

            if (builder.length() > 0) {
                builder.append(" ");
            }

            builder.append("include ");

            // TODO: This assumes the libraries resource id is the same as the library name,
            // need to work this out better
            Library lib = (Library) this.jpaFhirDalFactory.create(theRequest).read(this.getIdType(reference));
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

    private Context setupContext(DomainResource instance, String patientId,
        LibraryLoader libraryLoader, RequestDetails theRequest) {
        // Provide the instance as the value of the '%context' parameter, as well as the
        // value of a parameter named the same as the resource
        // This enables expressions to access the resource by root, as well as through
        // the %context attribute
        Context context = new Context(libraryLoader.load(new VersionedIdentifier().withId("LocalLibrary")));
        context.setDebugMap(getDebugMap());
        context.setParameter(null, instance.fhirType(), instance);
        context.setParameter(null, "%context", instance);
        context.setExpressionCaching(true);
        context.registerLibraryLoader(libraryLoader);
        context.setContextValue("Patient", patientId);

		  DataProvider dataProvider = jpaDataProviderFactory.create(theRequest, jpaTerminologyProviderFactory.create(theRequest));
        context.registerDataProvider("http://hl7.org/fhir", dataProvider);
        return context;
    }

    public DebugMap getDebugMap() {
        DebugMap debugMap = new DebugMap();
        if (cqlProperties.getCql_debug_enabled()) {
            debugMap.setIsLoggingEnabled(true);
        }
        return debugMap;
    }
}
