package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.rp.dstu3.MeasureReportResourceProvider;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.dstu3.model.*;

public class FHIRMeasureReportProvider extends MeasureReportResourceProvider {

    private JpaDataProvider provider;

    public FHIRMeasureReportProvider(JpaDataProvider provider) {
        this.provider = provider;
    }

    @Operation(name = "$submit-data", idempotent = true)
    public Resource submitData(
            RequestDetails details,
            @IdParam IdType theId,
            @OperationParam(name="measure-report", min = 1, max = 1, type = MeasureReport.class) MeasureReport report,
            @OperationParam(name="resource", type = Bundle.class) Bundle resources)
    {
        Measure measure = (Measure) provider.resolveResourceProvider("Measure").getDao().read(new IdType(theId.getIdPart()));

        if (measure == null) {
            throw new IllegalArgumentException(theId.getValue() + " does not exist");
        }

        // TODO - validate resources are in Measure's data requirements
        try {
            provider.setEndpoint(details.getFhirServerBase());
            return provider.getFhirClient().transaction().withBundle(createTransactionBundle(report, resources)).execute();
        } catch (Exception e) {
            return new OperationOutcome().addIssue(
                    new OperationOutcome.OperationOutcomeIssueComponent()
                            .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                            .setCode(OperationOutcome.IssueType.EXCEPTION)
                            .setDetails(new CodeableConcept().setText(e.getMessage()))
                            .setDiagnostics(ExceptionUtils.getStackTrace(e))
            );
        }
    }

    private Bundle createTransactionBundle(MeasureReport report, Bundle bundle) {
        Bundle transactionBundle;
        if (bundle != null) {
            if (bundle.hasType() && bundle.getType() == Bundle.BundleType.TRANSACTION) {
                transactionBundle = bundle;
            }
            else {
                transactionBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
                if (bundle.hasEntry()) {
                    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                        if (entry.hasResource()) {
                            transactionBundle.addEntry(createTransactionEntry(entry.getResource()));
                        }
                    }
                }
            }
        }
        else {
            transactionBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
        }

        return transactionBundle.addEntry(createTransactionEntry(report));
    }

    private Bundle.BundleEntryComponent createTransactionEntry(Resource resource) {
        Bundle.BundleEntryComponent transactionEntry = new Bundle.BundleEntryComponent().setResource(resource);
        if (resource.hasId()) {
            transactionEntry.setRequest(
                    new Bundle.BundleEntryRequestComponent()
                            .setMethod(Bundle.HTTPVerb.PUT)
                            .setUrl(resource.getId())
            );
        }
        else {
            transactionEntry.setRequest(
                    new Bundle.BundleEntryRequestComponent()
                            .setMethod(Bundle.HTTPVerb.POST)
                            .setUrl(resource.fhirType())
            );
        }
        return transactionEntry;
    }
}
