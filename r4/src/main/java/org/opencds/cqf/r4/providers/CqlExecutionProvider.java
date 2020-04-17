package org.opencds.cqf.r4.providers;

import com.alphora.cql.service.factory.DataProviderFactory;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.common.providers.LibraryResolutionProvider;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.r4.processors.CqlExecutionProcessor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;

/**
 * Created by Bryn on 1/16/2017.
 */
public class CqlExecutionProvider {
    private CqlExecutionProcessor cqlExecutionProcessor;

    public CqlExecutionProvider(LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> libraryResourceProvider, DataProviderFactory dataProviderFactory, FhirContext fhirContext, TerminologyProvider localSystemTerminologyProvider) {
        cqlExecutionProcessor = new CqlExecutionProcessor(libraryResourceProvider, dataProviderFactory, fhirContext, localSystemTerminologyProvider);
    }
    

    @Operation(name = "$cql")
    public Bundle evaluate(@OperationParam(name = "code") String code,
            @OperationParam(name = "patientId") String patientId,
            @OperationParam(name="periodStart") String periodStart,
            @OperationParam(name="periodEnd") String periodEnd,
            @OperationParam(name="productLine") String productLine,
			@OperationParam(name = "context") String contextParam,
			@OperationParam(name = "executionResults") String executionResults,
            @OperationParam(name = "endpoint") Endpoint endpoint,
            @OperationParam(name = "parameters") Parameters parameters) {
            return cqlExecutionProcessor.evaluate(code, patientId, periodStart, periodEnd, productLine, contextParam, executionResults, endpoint, parameters);
    }

    //kept in to avoid conflicts
    //TODO: replace this with cqlExecutionProcessor
    public Object evaluateInContext(Resource instance, String cql, String patientId) {
        return cqlExecutionProcessor.evaluateInContext(instance, cql, patientId, false);
    }

    //kept in to avoid conflicts
    //TODO: replace this with cqlExecutionProcessor
    public Object evaluateInContext(Resource instance, String cqlName, String patientId, Boolean aliasedExpression) {
        return cqlExecutionProcessor.evaluateInContext(instance, cqlName, patientId, aliasedExpression);
    }


	public CqlExecutionProcessor getProcessor() {
		return cqlExecutionProcessor;
	}
}
