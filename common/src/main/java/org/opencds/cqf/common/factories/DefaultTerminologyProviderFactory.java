  
package org.opencds.cqf.common.factories;

import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.apache.commons.lang3.tuple.Pair;
import org.opencds.cqf.common.providers.Dstu3ApelonFhirTerminologyProvider;
import org.opencds.cqf.common.providers.R4ApelonFhirTerminologyProvider;
import org.opencds.cqf.cql.terminology.fhir.Dstu3FhirTerminologyProvider;
import org.opencds.cqf.cql.terminology.fhir.R4FhirTerminologyProvider;

import java.util.Map;

import com.alphora.cql.service.factory.TerminologyProviderFactory;

import ca.uhn.fhir.context.FhirContext;

public class DefaultTerminologyProviderFactory implements TerminologyProviderFactory{
    private FhirContext fhirContext;
    private TerminologyProvider defaultTerminologyProvider;
    //This is a workaround for now these should be removed in the future
    private String user;
    private String pass;

    public DefaultTerminologyProviderFactory(FhirContext fhirContext, TerminologyProvider defaultTerminologyProvider) {
        this.fhirContext = fhirContext;
        this.defaultTerminologyProvider = defaultTerminologyProvider;
    }

    public DefaultTerminologyProviderFactory() {
    }

    @Override
	public TerminologyProvider create(Map<String, Pair<String, String>> modelVersionsAndUrls, String terminologyUri) {
		return create(terminologyUri);
    }
    
    TerminologyProvider create(String terminologyUri) {
		switch(fhirContext.getVersion().getVersion().getFhirVersionString()) {
            case "4.0.0":
                return createR4TerminologyProvider(terminologyUri, user, pass);

            case "3.0.0":
                return createDstu3TerminologyProvider(terminologyUri, user, pass);

            default: return this.defaultTerminologyProvider;
        }
    }

    public TerminologyProvider createR4TerminologyProvider(String terminologyUri, String user, String pass) {
        if (terminologyUri != null && terminologyUri.contains("apelon.com")) {
            return new R4ApelonFhirTerminologyProvider(this.fhirContext)
            .withBasicAuth(user, pass).setEndpoint(terminologyUri, false);
        }
        else if (terminologyUri != null && !terminologyUri.isEmpty()) {
            return new R4FhirTerminologyProvider(this.fhirContext).withBasicAuth(user, pass).setEndpoint(terminologyUri, false);
        } else
            return this.defaultTerminologyProvider;
    }

    public TerminologyProvider createDstu3TerminologyProvider(String terminologyUri, String user, String pass) {
        if (terminologyUri != null && user != null && pass != null && terminologyUri.contains("apelon.com")) {
            return new Dstu3ApelonFhirTerminologyProvider(this.fhirContext)
            .withBasicAuth(user, pass).setEndpoint(terminologyUri, false);
        }
        else if (terminologyUri != null && user != null && pass != null && !terminologyUri.isEmpty()) {
            return new Dstu3FhirTerminologyProvider(this.fhirContext).withBasicAuth(user, pass).setEndpoint(terminologyUri, false);
        } else
            return this.defaultTerminologyProvider;
    }

	public void setUser(String user) {
		this.user = user;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}
}