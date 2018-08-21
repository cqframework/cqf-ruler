package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.rp.dstu3.StructureMapResourceProvider;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import dstu3.hapi.ctx.MyWorkerContext;
import org.hl7.fhir.dstu3.hapi.validation.DefaultProfileValidationSupport;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.utils.StructureMapUtilities;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IAnyResource;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
            javax.servlet.http.HttpServletRequest theServletRequest,
            RequestDetails theRequestDetails,
            @IdParam IdType theId,
            @OptionalParam(name="source")  String source
    ) throws FHIRException  {

        // 0 Initialise
        IFhirResourceDao<? extends IAnyResource> fhirResourceDao = this.getDao();
        if (fhirResourceDao == null){
            throw new FHIRException("DAO for StructureMap not found");
        }

        StructureMap structuredMap = (StructureMap) fhirResourceDao.read( theId );
        if (structuredMap == null){
            throw new FHIRException("StructureMap " + theId + " not found");
        }

        // 1 retrieve parameters
        Object parObject = theRequestDetails.getUserData().get("ca.uhn.fhir.rest.annotation.OperationParam_PARSED_RESOURCE");
        Parameters parameters = null;
        if ( parObject instanceof Parameters ) {
            parameters = (Parameters) parObject;
        } else {
            throw new FHIRException( "Body does not contain a Parameters resource." );
        }

        Resource content = null;
        Optional<Parameters.ParametersParameterComponent> optContent = parameters.getParameter().stream()
                .filter(parametersParameterComponent -> parametersParameterComponent.hasName())
                .filter(parametersParameterComponent -> parametersParameterComponent.getName().equals("content"))
                .findFirst();
        if ( optContent.isPresent() ){
            content = optContent.get().getResource();
        }
        if ( content==null ) {
            throw new FHIRException( "Body does not contain a content resource." );
        }

//        // 2 Create result
//        List<StructureMap.StructureMapStructureComponent> structureMapStructureComponents = structuredMap.getStructure().stream()
//                .filter( structureMapStructureComponent -> structureMapStructureComponent.getMode().equals(StructureMap.StructureMapModelMode.PRODUCED)
//                        || structureMapStructureComponent.getMode().equals(StructureMap.StructureMapModelMode.TARGET))
//                .collect(Collectors.toList());
//
//        if (structureMapStructureComponents.size() != 1){
//            throw new FHIRException("StructureMap has more than one TARGET and PRODUCED");
//        }
//
//        Resource result = null;
//        Class resultType = null;
//        try {
//            StructureMap.StructureMapStructureComponent structure = structureMapStructureComponents.get(0);
//            String resourceUrl = structure.getUrl();
//            String resourceName = resourceUrl.replace("http://hl7.org/fhir/StructureDefinition/","");
//            resourceName = resourceName.substring(0,1).toUpperCase()+resourceName.substring(1);
//            resultType = Class.forName("org.hl7.fhir.dstu3.model." + resourceName);
//            result = (Resource) resultType.newInstance();
//        } catch (Exception e) {
//            throw new FHIRException("StructureMap can not instantiate result resource");
//        }
//
//        // 3 process structure map
////        MockWorker myWorker = new MockWorker( this.provider );
//        HapiWorkerContext hapiWorkerContext = new HapiWorkerContext(provider.getFhirContext(), new DefaultProfileValidationSupport());
//
//        // TODO Map should contain all structure maps.
//        Map<String, StructureMap> mapTreeMap = new TreeMap<>();
////        mapTreeMap.put(structuredMap.getUrl(), structuredMap);
//        mapTreeMap.put(structuredMap.getId(), structuredMap);
//
//        StructureMapUtilities structureMapUtilities = new StructureMapUtilities(hapiWorkerContext, mapTreeMap);
//        structureMapUtilities.transform(null, content, structuredMap, result);
//        return result;
        // 2 Create result
        return doTransform(structuredMap, content, null);
    }

    private Resource doTransform(StructureMap structuredMap, Resource content, Resource result ) throws FHIRException {
        List<StructureMap.StructureMapStructureComponent> structureMapStructureComponents = structuredMap.getStructure().stream()
                .filter( structureMapStructureComponent -> structureMapStructureComponent.getMode().equals(StructureMap.StructureMapModelMode.PRODUCED)
                        || structureMapStructureComponent.getMode().equals(StructureMap.StructureMapModelMode.TARGET))
                .collect(Collectors.toList());

        if (structureMapStructureComponents.size() != 1){
            throw new FHIRException("StructureMap has more than one TARGET and PRODUCED");
        }

        if ( result == null ) {
            try {
                StructureMap.StructureMapStructureComponent structure = structureMapStructureComponents.get(0);
                String resourceUrl = structure.getUrl();
                String resourceName = resourceUrl.replace("http://hl7.org/fhir/StructureDefinition/", "");
                //            resourceName = resourceName.substring(0,1).toUpperCase()+resourceName.substring(1);
                ResourceType rt = null;
                for (ResourceType resourceType : ResourceType.values()) {
                    if (resourceType.name().toLowerCase().equals(resourceName)) {
                        rt = resourceType;
                    }
                }
                //            resultType = Class.forName("org.hl7.fhir.dstu3.model." + rt.name());

                //            result = (Resource) resultType.newInstance();
                result = ResourceFactory.createResource(rt.name());
            } catch (Exception e) {
                throw new FHIRException("StructureMap can not instantiate result resource");
            }
        }
        // 3 process structure map
//        MockWorker myWorker = new MockWorker( this.provider );
        MyWorkerContext hapiWorkerContext = new MyWorkerContext(provider.getFhirContext(), new DefaultProfileValidationSupport());
//        HapiWorkerContext hapiWorkerContext = new HapiWorkerContext(ourCtxt, ourCtxt.newValidator());

        // TODO Map should contain all structure maps.
        Map<String, StructureMap> mapTreeMap = new TreeMap<>();
//        mapTreeMap.put(structureMap.getUrl(), structureMap);
        mapTreeMap.put(structuredMap.getId(), structuredMap);

        StructureMapUtilities structureMapUtilities = new StructureMapUtilities(hapiWorkerContext, mapTreeMap);

        structureMapUtilities.transform(null, content, structuredMap, result);
        return result;
    }

    public Resource internalTransform(Reference transformReference, Resource resource) throws FHIRException {

        return internalTransform( transformReference, resource, null );
    }

    public Resource internalTransform(Reference transformReference, Resource resource, Resource result) throws FHIRException {
        IFhirResourceDao<? extends IAnyResource> fhirResourceDao = this.getDao();
        if (fhirResourceDao == null){
            throw new FHIRException("DAO for StructureMap not found");
        }

        IdType idType = new IdType().setValue(transformReference.getReference());
        StructureMap structuredMap = (StructureMap) fhirResourceDao.read( idType );
        if (structuredMap == null){
            throw new FHIRException("StructureMap " + transformReference.getReference() + " not found");
        }
        return doTransform( structuredMap, resource, result );
    }
}
