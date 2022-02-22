package org.opencds.cqf.ruler.cr.dstu3.provider;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.dstu3.model.Measure;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.BundleFhirLibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.ruler.cql.JpaLibraryContentProvider;
import org.opencds.cqf.ruler.cql.JpaLibraryContentProviderFactory;
import org.opencds.cqf.ruler.cql.LibraryManagerFactory;
import org.opencds.cqf.ruler.cr.utility.DataRequirements;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.opencds.cqf.ruler.utility.Ids;
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
	public Library dataRequirements(@IdParam IdType theId, @OperationParam(name = "target") String target,
			RequestDetails theRequestDetails) throws InternalErrorException, FHIRException {

		Library library = read(theId, theRequestDetails);
		return processDataRequirements(library, theRequestDetails);
	}

	@Operation(name = "$data-requirements", idempotent = true, type = Measure.class)
	public Library dataRequirements(@IdParam IdType theId,
												@OperationParam(name = "startPeriod") String startPeriod,
												@OperationParam(name = "endPeriod") String endPeriod,
												RequestDetails theRequestDetails) throws InternalErrorException, FHIRException {

		Measure measure = read(theId, theRequestDetails);
		Library library = getLibraryFromMeasure(measure, theRequestDetails);

		if (library == null) {
			throw new RuntimeException("Could not load measure library.");
		}
		return processDataRequirements(library, theRequestDetails);
	}

	public Library getLibraryFromMeasure(Measure measure, RequestDetails theRequestDetails) {
		Iterator<Reference> var6 = measure.getLibrary().iterator();

		String libraryIdOrCanonical = null;
		//use the first library
		while (var6.hasNext() && libraryIdOrCanonical == null) {
			Reference ref = var6.next();

			if (ref != null) {
				libraryIdOrCanonical = ref.getReference();
			}
		}

		Library library = read(new IdType(libraryIdOrCanonical), theRequestDetails);

		if(library == null){
			library = search(Library.class, Searches.byCanonical(libraryIdOrCanonical), theRequestDetails).firstOrNull();
		}
		return library;
	}


	private Library processDataRequirements(Library library, RequestDetails theRequestDetails) {
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

		List<LibraryContentProvider> sourceProviders = Lists.newArrayList(bundleLibraryProvider,
			jpaLibraryContentProvider);

		LibraryManager libraryManager = libraryManagerFactory.create(sourceProviders);

		CqlTranslator translator = Translators.getTranslator(
			new ByteArrayInputStream(Libraries.getContent(library, "text/cql")), libraryManager,
			libraryManager.getModelManager());

		if (!translator.getErrors().isEmpty()) {
			throw new CqlTranslatorException(Translators.errorsToString(translator.getErrors()));
		}
		return DataRequirements.getModuleDefinitionLibraryDstu3(libraryManager,
			translator.getTranslatedLibrary(), Translators.getTranslatorOptions());
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
				IdType id = Ids.newId(Library.class, relatedArtifact.getResource().getReference());
				Library lib = search(Library.class, Searches.byId(id), theRequestDetails).firstOrNull();
				if (lib != null) {
					resources.putIfAbsent(lib.getId(), lib);
					queue.add(lib);
				}
			}
		}
	}
}
