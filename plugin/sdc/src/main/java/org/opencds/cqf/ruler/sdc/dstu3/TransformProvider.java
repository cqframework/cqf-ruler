package org.opencds.cqf.ruler.sdc.dstu3;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ConceptMap;
import org.hl7.fhir.dstu3.model.Observation;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.sdc.SDCProperties;
import org.opencds.cqf.ruler.utility.Clients;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleUtil;

public class TransformProvider implements OperationProvider {

    @Autowired
    private FhirContext fhirContext;

    @Autowired
    private SDCProperties mySdcProperties;

    private String replaceCode;
    private String username;
    private String password;
    private String endpoint; // Not needed maybe

    @Operation(name = "$transform", idempotent = false, type = Observation.class)
    public Bundle transformObservations(
            @OperationParam(name = "observations") Bundle observationsBundle,
            @OperationParam(name = "conceptMapURL") String conceptMapURL) {
        if (null == observationsBundle) {
            throw new IllegalArgumentException(
                    "Unable to perform operation Observation$transform.  No Observation bundle passed in.");
        }
        if (null == conceptMapURL) {
            throw new IllegalArgumentException(
                    "Unable to perform operation Observation$transform.  No concept map url specified.");
        }

        this.replaceCode = mySdcProperties.getTransform().getReplaceCode();
        this.username = mySdcProperties.getTransform().getUsername();
        this.password = mySdcProperties.getTransform().getPassword();
        this.endpoint = mySdcProperties.getTransform().getEndpoint();

        IGenericClient client = Clients.forUrl(fhirContext, this.endpoint);
        ConceptMap transformConceptMap = client.read().resource(ConceptMap.class).withUrl(conceptMapURL).execute();
        if (null == transformConceptMap) {
            throw new IllegalArgumentException(
                    "Unable to perform operation Observation$transform.  Unable to get concept map.");
        }
        List<Observation> observations = BundleUtil.toListOfResources(fhirContext, observationsBundle).stream()
                .filter(resource -> resource instanceof Observation)
                .map(Observation.class::cast)
                .collect(Collectors.toList());
        /**
         * TODO - There must be a more efficient way to loop through this, but so far I
         * have not come up with it.
         */
        transformConceptMap.getGroup().forEach(group -> {
            HashMap<String, ConceptMap.TargetElementComponent> codeMappings = new HashMap<>();
            String targetSystem = group.getTarget();
            group.getElement().forEach(codeElement -> {
                codeMappings.put(codeElement.getCode(), codeElement.getTarget().get(0));
            });
            observations.forEach(observation -> {
                if (observation.getValue().fhirType().equalsIgnoreCase("codeableconcept")) {
                    String obsValueCode = observation.getValueCodeableConcept().getCoding().get(0).getCode();
                    if (obsValueCode != null && codeMappings
                            .get(observation.getValueCodeableConcept().getCoding().get(0).getCode()) != null) {
                        if (this.replaceCode != null) {
                            observation.getValueCodeableConcept().getCoding().get(0)
                                    .setCode(codeMappings.get(obsValueCode).getCode());
                            observation.getValueCodeableConcept().getCoding().get(0)
                                    .setDisplay(codeMappings.get(obsValueCode).getDisplay());
                            observation.getValueCodeableConcept().getCoding().get(0).setSystem(targetSystem);
                        } else {
                            Coding newCoding = new Coding();
                            newCoding.setSystem(targetSystem);
                            newCoding.setCode(codeMappings.get(obsValueCode).getCode());
                            newCoding.setDisplay(codeMappings.get(obsValueCode).getDisplay());
                            observation.getValueCodeableConcept().getCoding().add(newCoding);
                        }
                    }
                }
            });
        });
        return observationsBundle;
    }
}
