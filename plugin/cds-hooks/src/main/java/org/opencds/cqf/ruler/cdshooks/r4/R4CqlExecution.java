package org.opencds.cqf.ruler.cdshooks.r4;

import ca.uhn.fhir.cr.r4.cpg.CqlExecutionOperationProvider;
import ca.uhn.fhir.cr.r4.cpg.LibraryEvaluationOperationProvider;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.cql.engine.exception.CqlException;

import java.util.List;

public class R4CqlExecution {
    private final RequestDetails requestDetails = new SystemRequestDetails();

    public R4CqlExecution(String baseUrl) {
        this.requestDetails.setFhirServerBase(baseUrl);
    }

    private void checkError(Parameters result) {
        if (result.hasParameter("evaluation error")
                && result.getParameter().get(0).hasResource()
                && result.getParameter().get(0).getResource() instanceof OperationOutcome) {
            throw new CqlException(
                    ((OperationOutcome) result.getParameter().get(0).getResource())
                            .getIssueFirstRep().getDetails().getText());
        }
    }

    public Parameters getLibraryExecution(LibraryEvaluationOperationProvider libraryExecution, IdType logicId,
														String patientId, List<String> expressions, Parameters parameters,
														Bundle data, Endpoint remoteDataEndpoint) {
        Parameters executionResult = libraryExecution.evaluate(requestDetails, logicId, patientId,
                expressions, parameters, data, null, remoteDataEndpoint,
                null, null);
        checkError(executionResult);
        return executionResult;
    }

    public Parameters getExpressionExecution(CqlExecutionOperationProvider cqlExecution, String patientId,
															String expression) {
        Parameters executionResult = cqlExecution.evaluate(requestDetails,
                patientId, expression, null, null, null,
                null, null, null, null,
                null, null);
        checkError(executionResult);
        return executionResult;
    }
}
