package org.opencds.cqf.servlet;

import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.dstu3.model.*;
import org.opencds.cqf.providers.JpaDataProvider;

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
    // $evaluate-measure?patient=Patient-1153&periodStart=2017-01&periodEnd=2017-12&lastUpdated=@lastUpdated

    // To hit this: POST <base>/cqf-ruler/on-measure-subscription-mrp
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

    /*
        Chris Schuler's comments:
            I think I understand what the above POST request is trying to accomplish, but I think we can do better.
            Why are we running the measure again only to get the evaluated resources to update this server? Or
            is the subscribed-to server updating the resources and this endpoint merely re-runs the measure? If the
            former question is true, then I think it is kind of ugly. If the latter, then disregard the rest of this.
            From what I understand from reading this: http://hl7.org/fhir/subscription.html#2.46.6.1, we can
            configure a much cleaner way in the subscription.

            I am suggesting the Subscription resources look something like the following:
                Subscription
                    status: active
                    criteria: ...
                    channel:
                        type: rest-hook
                        endpoint: <base>/cqf-ruler/baseDstu3
                        payload: application/fhir+json or application/fhir+xml
                        header: ...

            An example for the BCS measure (during 1997 measurement year) would be the following criteria
            for the "Is Bilateral Mastectomy" expression:
                Procedure?status=completed&date=eb1997-12-31&code:in=2.16.840.1.113883.3.464.1004.1042
            We would need Subscriptions for all the data requirements in the library, which could be automated
            using the $data-requirements operation.
            With this Subscription the subscribed-to server could simply POST that resource (or a Bundle
            of resources) to this endpoint.
     */
    /*protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean isJson = true;
        if (request.getContentType().startsWith("application/fhir+xml")) {
            isJson = false;
        }
        else if (!request.getContentType().startsWith("application/fhir+json")) {
            throw new ServletException(String.format("Exprecting content type of application/fhir+json or application/fhir+xml, found %s.", request.getContentType()));
        }

        JpaDataProvider provider = new JpaDataProvider(getResourceProviders());
        Resource resource = (Resource) (isJson
                                ? provider.getFhirContext().newJsonParser().parseResource(request.getReader())
                                : provider.getFhirContext().newXmlParser().parseResource(request.getReader()));

        if (resource instanceof Bundle) {
            for (Bundle.BundleEntryComponent entry : ((Bundle) resource).getEntry()) {
                if (entry.hasResource()) {
                    JpaResourceProviderDstu3 resourceProvider = provider.resolveResourceProvider(entry.fhirType());
                    resourceProvider.getDao().update(entry.getResource());
                }
            }
        }

        else {
            JpaResourceProviderDstu3 resourceProvider = provider.resolveResourceProvider(resource.fhirType());
            resourceProvider.getDao().update(resource);
        }
    }*/
}
