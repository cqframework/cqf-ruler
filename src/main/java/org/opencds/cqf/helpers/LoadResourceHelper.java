package org.opencds.cqf.helpers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.primitive.IdDt;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.data.fhir.BaseFhirDataProvider;
import org.opencds.cqf.cql.data.fhir.FhirDataProviderStu3;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;

/**
 * Created by Christopher on 1/17/2017.
 */
public class LoadResourceHelper {

    private BaseFhirDataProvider GETProvider;
    private BaseFhirDataProvider POSTProvider;

    LoadResourceHelper(String getURL, String postURL) {
        this.GETProvider = new FhirDataProviderStu3().setEndpoint(getURL);
        this.GETProvider.setExpandValueSets(true);
        this.POSTProvider = new FhirDataProviderStu3().setEndpoint(postURL);
    }

    public Bundle resolveBundle(String filePath) {
        FhirContext context = FhirContext.forDstu3();
        Reader reader;
        try {
            reader = new FileReader(filePath);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Invalid file path");
        }
        return (Bundle) context.newJsonParser().parseResource(reader);
    }

    public IBaseResource getResource(String resourceId, String resourceName) {
        return GETProvider.getFhirClient().read(new IdDt(resourceId).withResourceType(resourceName));
    }

    public void loadResource(IBaseResource resource) {
        String resourceId = resource.getIdElement().getIdPart();
        POSTProvider.getFhirClient().update(new IdDt(resourceId), resource);
    }

    public void loadBundle(Bundle bundle) {
        for (Bundle.BundleEntryComponent resource : bundle.getEntry()) {
            if (resource.getResource() instanceof Bundle) {
                loadBundle((Bundle) resource.getResource());
            }
            else {
                loadResource(resource.getResource());
            }
        }
    }
}
