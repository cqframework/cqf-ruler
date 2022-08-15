package org.opencds.cqf.ruler.cdshooks.dstu3;

import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Type;
import org.opencds.cqf.ruler.cpg.dstu3.provider.CqlExecutionProvider;
import org.opencds.cqf.ruler.cpg.dstu3.provider.LibraryEvaluationProvider;

import java.util.List;
import java.util.Optional;

public class Dstu3CqlExecution {
    private OperationOutcome error;
    private final RequestDetails requestDetails = new SystemRequestDetails();

    public Dstu3CqlExecution(String baseUrl) {
        this.requestDetails.setFhirServerBase(baseUrl);
    }

    public OperationOutcome getError() {
        return error;
    }

    public void setError(String errorMessage) {
        error = new OperationOutcome();
        error.addIssue().setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setDetails(new CodeableConcept().setText(errorMessage));
    }

    public boolean isError() {
        return error != null;
    }

    private void checkError(Parameters result) {
        if (result.getParameter().stream().anyMatch(x -> x.getName().equals("evaluation error"))
                && result.getParameter().get(0).hasResource()
                && result.getParameter().get(0).getResource() instanceof OperationOutcome) {
            error = (OperationOutcome) result.getParameter().get(0).getResource();
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
        Parameters executionResult = cqlExecution.evaluate(requestDetails,
                patientId, expression, null, null, null,
                null, null, null, null,
                null, null);
        checkError(executionResult);
        // since there is no defined way to determine between a cql expression reference and cql expression
        // we will not report errors for expression execution and assume null result is accurate
        if (isError()) {
            error = null;
            return null;
        }
        return executionResult;
    }

    public Type getEvaluationResult(Parameters evaluationResults, String expressionName) {
        Optional<Parameters.ParametersParameterComponent> result =
                evaluationResults.getParameter().stream()
                        .filter(p -> p.getName().equals(expressionName)).findFirst();
        return result.map(Parameters.ParametersParameterComponent::getValue).orElse(null);
    }
}
