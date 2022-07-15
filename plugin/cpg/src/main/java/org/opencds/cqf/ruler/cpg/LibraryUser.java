package org.opencds.cqf.ruler.cpg;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBooleanDatatype;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.engine.fhir.retrieve.BaseFhirQueryGenerator;
import org.opencds.cqf.cql.engine.fhir.retrieve.RestFhirRetrieveProvider;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter;
import org.opencds.cqf.cql.evaluator.builder.LibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirRestRetrieveProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.FhirRestLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.FhirRestTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.content.InMemoryLibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.retrieve.PriorityRetrieveProvider;
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.fhir.ClientFactory;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.cql.evaluator.library.CqlFhirParametersConverter;
import org.opencds.cqf.ruler.cql.JpaFhirRetrieveProvider;
import org.opencds.cqf.ruler.cql.JpaLibraryContentProviderFactory;
import org.opencds.cqf.ruler.cql.JpaTerminologyProviderFactory;
import org.opencds.cqf.ruler.cql.LibraryLoaderFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class LibraryUser {

	private DaoRegistry daoRegistry;
	private FhirContext fhirContext;
	private ModelResolver myModelResolver;
	private LibraryLoaderFactory libraryLoaderFactory;
	private JpaLibraryContentProviderFactory jpaLibraryContentProviderFactory;
	private FhirRestLibraryContentProviderFactory fhirRestLibraryContentProviderFactory;
	private JpaTerminologyProviderFactory jpaTerminologyProviderFactory;
	private AdapterFactory adapterFactory;
	private ClientFactory clientFactory;

	private String subject;
	private List<String> expression;
	private IBaseParameters parameters;
	private IBaseBooleanDatatype useServerData;
	private IBaseBundle data;
	private IDomainResource dataEndpoint;
	private IDomainResource libraryContentEndpoint;
	private IDomainResource terminologyEndpoint;
	private String content;

	public LibraryUser(DaoRegistry daoRegistry, FhirContext fhirContext, ModelResolver myModelResolver, LibraryLoaderFactory libraryLoaderFactory,
							 JpaLibraryContentProviderFactory jpaLibraryContentProviderFactory, FhirRestLibraryContentProviderFactory fhirRestLibraryContentProviderFactory,
							 JpaTerminologyProviderFactory jpaTerminologyProviderFactory, AdapterFactory adapterFactory, String subject,
							 List<String> expression, IBaseParameters parameters, IBaseBooleanDatatype useServerData, IBaseBundle data,
							 IDomainResource dataEndpoint, IDomainResource libraryContentEndpoint, IDomainResource terminologyEndpoint, String content) {
		this.daoRegistry = daoRegistry;
		this.fhirContext = fhirContext;
		this.myModelResolver = myModelResolver;
		this.libraryLoaderFactory = libraryLoaderFactory;
		this.jpaLibraryContentProviderFactory = jpaLibraryContentProviderFactory;
		this.fhirRestLibraryContentProviderFactory = fhirRestLibraryContentProviderFactory;
		this.jpaTerminologyProviderFactory = jpaTerminologyProviderFactory;
		this.adapterFactory = adapterFactory;
		this.clientFactory = new ClientFactory(fhirContext);
		this.subject = subject;
		this.expression = expression;
		this.parameters = parameters;
		this.useServerData = useServerData;
		this.data = data;
		this.dataEndpoint = dataEndpoint;
		this.libraryContentEndpoint = libraryContentEndpoint;
		this.terminologyEndpoint = terminologyEndpoint;
		this.content = content;
	}

	// Getters
	protected FhirContext getFhirContext() {
		return fhirContext;
	}
	protected JpaTerminologyProviderFactory getJpaTerminologyProviderFactory() {
		return jpaTerminologyProviderFactory;
	}
	protected FhirRestLibraryContentProviderFactory getFhirRestLibraryContentProviderFactory() {
		return fhirRestLibraryContentProviderFactory;
	}
	public IDomainResource getLibraryContentEndpoint() {
		return libraryContentEndpoint;
	}
	public IDomainResource getTerminologyEndpoint() {
		return terminologyEndpoint;
	}
	public IDomainResource getDataEndpoint() {
		return dataEndpoint;
	}
	public AdapterFactory getAdapterFactory() {
		return adapterFactory;
	}
	public IBaseParameters getParameters() {
		return parameters;
	}
	public List<String> getExpression() {
		return expression;
	}
	public String getSubject() {
		return subject;
	}
	public IBaseBooleanDatatype getUseServerData() {
		return useServerData;
	}
	public IBaseBundle getData() {
		return data;
	}
	public String getContent() {
		return content;
	}

	public LibraryLoader resolveLibraryLoader(RequestDetails requestDetails) {
		List<LibraryContentProvider> libraryProviders = new ArrayList<>();
		libraryProviders.add(jpaLibraryContentProviderFactory.create(requestDetails));

		resolveLibraryContentEndpoint(libraryProviders);

		if (!StringUtils.isBlank(content)) {
			libraryProviders.add(
				new InMemoryLibraryContentProvider(Collections.singletonList(content))
			);
		}

		return libraryLoaderFactory.create(new ArrayList<>(libraryProviders));
	}

	public DataProvider resolveDataProvider(TerminologyProvider terminologyProvider, BaseFhirQueryGenerator queryGenerator) {
		List<RetrieveProvider> retrieveProviderList = new ArrayList<>();
		if (useServerData.getValue()) {
			JpaFhirRetrieveProvider jpaRetriever = new JpaFhirRetrieveProvider(daoRegistry, new SearchParameterResolver(fhirContext));
			jpaRetriever.setTerminologyProvider(terminologyProvider);
			jpaRetriever.setModelResolver(myModelResolver);
			jpaRetriever.setFhirQueryGenerator(queryGenerator);
			if (terminologyEndpoint != null) {
				jpaRetriever.setExpandValueSets(true);
			}
			retrieveProviderList.add(jpaRetriever);
		}

		if (dataEndpoint != null) {
			IGenericClient client = getDataEndpointClient();
			RestFhirRetrieveProvider restRetriever = new RestFhirRetrieveProvider(new SearchParameterResolver(fhirContext), client);
			restRetriever.setTerminologyProvider(terminologyProvider);
			restRetriever.setModelResolver(myModelResolver);
			restRetriever.setFhirQueryGenerator(queryGenerator);
			if (terminologyEndpoint == null || !isTerminologyEndpointSameAsData()) {
				restRetriever.setExpandValueSets(true);
			}

			retrieveProviderList.add(restRetriever);
		}

		if (data != null) {
			BundleRetrieveProvider bundleRetriever = new BundleRetrieveProvider(fhirContext, data);
			bundleRetriever.setTerminologyProvider(terminologyProvider);
			retrieveProviderList.add(bundleRetriever);
		}

		PriorityRetrieveProvider priorityProvider = new PriorityRetrieveProvider(retrieveProviderList);
		return new CompositeDataProvider(myModelResolver, priorityProvider);
	}

	public ExpressionEvaluator resolveExpressionEvaluator() {
		return new ExpressionEvaluator(
			fhirContext,
			new CqlFhirParametersConverter(
				fhirContext, adapterFactory, new FhirTypeConverterFactory().create(getFhirContext().getVersion().getVersion())
			),
			resolveLibraryContentProviderFactory(), resolveDataProviderFactory(), resolveTerminologyProviderFactory(),
			new EndpointConverter(adapterFactory), new FhirModelResolverFactory(), CqlEvaluatorBuilder::new);
	}

	private LibraryContentProviderFactory resolveLibraryContentProviderFactory() {
		return new org.opencds.cqf.cql.evaluator.builder.library.LibraryContentProviderFactory(
			fhirContext, adapterFactory,
			Collections.singleton(resolveFhirRestLibraryContentProviderFactory()),
			new LibraryVersionSelector(adapterFactory)
		);
	}

	private FhirRestLibraryContentProviderFactory resolveFhirRestLibraryContentProviderFactory() {
		return new FhirRestLibraryContentProviderFactory(
			new ClientFactory(fhirContext), adapterFactory, new LibraryVersionSelector(adapterFactory)
		);
	}

	private DataProviderFactory resolveDataProviderFactory() {
		return new org.opencds.cqf.cql.evaluator.builder.data.DataProviderFactory(
			fhirContext, Collections.singleton(new FhirModelResolverFactory()),
			Collections.singleton(new FhirRestRetrieveProviderFactory(fhirContext, clientFactory))
		);
	}

	private TerminologyProviderFactory resolveTerminologyProviderFactory() {
		return new org.opencds.cqf.cql.evaluator.builder.terminology.TerminologyProviderFactory(
			fhirContext, Collections.singleton(new FhirRestTerminologyProviderFactory(fhirContext, clientFactory))
		);
	}

	public abstract void resolveLibraryContentEndpoint(List<LibraryContentProvider> libraryProviders);
	public abstract TerminologyProvider resolveTerminologyProvider(RequestDetails requestDetails);
	public abstract IGenericClient getDataEndpointClient();
	public abstract boolean isTerminologyEndpointSameAsData();
}
