package org.opencds.cqf.servlet;

import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;
import ca.uhn.fhir.jpa.rp.dstu3.CodeSystemResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.ValueSetResourceProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.dstu3.model.*;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.providers.JpaDataProvider;
import org.opencds.cqf.providers.JpaTerminologyProvider;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by Bryn on 6/4/2018.
 */
@WebServlet(name = "on-measure-subscription-mrp")
public class MeasureSubscriptionServlet extends BaseServlet {
    // This endpoint is registered to receive notifications for a measure subscription
    // https://fhir-pit.mihin.org/cqf-ruler/baseDstu3/Subscription
    // NOTE: This ruler is not enabled for subscription, so we will fire the subscription notification manually

    // Issue the $evaluate-measure for the subscription endpoint

    // $evaluate-measure?patient=Patient-1153&periodStart=2017-01&periodNed=2017-12&lastUpdated=@lastUpdated
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO: This endpoint assumes that we've registered the subscription here
        IGenericClient client = getFhirContext().newRestfulGenericClient("https://fhir-pit.mihin.org/cqf-ruler/baseDstu3");

        // TODO: Needs to handle the patient, we're assuming a specific patient-id here
        // TODO: Needs to handle the lastUpdated parameter, so that will need to be saved locally per subscription
        Parameters result = client.operation().onInstance(new IdType("Measure", "measure-mrp"))
                .named("evaluate-measure")
                .withParameter(Parameters.class, "patient", new StringType("Patient-1153"))
                .andParameter("periodStart", new DateTimeType("2018-01"))
                .andParameter("periodEnd", new DateTimeType("2018-12"))
                .useHttpGet()
                .execute();

        MeasureReport report = (MeasureReport)result.getParameter().get(0).getResource();
        Bundle bundle = report.getEvaluatedResourcesTarget();
        JpaDataProvider provider = new JpaDataProvider(getResourceProviders());
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            // TODO: Simple creates for now, needs to handle update
            JpaResourceProviderDstu3 resourceProvider = provider.resolveResourceProvider(entry.fhirType());
            resourceProvider.getDao().create(entry.getResource());
        }
    }
}
