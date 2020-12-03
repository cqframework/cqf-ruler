package org.opencds.cqf.dstu3.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleUtil;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ConceptMap;
import org.hl7.fhir.dstu3.model.Observation;
import org.opencds.cqf.common.config.HapiProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import static org.opencds.cqf.common.helpers.ClientHelper.getClient;

@Component
public class ObservationProvider {

    private FhirContext fhirContext;

    @Inject
    public ObservationProvider(FhirContext fhirContext){
        this.fhirContext = fhirContext;
    }

    @Operation(name = "$transform", idempotent = false, type = Observation.class)
    public Bundle transformObservations(
            @OperationParam(name = "observations") Bundle observationsBundle,
            @OperationParam(name = "conceptMapURL") String conceptMapURL
    ) {
        if(null == observationsBundle) {
            throw new IllegalArgumentException("Unable to perform operation Observation$transform.  No Observation bundle passed in.");
        }
        if(null == conceptMapURL) {
            throw new IllegalArgumentException("Unable to perform operation Observation$transform.  No concept map url specified.");
        }
        IGenericClient client = getClient(fhirContext, conceptMapURL, HapiProperties.getObservationTransformUsername(), HapiProperties.getObservationTransformPassword() );
        ConceptMap transformConceptMap = client.read().resource(ConceptMap.class).withUrl (conceptMapURL).execute();
        if(null == transformConceptMap) {
            throw new IllegalArgumentException("Unable to perform operation Observation$transform.  Unable to get concept map.");
        }
        List<Observation> observations = BundleUtil.toListOfResources(fhirContext, observationsBundle).stream()
                .filter(resource -> resource instanceof Observation)
                .map(Observation.class::cast)
                .collect(Collectors.toList());
        /**
         * TODO - There must be a more efficient way to loop through this, but so far I have not come up with it.
         */
        transformConceptMap.getGroup().forEach(group -> {
            HashMap<String, ConceptMap.TargetElementComponent> codeMappings = new HashMap<>();
            String targetSystem = group.getTarget();
            group.getElement().forEach(codeElement -> {
                codeMappings.put(codeElement.getCode(), codeElement.getTarget().get(0));
            });
            observations.forEach(observation -> {
                if(observation.getValue().fhirType().equalsIgnoreCase("codeableconcept")){
                    String obsValueCode = observation.getValueCodeableConcept().getCoding().get(0).getCode();
                    if(obsValueCode != null) {
                        if (codeMappings.get(observation.getValueCodeableConcept().getCoding().get(0).getCode()) != null) {
                            if (HapiProperties.getObservationTransformReplaceCode()) {
                                observation.getValueCodeableConcept().getCoding().get(0).setCode(codeMappings.get(obsValueCode).getCode());
                                observation.getValueCodeableConcept().getCoding().get(0).setDisplay(codeMappings.get(obsValueCode).getDisplay());
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
                }
            });
        });
        return observationsBundle;
    }
}
