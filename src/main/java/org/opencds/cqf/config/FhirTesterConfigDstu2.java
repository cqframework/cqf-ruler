package org.opencds.cqf.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.to.FhirTesterMvcConfig;
import ca.uhn.fhir.to.TesterConfig;

/**
 * Created by Christopher on 5/4/2017.
 */
@Configuration
@Import(FhirTesterMvcConfig.class)
public class FhirTesterConfigDstu2 {

    @Bean
    public TesterConfig testerConfig() {
        TesterConfig retVal = new TesterConfig();
        retVal
                .addServer()
                .withId("home")
                .withFhirVersion(FhirVersionEnum.DSTU2)
                .withBaseUrl("${serverBase}/baseDstu2")
                .withName("Local Tester")
                .addServer()
                .withId("hapi")
                .withFhirVersion(FhirVersionEnum.DSTU2)
                .withBaseUrl("http://fhirtest.uhn.ca/baseDstu2")
                .withName("Public HAPI Test Server");
        return retVal;
    }
}
