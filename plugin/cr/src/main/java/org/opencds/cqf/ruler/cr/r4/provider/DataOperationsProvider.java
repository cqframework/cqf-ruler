package org.opencds.cqf.ruler.cr.r4.provider;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.opencds.cqf.cql.engine.fhir.exception.FhirVersionMisMatchException;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.BundleFhirLibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.ruler.cql.JpaLibraryContentProvider;
import org.opencds.cqf.ruler.cql.JpaLibraryContentProviderFactory;
import org.opencds.cqf.ruler.cql.LibraryManagerFactory;
import org.opencds.cqf.ruler.cr.utility.DataRequirements;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.opencds.cqf.ruler.utility.CanonicalParts;
import org.opencds.cqf.ruler.utility.Canonicals;
import org.opencds.cqf.ruler.utility.Libraries;
import org.opencds.cqf.ruler.utility.Searches;
import org.opencds.cqf.ruler.utility.Translators;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;

public class DataOperationsProvider extends DaoRegistryOperationProvider {

	@Autowired
	private JpaLibraryContentProviderFactory jpaLibraryContentProviderFactory;

	@Autowired
	private LibraryManagerFactory libraryManagerFactory;

	@Autowired
	private LibraryVersionSelector libraryVersionSelector;

	@Autowired
	private AdapterFactory adapterFactory;

	@Operation(name = "$data-requirements", idempotent = true, type = Library.class)
	public Library dataRequirements(@IdParam IdType theId,
											  @OperationParam(name = "target") String target,
											  RequestDetails theRequestDetails) throws InternalErrorException, FHIRException, FhirVersionMisMatchException {

		Library library = read(theId, theRequestDetails);
		return processDataRequirements(library, theRequestDetails);

	}

	@Operation(name = "$data-requirements", idempotent = true, type = Measure.class)
	public Library dataRequirements(@IdParam IdType theId,
											  @OperationParam(name = "startPeriod") String startPeriod,
											  @OperationParam(name = "endPeriod") String endPeriod,
											  RequestDetails theRequestDetails) throws InternalErrorException, FHIRException, FhirVersionMisMatchException {

		Measure measure = read(theId, theRequestDetails);
		Library library = getLibraryFromMeasure(measure, theRequestDetails);

		if (library == null) {
			throw new RuntimeException("Could not load measure library.");
		}
		return processDataRequirements(library, theRequestDetails);
	}

	public Library getLibraryFromMeasure(Measure measure, RequestDetails theRequestDetails) {
		Iterator<CanonicalType> var6 = measure.getLibrary().iterator();

		String libraryIdOrCanonical = null;
		//use the first library
		while (var6.hasNext() && libraryIdOrCanonical == null) {
			CanonicalType ref = var6.next();

			if (ref != null) {
				libraryIdOrCanonical = ref.getValue();
			}
		}

		Library library = read(new IdType(libraryIdOrCanonical), theRequestDetails);

		if(library == null){
			library = search(Library.class, Searches.byCanonical(libraryIdOrCanonical), theRequestDetails).firstOrNull();
		}
		return library;
	}

	private Library processDataRequirements(Library library, RequestDetails theRequestDetails) throws FhirVersionMisMatchException {
		JpaLibraryContentProvider jpaLibraryContentProvider = jpaLibraryContentProviderFactory.create(theRequestDetails);

		Bundle libraryBundle = new Bundle();
		List<Library> listLib = fetchDependencyLibraries(library, theRequestDetails);
		listLib.add(library);

		listLib.forEach(lib -> {
			Bundle.BundleEntryComponent component = new Bundle.BundleEntryComponent();
			component.setResource(lib);
			libraryBundle.addEntry(component);
		});

		LibraryContentProvider bundleLibraryProvider = new BundleFhirLibraryContentProvider(this.getFhirContext(),
			libraryBundle, adapterFactory, libraryVersionSelector);

		List<LibraryContentProvider> sourceProviders = new ArrayList<>(
			Arrays.asList(bundleLibraryProvider, jpaLibraryContentProvider));

		LibraryManager libraryManager = libraryManagerFactory.create(sourceProviders);

		CqlTranslator translator = Translators.getTranslator(
			new ByteArrayInputStream(Libraries.getContent(library, "text/cql")), libraryManager,
			libraryManager.getModelManager());
		if (!translator.getErrors().isEmpty()) {
			throw new CqlTranslatorException(Translators.errorsToString(translator.getErrors()));
		}
		return DataRequirements.getModuleDefinitionLibraryR4(libraryManager, translator.getTranslatedLibrary(), Translators.getTranslatorOptions());
	}

	private List<Library> fetchDependencyLibraries(Library library, RequestDetails theRequestDetails) {
		Map<String, Library> resources = new HashMap<>();
		List<Library> queue = new ArrayList<>();
		queue.add(library);

		while (!queue.isEmpty()) {
			Library current = queue.get(0);
			queue.remove(0);
			visitLibrary(current, queue, resources, theRequestDetails);
		}
		return new ArrayList<>(resources.values());
	}

	private void visitLibrary(Library library, List<Library> queue, Map<String, Library> resources,
			RequestDetails theRequestDetails) {
		for (RelatedArtifact relatedArtifact : library.getRelatedArtifact()) {
			if (relatedArtifact.getType().equals(RelatedArtifact.RelatedArtifactType.DEPENDSON)
					&& relatedArtifact.hasResource()) {

				// FHIR R4+, resource is defined as a canonical
				String resourceString = relatedArtifact.getResource();
				CanonicalParts parts = Canonicals.getParts(resourceString);
				if (parts.resourceType().equals("Library")) {
					Library lib = search(Library.class, Searches.byCanonical(resourceString), theRequestDetails).firstOrNull();
					if (lib != null) {
						resources.putIfAbsent(lib.getId(), lib);
						queue.add(lib);
					}
				}
			}
		}
	}
}
