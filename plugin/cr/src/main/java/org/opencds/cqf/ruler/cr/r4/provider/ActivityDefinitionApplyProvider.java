package org.opencds.cqf.ruler.cr.r4.provider;

import java.util.Collections;
import java.util.Set;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.evaluator.activitydefinition.r4.ActivityDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.library.FhirRestLibrarySourceProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.LibrarySourceProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.TypedLibrarySourceProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.FhirRestTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.TypedTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory;
import org.opencds.cqf.cql.evaluator.library.CqlFhirParametersConverter;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.opencds.cqf.ruler.behavior.ResourceCreator;
import org.opencds.cqf.ruler.cql.JpaFhirDalFactory;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;

/**
 * Created by Bryn on 1/16/2017.
 */
public class ActivityDefinitionApplyProvider extends DaoRegistryOperationProvider implements ResourceCreator {

	@Autowired
	private DataProviderFactory dataProviderFactory;

	@Autowired
	private FhirRestLibrarySourceProviderFactory fhirRestLibrarySourceProviderFactory;

	@Autowired
	private FhirRestTerminologyProviderFactory fhirRestTerminologyProviderFactory;

	@Autowired
	private JpaFhirDalFactory fhirDalFactory;

	@Autowired
	private FhirContext fhirContext;

	private ActivityDefinitionProcessor createProcessor(RequestDetails requestDetails) {
		var adapterFactory = new AdapterFactory();
		var endpointConverter = new EndpointConverter(adapterFactory);
		var libraryVersionSelector = new LibraryVersionSelector(adapterFactory);
		var fhirTypeConverter = new FhirTypeConverterFactory().create(fhirContext.getVersion().getVersion());
		var cqlFhirParametersConverter = new CqlFhirParametersConverter(fhirContext, adapterFactory, fhirTypeConverter);
		var fhirDal = this.fhirDalFactory.create(requestDetails);
		var fhirModelResolverFactory = new FhirModelResolverFactory();
		Set<TypedLibrarySourceProviderFactory> librarySourceProviderFactories = Collections
				.singleton(this.fhirRestLibrarySourceProviderFactory);
		Set<TypedTerminologyProviderFactory> terminologyProviderFactories = Collections
				.singleton(this.fhirRestTerminologyProviderFactory);

		var librarySourceProviderFactory = new LibrarySourceProviderFactory(
				fhirContext, adapterFactory, librarySourceProviderFactories, libraryVersionSelector);
		var terminologyProviderFactory = new TerminologyProviderFactory(fhirContext, terminologyProviderFactories);

		var libraryProcessor = new LibraryProcessor(this.fhirContext, cqlFhirParametersConverter,
				librarySourceProviderFactory, this.dataProviderFactory, terminologyProviderFactory,
				endpointConverter, fhirModelResolverFactory, () -> new CqlEvaluatorBuilder());

		return new ActivityDefinitionProcessor(this.fhirContext, fhirDal, libraryProcessor);
	}

	@Operation(name = "$apply", idempotent = true, type = ActivityDefinition.class)
	public Resource apply(RequestDetails theRequest, @IdParam IdType theId,
			@OperationParam(name = "patient") String patientId,
			@OperationParam(name = "encounter") String encounterId,
			@OperationParam(name = "practitioner") String practitionerId,
			@OperationParam(name = "organization") String organizationId,
			@OperationParam(name = "userType") String userType,
			@OperationParam(name = "userLanguage") String userLanguage,
			@OperationParam(name = "userTaskContext") String userTaskContext,
			@OperationParam(name = "setting") String setting,
			@OperationParam(name = "settingContext") String settingContext)
			throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		var processor = createProcessor(theRequest);

		return (Resource) processor.apply(theId, patientId, encounterId, practitionerId, organizationId, userType,
				userLanguage, userTaskContext, setting, settingContext, null, null, null, null);
	}
}
