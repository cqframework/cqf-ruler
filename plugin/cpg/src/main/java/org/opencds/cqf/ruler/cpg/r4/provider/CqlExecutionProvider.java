package org.opencds.cqf.ruler.cpg.r4.provider;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import ca.uhn.fhir.rest.api.RequestTypeEnum;
import org.apache.commons.lang3.StringUtils;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter;
import org.opencds.cqf.cql.evaluator.builder.EndpointInfo;
import org.opencds.cqf.cql.evaluator.builder.library.FhirRestLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory;
import org.opencds.cqf.ruler.cpg.CqlEvaluationHelper;
import org.opencds.cqf.ruler.cql.JpaFhirDalFactory;
import org.opencds.cqf.ruler.cql.JpaLibraryContentProviderFactory;
import org.opencds.cqf.ruler.cql.JpaTerminologyProviderFactory;
import org.opencds.cqf.ruler.cql.LibraryLoaderFactory;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;

import static org.opencds.cqf.ruler.utility.r4.Parameters.newParameters;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newPart;

/**
 * This class is used to provide an {@link DaoRegistryOperationProvider
 * OperationProvider}
 * implementation that supports cql expression evaluation
 * Created by Bryn on 1/16/2017.
 */
public class CqlExecutionProvider extends DaoRegistryOperationProvider {

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
	Map<VersionedIdentifier, Library> globalLibraryCache;

//	/**
//	 * Data to be made available to the library evaluation, organized as prefetch
//	 * response bundles. Each prefetchData parameter specifies either the name of
//	 * the prefetchKey it is satisfying, a DataRequirement describing the prefetch,
//	 * or both.
//	 */
//	class PrefetchData {
//		/**
//		 * The key of the prefetch item. This typically corresponds to the name of a
//		 * parameter in a library, or the name of a prefetch item in a CDS Hooks
//		 * discovery response
//		 */
//		String key;
//		/**
//		 * A {@link DataRequirement} DataRequirement describing the content of the
//		 * prefetch item.
//		 */
//		DataRequirement descriptor;
//		/**
//		 * The prefetch data as a {@link Bundle} Bundle. If the prefetchData has no
//		 * prefetchResult part, it indicates there is no data associated with this
//		 * prefetch item.
//		 */
//		Bundle data;
//
//		public PrefetchData withKey(String key) {
//			this.key = key;
//			return this;
//		}
//
//		public PrefetchData withDescriptor(DataRequirement descriptor) {
//			this.descriptor = descriptor;
//			return this;
//		}
//
//		public PrefetchData withData(Bundle data) {
//			this.data = data;
//			return this;
//		}
//	}

	/**
	 * Evaluates a CQL expression and returns the results as a Parameters resource.
	 * 
	 * @param requestDetails   the {@link RequestDetails RequestDetails}
	 * @param subject             Subject for which the expression will be
	 *                            evaluated. This corresponds to the context in
	 *                            which the expression will be evaluated and is
	 *                            represented as a relative FHIR id (e.g.
	 *                            Patient/123), which establishes both the context
	 *                            and context value for the evaluation
	 * @param expression          Expression to be evaluated. Note that this is an
	 *                            expression of CQL, not the text of a library with
	 *                            definition statements. If the content parameter is
	 *                            set, the expression will be the name of the
	 *                            expression to be evaluated.
	 * @param parameters          Any input parameters for the expression.
	 *                            {@link Parameters} Parameters defined in this
	 *                            input will be made available by name to the CQL
	 *                            expression. Parameter types are mapped to CQL as
	 *                            specified in the Using CQL section of the CPG
	 *                            Implementation guide. If a parameter appears more
	 *                            than once in the input Parameters resource, it is
	 *                            represented with a List in the input CQL. If a
	 *                            parameter has parts, it is represented as a Tuple
	 *                            in the input CQL.
	 * @param library             A library to be included. The {@link org.hl7.fhir.r4.model.Library}
	 *                            library is resolved by url and made available by
	 *                            name within the expression to be evaluated.
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
	 *                            provided).
	 * @param data                Data to be made available to the library
	 *                            evaluation. This parameter is exclusive with the
	 *                            prefetchData parameter (i.e. either provide all
	 *                            data as a single bundle, or provide data using
	 *                            multiple bundles with prefetch descriptions).
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
	 *                            performed.
	 * @param terminologyEndpoint An {@link Endpoint} endpoint to use to access
	 *                            terminology (i.e. valuesets, codesystems, and
	 *                            membership testing) referenced by the library. If
	 *                            no terminology endpoint is supplied, the
	 *                            evaluation will attempt to use the server on which
	 *                            the operation is being performed as the
	 *                            terminology server.
	 * @param content             The CQL library content. If this and the
	 *                            expression
	 *                            parameter are set, only the expression specified
	 *                            will be evaluated.
	 * @return The result of evaluating the given expression, returned as a FHIR
	 *         type, either a {@link org.hl7.fhir.r4.model.Resource} resource, or a FHIR-defined type
	 *         corresponding to the CQL return type, as defined in the Using CQL
	 *         section of the CPG Implementation guide. If the result is a List of
	 *         resources, the result will be a {@link Bundle} Bundle . If the result
	 *         is a CQL system-defined or FHIR-defined type, the result is returned
	 *         as a {@link Parameters} Parameters resource
	 */
	@Operation(name = "$cql")
	@Description(shortDefinition = "$cql", value = "Evaluates a CQL expression and returns the results as a Parameters resource. Defined: http://build.fhir.org/ig/HL7/cqf-recommendations/OperationDefinition-cpg-cql.html", example = "$cql?expression=5*5")
	public Parameters evaluate(
		RequestDetails requestDetails,
		@OperationParam(name = "subject", max = 1) String subject,
		@OperationParam(name = "expression", max = 1) String expression,
		@OperationParam(name = "parameters", max = 1) Parameters parameters,
		@OperationParam(name = "library") List<Parameters> library,
		@OperationParam(name = "useServerData", max = 1) BooleanType useServerData,
		@OperationParam(name = "data", max = 1) Bundle data,
		@OperationParam(name = "prefetchData") List<Parameters> prefetchData,
		@OperationParam(name = "dataEndpoint", max = 1) Endpoint dataEndpoint,
		@OperationParam(name = "contentEndpoint", max = 1) Endpoint contentEndpoint,
		@OperationParam(name = "terminologyEndpoint", max = 1) Endpoint terminologyEndpoint,
		@OperationParam(name = "content", max = 1) String content) {

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
				remoteData, remoteContent, remoteTerminology, content, libraryLoaderFactory,
				jpaLibraryContentProviderFactory.create(requestDetails), contentProvider,
				jpaTerminologyProviderFactory.create(requestDetails), getDaoRegistry());

		if (requestDetails.getRequestType() == RequestTypeEnum.GET) {
			IBaseOperationOutcome outcome = evaluationHelper.validateOperationParameters(
					requestDetails, "subject", "expression", "parameters", "library", "useServerData",
					"data", "prefetchData", "dataEndpoint", "contentEndpoint", "terminologyEndpoint", "content");

			if (outcome != null) {
				return newParameters(newPart("invalid parameters", (OperationOutcome) outcome));
			}
		}

		if (prefetchData != null) {
			return newParameters(newPart("invalid parameters",
					(OperationOutcome) evaluationHelper.createIssue(
							"warning", "prefetchData is not yet supported")));
		}

		if (expression == null && content == null) {
			return newParameters(newPart("invalid parameters",
					(OperationOutcome) evaluationHelper.createIssue("error",
							"The $cql operation requires the expression parameter and/or content parameter to exist")));
		}

		try {
			VersionedIdentifier libraryIdentifier = evaluationHelper.resolveLibraryIdentifier(content, null);
			if (libraryIdentifier != null) {
				globalLibraryCache.remove(libraryIdentifier);
			}

			if (StringUtils.isBlank(content)) {
				Endpoint defaultEndpoint = new Endpoint().setAddress(requestDetails.getFhirServerBase())
						.setHeader(Collections.singletonList(new StringType("Content-Type: application/json")));
				return (Parameters) evaluationHelper.getExpressionEvaluator().evaluate(expression,
						parameters == null ? new Parameters() : parameters, subject,
						evaluationHelper.resolveIncludedLibraries(library),
						useServerData == null || useServerData.booleanValue(), data, null,
						dataEndpoint == null ? defaultEndpoint : dataEndpoint,
						contentEndpoint == null ? defaultEndpoint : contentEndpoint,
						terminologyEndpoint == null ? defaultEndpoint : terminologyEndpoint);
			}

			return (Parameters) evaluationHelper.getLibraryEvaluator().evaluate(libraryIdentifier,
					evaluationHelper.resolveContextParameter(subject), parameters,
					expression == null ? null : Collections.singleton(expression));
		} catch (Exception e) {
			e.printStackTrace();
			return newParameters(newPart("evaluation error",
					(OperationOutcome) evaluationHelper.createIssue("error", e.getMessage())));
		}
	}
}
