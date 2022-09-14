package org.opencds.cqf.ruler.ra.r4;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.ruler.behavior.r4.MeasureReportUser;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.opencds.cqf.ruler.utility.Operations;

import static org.opencds.cqf.ruler.utility.r4.Parameters.newParameters;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newPart;

public class ResolveProvider extends DaoRegistryOperationProvider implements MeasureReportUser {

    @Operation(name = "$davinci-ra.resolve", idempotent = true, type = Bundle.class)
    public Parameters resolve(
            RequestDetails requestDetails, @IdParam IdType theId,
            @OperationParam(name = "periodStart") String periodStart,
            @OperationParam(name = "periodEnd") String periodEnd,
            @OperationParam(name = "subject") String subject,
            @OperationParam(name = "measureId") String measureId,
            @OperationParam(name = "measureIdentifier") String measureIdentifier,
            @OperationParam(name = "measureUrl") String measureUrl) {

        if (requestDetails.getRequestType() == RequestTypeEnum.GET) {
            try {
                Operations.validateCardinality(requestDetails, "generatedStart", 1);
                Operations.validateCardinality(requestDetails, "generatedEnd", 1);
                Operations.validateCardinality(requestDetails, "subject", 1);
                Operations.validateDate("periodStart", periodStart);
                Operations.validateDate("periodEnd", periodEnd);
            } catch (Exception e) {
                return newParameters(newPart("Invalid parameters",
                        generateIssue("error", e.getMessage())));
            }
        }
        return null;
    }
}
