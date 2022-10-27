package org.opencds.cqf.ruler.cr.dstu3.service;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.hl7.fhir.dstu3.model.CarePlan;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.evaluator.activitydefinition.dstu3.ActivityDefinitionProcessor;
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
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.fhir.adapter.dstu3.AdapterFactory;
import org.opencds.cqf.cql.evaluator.library.CqlFhirParametersConverter;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.opencds.cqf.cql.evaluator.plandefinition.OperationParametersParser;
import org.opencds.cqf.cql.evaluator.plandefinition.dstu3.PlanDefinitionProcessor;
import org.opencds.cqf.ruler.behavior.DaoRegistryUser;
import org.opencds.cqf.ruler.cql.JpaFhirDalFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class PlanDefinitionService implements DaoRegistryUser {

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

	@Autowired
	private DaoRegistry daoRegistry;

	private RequestDetails requestDetails;

	public void setRequestDetails(RequestDetails requestDetails) {
		this.requestDetails = requestDetails;
	}

	private PlanDefinitionProcessor createProcessor() {
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
				endpointConverter, fhirModelResolverFactory, CqlEvaluatorBuilder::new);

		var evaluator = new ExpressionEvaluator(this.fhirContext, cqlFhirParametersConverter,
				librarySourceProviderFactory, this.dataProviderFactory, terminologyProviderFactory, endpointConverter,
				fhirModelResolverFactory, CqlEvaluatorBuilder::new);

		var activityDefinitionProcessor = new ActivityDefinitionProcessor(this.fhirContext, fhirDal, libraryProcessor);
		var operationParametersParser = new OperationParametersParser(adapterFactory, fhirTypeConverter);

		return new PlanDefinitionProcessor(this.fhirContext, fhirDal, libraryProcessor, evaluator,
				activityDefinitionProcessor, operationParametersParser);
	}

	public CarePlan applyPlanDefinition(IdType theId,
			String patientId,
			String encounterId,
			String practitionerId,
			String organizationId,
			String userType,
			String userLanguage,
			String userTaskContext,
			String setting,
			String settingContext)
			throws IOException, FHIRException {
		var processor = createProcessor();

		return processor.apply(theId, patientId, encounterId, practitionerId, organizationId, userType, userLanguage,
				userTaskContext, setting, settingContext, null, null, null, null, null, null, null, null);
	}

	@Override
	public DaoRegistry getDaoRegistry() {
		return this.daoRegistry;
	}
}
