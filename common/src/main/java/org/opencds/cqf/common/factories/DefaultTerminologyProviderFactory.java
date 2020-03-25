  
package org.opencds.cqf.common.factories;

import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.apache.commons.lang3.tuple.Pair;
import org.opencds.cqf.common.helpers.ClientHelper;
import org.opencds.cqf.common.providers.Dstu3ApelonFhirTerminologyProvider;
import org.opencds.cqf.common.providers.R4ApelonFhirTerminologyProvider;
import org.opencds.cqf.cql.terminology.fhir.Dstu3FhirTerminologyProvider;
import org.opencds.cqf.cql.terminology.fhir.R4FhirTerminologyProvider;

import java.util.HashMap;
import java.util.Map;

import com.alphora.cql.service.factory.TerminologyProviderFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class DefaultTerminologyProviderFactory<Endpoint> implements TerminologyProviderFactory {
    private FhirContext fhirContext;
    private TerminologyProvider localServerTerminologyProvider;
    private Map<String, Endpoint> endpointIndex;
    //This is a workaround for now these should be removed in the future
    private IGenericClient client;

    public DefaultTerminologyProviderFactory(FhirContext fhirContext, TerminologyProvider localServerTerminologyProvider, Map<String, Endpoint> endpointIndex) {
        this.fhirContext = fhirContext;
        this.localServerTerminologyProvider = localServerTerminologyProvider;
        this.endpointIndex = endpointIndex;
    }

    public DefaultTerminologyProviderFactory() {
    }

    @Override
	public TerminologyProvider create(Map<String, Pair<String, String>> modelVersionsAndUrls, String terminologyUri) {
		return create(terminologyUri);
    }
    
    TerminologyProvider create(String terminologyUri) {
        // null = local database connection
        if (terminologyUri == null) {
            return localServerTerminologyProvider;
        }
        // fileuri = file connection
        // if (terminologyUri == fileuri *This already exists in the Service project* ) {
        //     throw new IllegalArgumentException("File Uri is not supported by this server.");
        // }
        // remoteuri = client connection
        // terminologyUri -> endpoint -> client -> provider
        Endpoint endpoint = this.endpointIndex.get(terminologyUri);
        IGenericClient client;
        if (endpoint instanceof org.hl7.fhir.r4.model.Endpoint) {
           client = ClientHelper.getClient(fhirContext, (org.hl7.fhir.r4.model.Endpoint)endpoint);
        }
        else 
            client = ClientHelper.getClient(fhirContext, (org.hl7.fhir.dstu3.model.Endpoint)endpoint);
        
		switch(fhirContext.getVersion().getVersion().getFhirVersionString()) {
            case "4.0.0":
                return createR4TerminologyProvider(client);

            case "3.0.0":
                return createDstu3TerminologyProvider(client);

            default: return this.localServerTerminologyProvider;
        }
    }

    public TerminologyProvider createR4TerminologyProvider(IGenericClient client) {
        if (client.getServerBase() != null && client.getServerBase().contains("apelon.com")) {
            return new R4ApelonFhirTerminologyProvider(client);
        }
        else if (client.getServerBase() != null && !client.getServerBase().isEmpty()) {
            return new R4FhirTerminologyProvider(client);
        } else
            return this.localServerTerminologyProvider;
    }

    public TerminologyProvider createDstu3TerminologyProvider(IGenericClient client) {
        if (client.getServerBase() != null && client.getServerBase().contains("apelon.com")) {
            return new Dstu3ApelonFhirTerminologyProvider(client);
        }
        else if (client.getServerBase() != null && !client.getServerBase().isEmpty()) {
            return new Dstu3FhirTerminologyProvider(client);
        } else
            return this.localServerTerminologyProvider;
    }

	public void setClient(IGenericClient client) {
		this.client = client;
	}
}