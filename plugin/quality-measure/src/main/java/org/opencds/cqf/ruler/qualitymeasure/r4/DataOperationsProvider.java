package org.opencds.cqf.ruler.qualitymeasure.r4;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.ruler.cpg.r4.util.R4BundleLibraryContentProvider;
import org.opencds.cqf.ruler.cql.JpaLibraryContentProvider;
import org.opencds.cqf.ruler.cql.JpaLibraryContentProviderFactory;
import org.opencds.cqf.ruler.cql.LibraryManagerFactory;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.opencds.cqf.ruler.utility.Canonicals;
import org.opencds.cqf.ruler.utility.Libraries;
import org.opencds.cqf.ruler.utility.Translators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DataOperationsProvider extends DaoRegistryOperationProvider {

	private static final Logger logger = LoggerFactory.getLogger(DataOperationsProvider.class);

	@Autowired
	private JpaLibraryContentProviderFactory jpaLibraryContentProviderFactory;


	@Autowired
	private LibraryManagerFactory libraryManagerFactory;


	@Autowired
	private DataRequirementsUtility dataRequirementsUtility;

	@Operation(name = "$data-requirements", idempotent = true, type = Library.class)
	public Library dataRequirements(@IdParam IdType theId, @OperationParam(name = "target") String target,
											  RequestDetails theRequestDetails) throws InternalErrorException, FHIRException {

		JpaLibraryContentProvider jpaLibraryContentProvider = jpaLibraryContentProviderFactory.create(theRequestDetails);

		Library library = read(theId, theRequestDetails);
		if (library == null) {
			throw new RuntimeException("Could not load library.");
		}

		Bundle libraryBundle = new Bundle();
		List<Library> listLib = fetchDependencyLibraries(library, theRequestDetails);
		listLib.add(library);

		listLib.forEach(lib -> {
			Bundle.BundleEntryComponent component =  new Bundle.BundleEntryComponent();
			component.setResource(lib);
			libraryBundle.addEntry(component);
		});

		LibraryContentProvider bundleLibraryProvider = new R4BundleLibraryContentProvider(libraryBundle);

		java.util.List<LibraryContentProvider> sourceProviders = new ArrayList<LibraryContentProvider>(
			Arrays.asList(bundleLibraryProvider, jpaLibraryContentProvider));

		LibraryManager libraryManager = libraryManagerFactory.create(sourceProviders);

		CqlTranslator translator = Translators.getTranslator(Libraries.extractContentStream(library), libraryManager, libraryManager.getModelManager());
		if (translator.getErrors().size() > 0) {
			throw new RuntimeException("Errors during library compilation.");
		}

		Library resultLibrary =
			this.dataRequirementsUtility.getModuleDefinitionLibrary(libraryManager,
				translator.getTranslatedLibrary(), Translators.getTranslatorOptions());

		return resultLibrary;
	}


	private List<Library> fetchDependencyLibraries( Library library,  RequestDetails theRequestDetails) {
		Map<String, Library> resources = new HashMap<>();
		List<Library> queue = new ArrayList<>();
		queue.add(library);

		while(!queue.isEmpty()) {
			Library current = queue.get(0);
			queue.remove(0);
			visitLibrary(current, queue, resources, theRequestDetails);
		}
		return new ArrayList<Library>(resources.values());
	}

	private void visitLibrary(Library library, List<Library> queue, Map<String, Library> resources,  RequestDetails theRequestDetails) {
		for (RelatedArtifact relatedArtifact : library.getRelatedArtifact()) {
			if (relatedArtifact.getType().equals(RelatedArtifact.RelatedArtifactType.DEPENDSON)) {
				if (relatedArtifact.hasResource()) {
					String resourceString = relatedArtifact.getResource();
					if (resourceString.startsWith("Library/") || resourceString.contains("/Library/")) {
						try {
							String id = Canonicals.getIdPart(resourceString);
							IdType newId = new IdType(Canonicals.getResourceType(resourceString)+"/"+id.substring(0,id.indexOf("|")));
							Library lib = read(newId, theRequestDetails);
							if (!resources.containsKey(lib.getId())) {
								resources.put(lib.getId(), lib);
								queue.add(lib);
							}
						} catch (Exception e) {
							logger.error("Exception:" + e.getMessage());
						}
					}
				}
			}
		}
	}


}
