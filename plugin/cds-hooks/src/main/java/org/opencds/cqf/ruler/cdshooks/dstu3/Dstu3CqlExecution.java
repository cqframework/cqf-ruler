package org.opencds.cqf.ruler.cdshooks.dstu3;

import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Type;
import org.opencds.cqf.cql.engine.exception.CqlException;
import org.opencds.cqf.ruler.cpg.dstu3.provider.CqlExecutionProvider;
import org.opencds.cqf.ruler.cpg.dstu3.provider.LibraryEvaluationProvider;

import java.util.List;
import java.util.Optional;

public class Dstu3CqlExecution {
    private final RequestDetails requestDetails = new SystemRequestDetails();

    public Dstu3CqlExecution(String baseUrl) {
        this.requestDetails.setFhirServerBase(baseUrl);
    }

    private void checkError(Parameters result) {
        if (result.getParameter().stream().anyMatch(x -> x.getName().equals("evaluation error"))
                && result.getParameter().get(0).hasResource()
                && result.getParameter().get(0).getResource() instanceof OperationOutcome) {
            throw new CqlException(
                    ((OperationOutcome) result.getParameter().get(0).getResource())
                            .getIssueFirstRep().getDetails().getText());
        }
    }

    public Parameters getLibraryExecution(LibraryEvaluationProvider libraryExecution, IdType logicId,
                                          String patientId, List<String> expressions, Parameters parameters,
                                          BooleanType useServerData, Bundle data, Endpoint remoteDataEndpoint) {
        Parameters executionResult = libraryExecution.evaluate(requestDetails, logicId, patientId,
                expressions, parameters, useServerData, data, null, remoteDataEndpoint,
                null, null);
        checkError(executionResult);
        return executionResult;
    }

    public Parameters getExpressionExecution(CqlExecutionProvider cqlExecution, String patientId,
                                             String expression) {
        try {
            Parameters executionResult = cqlExecution.evaluate(requestDetails,
                    patientId, expression, null, null, null,
                    null, null, null, null,
                    null, null);
            checkError(executionResult);
            return executionResult;
        } catch (CqlException cqle) {
            // since there is no defined way to determine between a cql expression reference and cql expression
            // we will not report errors for expression execution and assume null result is accurate
            return null;
        }
    }

    public Type getEvaluationResult(Parameters evaluationResults, String expressionName) {
        Optional<Parameters.ParametersParameterComponent> result =
                evaluationResults.getParameter().stream()
                        .filter(p -> p.getName().equals(expressionName)).findFirst();
        return result.map(Parameters.ParametersParameterComponent::getValue).orElse(null);
    }
}
