package org.opencds.cqf.ruler.cpg.r4.provider;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.engine.fhir.retrieve.R4FhirQueryGenerator;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.builder.library.FhirRestLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory;
import org.opencds.cqf.cql.evaluator.library.CqlFhirParametersConverter;
import org.opencds.cqf.cql.evaluator.library.LibraryEvaluator;
import org.opencds.cqf.ruler.cpg.r4.R4LibraryUser;
import org.opencds.cqf.ruler.cql.JpaFhirDalFactory;
import org.opencds.cqf.ruler.cql.JpaLibraryContentProviderFactory;
import org.opencds.cqf.ruler.cql.JpaTerminologyProviderFactory;
import org.opencds.cqf.ruler.cql.LibraryLoaderFactory;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.opencds.cqf.ruler.utility.Canonicals;
import org.opencds.cqf.ruler.utility.Operations;
import org.springframework.beans.factory.annotation.Autowired;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;

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
		if (requestDetails.getRequestType() == RequestTypeEnum.GET) {
			try {
				Operations.validateCardinality(requestDetails, "subject", 0, 1);
				Operations.validateCardinality(requestDetails, "parameters", 0, 1);
				Operations.validateCardinality(requestDetails, "useServerData", 0, 1);
				Operations.validateCardinality(requestDetails, "data", 0, 1);
				Operations.validateCardinality(requestDetails, "dataEndpoint", 0, 1);
				Operations.validateCardinality(requestDetails, "contentEndpoint", 0, 1);
				Operations.validateCardinality(requestDetails, "terminologyEndpoint", 0, 1);
			} catch (Exception e) {
				return org.opencds.cqf.ruler.utility.r4.Parameters.newParameters(
					org.opencds.cqf.ruler.utility.r4.Parameters.newPart(
						"error", generateIssue("error", e.getMessage())
					)
				);
			}
		}

		if (prefetchData != null) {
			return org.opencds.cqf.ruler.utility.r4.Parameters.newParameters(
				org.opencds.cqf.ruler.utility.r4.Parameters.newPart(
					"error", generateIssue("warning", "prefetchData is not yet supported")
				)
			);
		}

		if (useServerData == null) {
			useServerData = new BooleanType(true);
		}

		Endpoint defaultEndpoint = new Endpoint().setAddress(requestDetails.getFhirServerBase()).setHeader(Collections.singletonList(new StringType("Content-Type: application/json")));

		dataEndpoint = dataEndpoint == null ? defaultEndpoint : dataEndpoint;
		contentEndpoint = contentEndpoint == null ? defaultEndpoint : contentEndpoint;
		terminologyEndpoint = terminologyEndpoint == null ? defaultEndpoint : terminologyEndpoint;

		Library libraryToEvaluate = read(theId);

		R4LibraryUser libraryUser = new R4LibraryUser(getDaoRegistry(), getFhirContext(), myModelResolver, libraryLoaderFactory, jpaLibraryContentProviderFactory,
			fhirRestLibraryContentProviderFactory, jpaTerminologyProviderFactory, new AdapterFactory(), subject, expression, parameters,
			useServerData, data, dataEndpoint, contentEndpoint, terminologyEndpoint, null);

		LibraryLoader libraryLoader = libraryUser.resolveLibraryLoader(requestDetails);
		TerminologyProvider terminologyProvider = libraryUser.resolveTerminologyProvider(requestDetails);
		DataProvider dataProvider = libraryUser.resolveDataProvider(terminologyProvider, new R4FhirQueryGenerator(new SearchParameterResolver(getFhirContext()), terminologyProvider, myModelResolver));
		CqlEvaluator cqlEvaluator = new CqlEvaluator(libraryLoader, Collections.singletonMap("http://hl7.org/fhir", dataProvider), terminologyProvider, Collections.singleton(CqlEngine.Options.EnableExpressionCaching));
		LibraryEvaluator libraryEvaluator = new LibraryEvaluator(new CqlFhirParametersConverter(this.getFhirContext(), libraryUser.getAdapterFactory(), new FhirTypeConverterFactory().create(FhirVersionEnum.R4)), cqlEvaluator);
		return (Parameters) libraryEvaluator.evaluate(resolveLibraryIdentifier(libraryToEvaluate), resolveContextParameter(subject), parameters, expression == null ? null : new HashSet<>(expression));
	}

	private VersionedIdentifier resolveLibraryIdentifier(Library library) {
		return new VersionedIdentifier()
			.withId(library.hasUrl() ? Canonicals.getIdPart(library.getUrl()) : library.hasName() ? library.getName() : null)
			.withVersion(library.hasVersion() ? library.getVersion() : library.hasUrl() ? Canonicals.getVersion(library.getUrl()) : null);
	}

	private OperationOutcome generateIssue(String severity, String details) {
		return new OperationOutcome().addIssue(
			new OperationOutcome.OperationOutcomeIssueComponent()
				.setSeverity(OperationOutcome.IssueSeverity.fromCode(severity))
				.setDetails(new CodeableConcept().setText(details))
		);
	}

	private Pair<String, Object> resolveContextParameter(String subject) {
		if (StringUtils.isBlank(subject)) return null;
		Reference subjectReference = new Reference(subject);
		return Pair.of(subjectReference.getType(), subjectReference.getReference());
	}
}
