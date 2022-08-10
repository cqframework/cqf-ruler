package org.opencds.cqf.ruler.cpg.dstu3.provider;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter;
import org.opencds.cqf.cql.evaluator.builder.EndpointInfo;
import org.opencds.cqf.cql.evaluator.builder.library.FhirRestLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.fhir.adapter.dstu3.AdapterFactory;
import org.opencds.cqf.ruler.cpg.CqlEvaluationHelper;
import org.opencds.cqf.ruler.cql.JpaFhirDalFactory;
import org.opencds.cqf.ruler.cql.JpaLibraryContentProviderFactory;
import org.opencds.cqf.ruler.cql.JpaTerminologyProviderFactory;
import org.opencds.cqf.ruler.cql.LibraryLoaderFactory;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newParameters;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newPart;

public class LibraryEvaluationProvider extends DaoRegistryOperationProvider {

	@Autowired
	private LibraryLoaderFactory libraryLoaderFactory;
	@Autowired
	private JpaLibraryContentProviderFactory jpaLibraryContentProviderFactory;
	@Autowired
	private FhirRestLibraryContentProviderFactory fhirRestLibraryContentProviderFactory;
	@Autowired
	private JpaTerminologyProviderFactory jpaTerminologyProviderFactory;
	@Autowired
	private JpaFhirDalFactory jpaFhirDalFactory;
	@Autowired
	ModelResolver myModelResolver;
	@Autowired
	Map<VersionedIdentifier, org.cqframework.cql.elm.execution.Library> globalLibraryCache;

	/**
	 * Evaluates a CQL library and returns the results as a Parameters resource.
	 *
	 * @param requestDetails   	the {@link RequestDetails RequestDetails}
	 * @param subject 				Subject for which the library will be evaluated.
	 *                            This corresponds to the context in which the library
	 *                            will be evaluated and is represented as a relative
	 *                            FHIR id (e.g. Patient/123), which establishes both
	 *                            the context and context value for the evaluation
	 * @param expression          Expression(s) to be evaluated. If no expression names
	 *                            are provided, the operation evaluates all public
	 *                               expression definitions in the library
	 * @param parameters          Any input parameters for the expression.
	 *                            {@link Parameters} Parameters defined in this
	 *                            input will be made available by name to the CQL
	 *                            expression. Parameter types are mapped to CQL as
	 *                            specified in the Using CQL section of the CPG
	 *                            Implementation guide. If a parameter appears more
	 *                            than once in the input Parameters resource, it is
	 *                            represented with a List in the input CQL. If a
	 *                            parameter has parts, it is represented as a Tuple
	 *                            in the input CQL
	 * @param useServerData       Whether to use data from the server performing the
	 *                            evaluation. If this parameter is true (the
	 *                            default), then the operation will use data first
	 *                            from any bundles provided as parameters (through
	 *                            the data and prefetch parameters), second data
	 *                            from the server performing the operation, and
	 *                            third, data from the dataEndpoint parameter (if
	 *                            provided). If this parameter is false, the
	 *                            operation will use data first from the bundles
	 *                            provided in the data or prefetch parameters, and
	 *                            second from the dataEndpoint parameter (if
	 *                            provided)
	 * @param data                Data to be made available to the library
	 *                            evaluation. This parameter is exclusive with the
	 *                            prefetchData parameter (i.e. either provide all
	 *                            data as a single bundle, or provide data using
	 *                            multiple bundles with prefetch descriptions)
	 * @param prefetchData        ***Not Yet Implemented***
	 * @param dataEndpoint        An {@link Endpoint} endpoint to use to access data
	 *                            referenced by retrieve operations in the library.
	 *                            If provided, this endpoint is used after the data
	 *                            or prefetchData bundles, and the server, if the
	 *                            useServerData parameter is true.
	 * @param contentEndpoint     An {@link Endpoint} endpoint to use to access
	 *                            content (i.e. libraries) referenced by the
	 *                            library. If no content endpoint is supplied, the
	 *                            evaluation will attempt to retrieve content from
	 *                            the server on which the operation is being
	 *                            performed
	 * @param terminologyEndpoint An {@link Endpoint} endpoint to use to access
	 *                            terminology (i.e. valuesets, codesystems, and
	 *                            membership testing) referenced by the library. If
	 *                            no terminology endpoint is supplied, the
	 *                            evaluation will attempt to use the server on which
	 *                            the operation is being performed as the
	 *                            terminology server
	 * @return The results of the library evaluation, returned as a {@link Parameters} resource
	 * 		  with a parameter for each named expression defined in the library. The value of
	 * 		  each expression is returned as a FHIR type, either a resource, or a FHIR-defined
	 * 		  type corresponding to the CQL return type, as defined in the Using CQL section of
	 * 		  this implementation guide. If the result of an expression is a list of resources,
	 * 		  that parameter will be repeated for each element in the result
	 */
	@Operation(name = "$evaluate", idempotent = true, type = Library.class)
	public Parameters evaluate(
		RequestDetails requestDetails,
		@IdParam IdType theId,
		@OperationParam(name = "subject") String subject,
		@OperationParam(name = "expression") List<String> expression,
		@OperationParam(name = "parameters") Parameters parameters,
		@OperationParam(name = "useServerData") BooleanType useServerData,
		@OperationParam(name = "data") Bundle data,
		@OperationParam(name = "prefetchData") List<Parameters> prefetchData,
		@OperationParam(name = "dataEndpoint") Endpoint dataEndpoint,
		@OperationParam(name = "contentEndpoint") Endpoint contentEndpoint,
		@OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint) {

		EndpointInfo remoteData = dataEndpoint != null
				? new EndpointConverter(new AdapterFactory()).getEndpointInfo(dataEndpoint) : null;
		EndpointInfo remoteContent = contentEndpoint != null
				? new EndpointConverter(new AdapterFactory()).getEndpointInfo(contentEndpoint) : null;
		EndpointInfo remoteTerminology = terminologyEndpoint != null
				? new EndpointConverter(new AdapterFactory()).getEndpointInfo(terminologyEndpoint) : null;
		LibraryContentProvider contentProvider = remoteContent != null
				? fhirRestLibraryContentProviderFactory.create(remoteContent.getAddress(), remoteContent.getHeaders())
				: null;

		CqlEvaluationHelper evaluationHelper = new CqlEvaluationHelper(getFhirContext(), myModelResolver,
				new AdapterFactory(), useServerData == null || useServerData.booleanValue(), data,
				remoteData, remoteContent, remoteTerminology, null, libraryLoaderFactory,
				jpaLibraryContentProviderFactory.create(requestDetails), contentProvider,
				jpaTerminologyProviderFactory.create(requestDetails), getDaoRegistry());

		if (requestDetails.getRequestType() == RequestTypeEnum.GET) {
			IBaseOperationOutcome outcome = evaluationHelper.validateOperationParameters(requestDetails,
					"subject", "expression", "parameters", "useServerData", "data",
					"prefetchData", "dataEndpoint", "contentEndpoint", "terminologyEndpoint");

			if (outcome != null) return newParameters(newPart("error", (OperationOutcome) outcome));
		}

		if (prefetchData != null) return newParameters(newPart("invalid parameters",
				(OperationOutcome) evaluationHelper.createIssue("error",
						"prefetchData is not yet supported")));

		VersionedIdentifier libraryIdentifier = evaluationHelper.resolveLibraryIdentifier(null, read(theId));
		globalLibraryCache.remove(libraryIdentifier);

		try {
			return (Parameters) evaluationHelper.getLibraryEvaluator().evaluate(libraryIdentifier,
					evaluationHelper.resolveContextParameter(subject), parameters,
					expression == null ? null : new HashSet<>(expression));
		} catch (Exception e) {
			e.printStackTrace();
			return newParameters(newPart("evaluation error",
					(OperationOutcome) evaluationHelper.createIssue("error", e.getMessage())));
		}
	}
}
