package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.rp.dstu3.StructureMapResourceProvider;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.utils.StructureMapUtilities;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.opencds.cqf.helpers.MockWorker;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class FHIRStructureMapResourceProvider extends StructureMapResourceProvider {

    private JpaDataProvider provider;

    public FHIRStructureMapResourceProvider(JpaDataProvider provider) {
        this.provider = provider;
    }

    // TODO - tests for this operation
    @Operation(name = "$transform", idempotent = true)
    public Resource transform(
            @IdParam IdType theId,
            @ResourceParam Resource resource
    ) throws FHIRException  {

        // 0 Initialise
        IFhirResourceDao<? extends IAnyResource> fhirResourceDao = this.getDao();
        if (fhirResourceDao == null){
            throw new FHIRException("DAO for ActivityDefinition not found");
        }

        StructureMap structuredMap = (StructureMap) fhirResourceDao.read( theId );
        if (structuredMap == null){
            throw new FHIRException("StructureMap " + theId + " not found");
        }

        // 1 Create result
        List<StructureMap.StructureMapStructureComponent> structureMapStructureComponents = structuredMap.getStructure().stream()
                .filter( structureMapStructureComponent -> structureMapStructureComponent.getMode().equals(StructureMap.StructureMapModelMode.PRODUCED)
                        || structureMapStructureComponent.getMode().equals(StructureMap.StructureMapModelMode.TARGET))
                .collect(Collectors.toList());

        if (structureMapStructureComponents.size() != 1){
            throw new FHIRException("StructureMap has more than one TARGET and PRODUCED");
        }

        Resource result = null;
        Class resultType = null;
        try {
            StructureMap.StructureMapStructureComponent structure = structureMapStructureComponents.get(0);
            resultType = Class.forName(structure.getUrl());
            result = (Resource) resultType.newInstance();
        } catch (Exception e) {
            throw new FHIRException("StructureMap can not instantiate result resource");
        }

        // 2 process structure map
        MockWorker myWorker = new MockWorker( this.provider );

        // TODO Map should contain all structure maps.
        Map<String, StructureMap> mapTreeMap = new TreeMap<>();
        mapTreeMap.put(structuredMap.getUrl(), structuredMap);

        StructureMapUtilities structureMapUtilities = new StructureMapUtilities(myWorker, mapTreeMap);
        structureMapUtilities.transform(null, new ActivityDefinition(), structuredMap, result);
        return result;
    }

}
