package org.opencds.cqf.ruler.cdshooks.r4;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.ruler.cpg.r4.provider.CqlExecutionProvider;
import org.opencds.cqf.ruler.cpg.r4.provider.LibraryEvaluationProvider;

import java.util.List;

public class CqlExecutionHandler {
	private final R4CqlExecution cqlExecution;
	private final LibraryEvaluationProvider libraryExecutionProvider;
	private final CqlExecutionProvider cqlExecutionProvider;
	private final IdType primaryLibraryId;
	private final Parameters draftOrders;
	private final BooleanType useServerData;
	private final Bundle prefetchData;

	public CqlExecutionHandler(R4CqlExecution cqlExecution, LibraryEvaluationProvider libraryExecutionProvider,
										CqlExecutionProvider cqlExecutionProvider, IdType primaryLibraryId,
										Parameters draftOrders, BooleanType useServerData, Bundle prefetchData) {
		this.cqlExecution = cqlExecution;
		this.libraryExecutionProvider = libraryExecutionProvider;
		this.cqlExecutionProvider = cqlExecutionProvider;
		this.primaryLibraryId = primaryLibraryId;
		this.draftOrders = draftOrders;
		this.useServerData = useServerData;
		this.prefetchData = prefetchData;
	}

	public Parameters evaluateLibrary(String patientId, List<String> expressions, Endpoint remoteDataEndpoint) {
		return cqlExecution.getLibraryExecution(libraryExecutionProvider, primaryLibraryId, patientId,
			expressions, draftOrders, useServerData, prefetchData, remoteDataEndpoint);
	}

	public Parameters evaluateExpression(String patientId, String expression) {
		return cqlExecution.getExpressionExecution(cqlExecutionProvider, patientId, expression);
	}

	public Type evaluateExpressionGetResult(String patientId, String expression, String name) {
		return evaluateExpression(patientId, expression).getParameterValue(name == null ? "return" : name);
	}
}
