package org.opencds.cqf.r4.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleUtil;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.Observation;
import org.opencds.cqf.common.config.HapiProperties;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.opencds.cqf.common.config.HapiProperties.getObservationTransformReplaceCode;
import static org.opencds.cqf.common.helpers.ClientHelper.getClient;

public class ObservationProvider {

    private FhirContext fhirContext;

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
        HashMap<String, String> codeMappings = new HashMap<>();
        transformConceptMap.getGroup().forEach(group -> group.getElement().forEach(codeElement -> codeMappings.put(codeElement.getCode(), codeElement.getTarget().get(0).getCode())));

        List<Observation> observations = (List<Observation>) BundleUtil.toListOfResources(fhirContext, observationsBundle).stream()
                .filter(resource -> resource instanceof Observation)
                .map(Observation.class::cast)
                .collect(Collectors.toList());
        observations.forEach(observation -> {
            if (codeMappings.get(observation.getCode().getCoding().get(0).getCode()) != null) {
                if(HapiProperties.getObservationTransformReplaceCode()){
                    observation.getCode().getCoding().get(0).setCode(codeMappings.get(observation.getCode().getCoding().get(0).getCode()));
                }else{
                    observation.getCode().getCoding().add(new Coding("", codeMappings.get(observation.getCode().getCoding().get(0).getCode()), observation.getCode().getCoding().get(0).getDisplay()));
                }
            }
        });

        return observationsBundle;
    }
}
