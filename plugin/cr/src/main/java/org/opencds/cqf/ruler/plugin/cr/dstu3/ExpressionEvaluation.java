package org.opencds.cqf.ruler.plugin.cr.dstu3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;

import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.Type;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.debug.DebugMap;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.evaluator.cql2elm.content.InMemoryLibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.ruler.plugin.cql.CqlProperties;
import org.opencds.cqf.ruler.plugin.cql.JpaDataProviderFactory;
import org.opencds.cqf.ruler.plugin.cql.JpaFhirDal;
import org.opencds.cqf.ruler.plugin.cql.JpaFhirDalFactory;
import org.opencds.cqf.ruler.plugin.cql.JpaLibraryContentProviderFactory;
import org.opencds.cqf.ruler.plugin.cql.JpaTerminologyProviderFactory;
import org.opencds.cqf.ruler.plugin.cql.LibraryLoaderFactory;
import org.opencds.cqf.ruler.plugin.cr.utilities.LibraryUtilities;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.rest.api.server.RequestDetails;

public class ExpressionEvaluation implements LibraryUtilities {

    @Autowired
    private LibraryLoaderFactory libraryLoaderFactory;
    @Autowired
    private JpaLibraryContentProviderFactory jpaLibraryContentProviderFactory;
    @Autowired
    private JpaDataProviderFactory jpaDataProviderFactory;
	 @Autowired
	 private JpaTerminologyProviderFactory jpaTerminologyProviderFactory;
    @Autowired
    private JpaFhirDalFactory jpaFhirDalFactory;
    @Autowired
    private CqlProperties cqlProperties;
	 @Autowired
	 Map<VersionedIdentifier, org.cqframework.cql.elm.execution.Library> globalLibraryCache;
    /* Evaluates the given CQL expression in the context of the given resource */
    /*
     * If the resource has a library extension, or a library element, that library
     * is loaded into the context for the expression
     */
    public Object evaluateInContext(DomainResource instance, String cql, String patientId, RequestDetails theRequest) {
		JpaFhirDal jpaFhirDal = jpaFhirDalFactory.create(theRequest);
        Iterable<Reference> libraries = getLibraryReferences(instance, jpaFhirDal, theRequest);
        //String fhirVersion = this.context.getVersion().getVersion().getFhirVersionString();
        String fhirVersion = "3.0.0";

		// temporary LibraryLoader to resolve library dependencies when building includes
        LibraryLoader tempLibraryLoader = libraryLoaderFactory.create(
			  	new ArrayList<LibraryContentProvider>(
					  Arrays.asList(
							jpaLibraryContentProviderFactory.create(theRequest)
				))
			);
        // Provide the instance as the value of the '%context' parameter, as well as the
        // value of a parameter named the same as the resource
        // This enables expressions to access the resource by root, as well as through
        // the %context attribute
        String source = String.format(
                "library LocalLibrary using FHIR version '"+ fhirVersion + "' include FHIRHelpers version '"+ fhirVersion + "' called FHIRHelpers %s parameter %s %s parameter \"%%context\" %s define Expression: %s",
                buildIncludes(tempLibraryLoader, jpaFhirDal, libraries), instance.fhirType(), instance.fhirType(), instance.fhirType(), cql);
        // String source = String.format("library LocalLibrary using FHIR version '1.8'
        // include FHIRHelpers version '1.8' called FHIRHelpers %s parameter %s %s
        // parameter \"%%context\" %s define Expression: %s",
        // buildIncludes(libraries), instance.fhirType(), instance.fhirType(),
        // instance.fhirType(), cql);

        LibraryLoader libraryLoader = libraryLoaderFactory.create(
			  	new ArrayList<LibraryContentProvider>(
					  Arrays.asList(
							jpaLibraryContentProviderFactory.create(theRequest), 
							new InMemoryLibraryContentProvider(Arrays.asList(source)
				)))
			);

			// Remove LocalLibrary from cache first...
			VersionedIdentifier localLibraryIdentifier = new VersionedIdentifier().withId("LocalLibrary");
			globalLibraryCache.remove(localLibraryIdentifier);

        Context context = new Context(libraryLoader.load(localLibraryIdentifier));
        context.setDebugMap(getDebugMap());
        context.setParameter(null, instance.fhirType(), instance);
        context.setParameter(null, "%context", instance);
        context.setExpressionCaching(true);
        context.registerLibraryLoader(libraryLoader);
        context.setContextValue("Patient", patientId);

		  DataProvider dataProvider = jpaDataProviderFactory.create(theRequest, jpaTerminologyProviderFactory.create(theRequest));
        context.registerDataProvider("http://hl7.org/fhir", dataProvider);
        return context.resolveExpressionRef("Expression").evaluate(context);
    }

    private Iterable<Reference> getLibraryReferences(DomainResource instance, JpaFhirDal jpaFhirDal, RequestDetails theRequest) {
        List<Reference> references = new ArrayList<>();

        if (instance.hasContained()) {
            for (Resource resource : instance.getContained()) {
                if (resource instanceof Library) {
                    resource.setId(resource.getIdElement().getIdPart().replace("#", ""));
                    jpaFhirDal.update((Library) resource);
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

    private String buildIncludes(LibraryLoader libraryLoader, JpaFhirDal jpaFhirDal, Iterable<Reference> references) {
        StringBuilder builder = new StringBuilder();
        for (Reference reference : references) {
			  String cqlLibraryName = reference.getReferenceElement().getIdPart();
			  String cqlLibraryVersion = reference.getReferenceElement().getVersionIdPart();
			  VersionedIdentifier vi = new VersionedIdentifier();
			  vi.withId(cqlLibraryName);
			  if (!Strings.isNullOrEmpty(cqlLibraryVersion)) {
					vi.withVersion(cqlLibraryVersion);
			  }

			  // Still not the best way to build include, but at least checks dal for an existing library
			  // Check if id works for LibraryRetrieval
			  org.cqframework.cql.elm.execution.Library executionLibrary = null;
			  try {
					executionLibrary = libraryLoader.load(vi);  
			  } catch (Exception e) {
				  // log error
			  }
			  if (executionLibrary != null) {
				// log not found so looking in local data
					builder.append(buildLibraryIncludeString(reference, cqlLibraryName, cqlLibraryVersion));
			  }
			  // else check local data for Library to get name and version from
			  else {
				  Library library = (Library) jpaFhirDal.read(reference.getReferenceElement());
				  builder.append(buildLibraryIncludeString(reference, library.getName(), library.getVersion()));
			  }
        }

        return builder.toString();
    }

	private String buildLibraryIncludeString(Reference reference, String cqlLibraryName, String cqlLibraryVersion) {
		StringBuilder builder = new StringBuilder();
		
		builder.append("include ");
		
		// TODO: This assumes the libraries resource id is the same as the library name,
		// need to work this out better
		builder.append(cqlLibraryName);

		if (cqlLibraryVersion != null) {
			builder.append(" version '");
			builder.append(cqlLibraryVersion);
			builder.append("'");
		}

		builder.append(" called ");
		builder.append(cqlLibraryName);

		builder.append(" ");

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

    public DebugMap getDebugMap() {
        DebugMap debugMap = new DebugMap();
        if (cqlProperties.getCql_debug_enabled()) {
            debugMap.setIsLoggingEnabled(true);
        }
        return debugMap;
    }
}