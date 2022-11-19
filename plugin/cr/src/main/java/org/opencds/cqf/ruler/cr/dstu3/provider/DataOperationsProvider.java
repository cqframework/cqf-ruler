package org.opencds.cqf.ruler.cr.dstu3.provider;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.BundleFhirLibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.cql.evaluator.fhir.util.Ids;
import org.opencds.cqf.cql.evaluator.fhir.util.Libraries;
import org.opencds.cqf.ruler.cql.JpaLibrarySourceProvider;
import org.opencds.cqf.ruler.cql.JpaLibrarySourceProviderFactory;
import org.opencds.cqf.ruler.cql.LibraryManagerFactory;
import org.opencds.cqf.ruler.cql.utility.Translators;
import org.opencds.cqf.ruler.cr.utility.DataRequirements;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.opencds.cqf.ruler.utility.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

public class DataOperationsProvider extends DaoRegistryOperationProvider {
	private Logger myLog = LoggerFactory.getLogger(DataOperationsProvider.class);

	@Autowired
	private JpaLibrarySourceProviderFactory jpaLibrarySourceProviderFactory;

	@Autowired
	private LibraryManagerFactory libraryManagerFactory;

	@Autowired
	private LibraryVersionSelector libraryVersionSelector;

	@Autowired
	private AdapterFactory adapterFactory;

	@Autowired
	private CqlTranslatorOptions cqlTranslatorOptions;

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
			throw new ResourceNotFoundException(measure.getLibrary().get(0).getReference());
		}
		// TODO: Pass startPeriod and endPeriod as parameters to the data requirements
		// operation
		return processDataRequirements(measure, library, theRequestDetails);
	}

	public Library getLibraryFromMeasure(Measure measure, RequestDetails theRequestDetails) {
		Iterator<Reference> libraryIter = measure.getLibrary().iterator();

		String libraryIdOrCanonical = null;
		// use the first library
		while (libraryIter.hasNext() && libraryIdOrCanonical == null) {
			Reference ref = libraryIter.next();

			if (ref != null) {
				libraryIdOrCanonical = ref.getReference();
			}
		}

		Library library = null;

		try {
			library = read(new IdType(libraryIdOrCanonical), theRequestDetails);
		} catch (Exception e) {
			myLog.info("Library read failed as measure.getLibrary() is not an ID, fall back to search as canonical");
		}

		if (library == null) {
			library = search(Library.class, Searches.byCanonical(libraryIdOrCanonical), theRequestDetails)
					.firstOrNull();
		}
		return library;
	}

	private LibraryManager createLibraryManager(Library library, RequestDetails theRequestDetails) {
		JpaLibrarySourceProvider jpaLibrarySourceProvider = jpaLibrarySourceProviderFactory.create(theRequestDetails);

		Bundle libraryBundle = new Bundle();
		List<Library> listLib = fetchDependencyLibraries(library, theRequestDetails);
		listLib.add(library);

		listLib.forEach(lib -> {
			Bundle.BundleEntryComponent component = new Bundle.BundleEntryComponent();
			component.setResource(lib);
			libraryBundle.addEntry(component);
		});

		LibrarySourceProvider bundleLibraryProvider = new BundleFhirLibrarySourceProvider(this.getFhirContext(),
				libraryBundle, adapterFactory, libraryVersionSelector);

		List<LibrarySourceProvider> sourceProviders = Lists.newArrayList(bundleLibraryProvider,
				jpaLibrarySourceProvider);

		return libraryManagerFactory.create(sourceProviders);
	}

	private CqlTranslator translateLibrary(Library library, LibraryManager libraryManager) {
		CqlTranslator translator = Translators.getTranslator(
				new ByteArrayInputStream(Libraries.getContent(library, "text/cql")), libraryManager,
				libraryManager.getModelManager(), cqlTranslatorOptions);

		if (!translator.getErrors().isEmpty()) {
			throw new CqlCompilerException(Translators.errorsToString(translator.getErrors()));
		}
		return translator;
	}

	private Library processDataRequirements(Measure measure, Library library, RequestDetails theRequestDetails) {
		LibraryManager libraryManager = createLibraryManager(library, theRequestDetails);
		CqlTranslator translator = translateLibrary(library, libraryManager);

		return DataRequirements.getModuleDefinitionLibraryDstu3(measure, libraryManager,
				translator.getTranslatedLibrary(), cqlTranslatorOptions);
	}

	private Library processDataRequirements(Library library, RequestDetails theRequestDetails) {
		LibraryManager libraryManager = createLibraryManager(library, theRequestDetails);
		CqlTranslator translator = translateLibrary(library, libraryManager);

		return DataRequirements.getModuleDefinitionLibraryDstu3(libraryManager,
				translator.getTranslatedLibrary(), cqlTranslatorOptions);
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
				String reference = relatedArtifact.getResource().getReference();
				IdType id = Ids.newId(Library.class, reference.substring(reference.indexOf("/") + 1));
				if (id != null) {
					Library lib = search(Library.class, Searches.byId(id), theRequestDetails).firstOrNull();
					if (lib != null) {
						resources.putIfAbsent(lib.getId(), lib);
						queue.add(lib);
					}
				}
			}
		}
	}
}
