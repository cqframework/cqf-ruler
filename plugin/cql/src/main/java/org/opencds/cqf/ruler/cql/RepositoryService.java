package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class RepositoryService extends DaoRegistryOperationProvider {

	@Autowired
	private DaoRegistry daoRegistry;

	 SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");

    public RepositoryService() {}

	//public DaoRegistry getDaoRegistry() {
	//	return this.daoRegistry;
	//}

	 @Operation(name = "$draft", idempotent = true, type = Library.class)
	 @Description(shortDefinition = "$draft", value = "Create a new draft library version")
		 public Library createNewVersion(@OperationParam(name = "currentLibrary") Library currentLibrary) throws FHIRException {
		 List<RelatedArtifact> relatedArtifactList = currentLibrary.getRelatedArtifact();
		 Library newLibrary = new Library();

		 for (RelatedArtifact ra : relatedArtifactList) {
			 if (ra.getType().ordinal() == RelatedArtifact.RelatedArtifactType.COMPOSEDOF.ordinal()) {
				 RelatedArtifact newRA = new RelatedArtifact();
				 if (ra.getType().toString().equals("PlanDefinition")) {
					 ra.setResource("http://ersd.aimsplatform.org/fhir/PlanDefinition/us-ecr-");
					 currentLibrary.addRelatedArtifact(ra);
					 newLibrary = createNewVersionData(currentLibrary);
					 //newLibrary.setVersion("1.0.0");
				 } else if (ra.getType().toString().equals("Library")) {
					 ra.setResource("http://ersd.aimsplatform.org/fhir/Library/us-ecr-rctc");
					 currentLibrary.addRelatedArtifact(ra);
					 newLibrary = createNewVersionData(currentLibrary);
					 //newLibrary.setVersion("1.0.0");
				 } else if (ra.getType().toString().equals("ValueSet")) {
					 ra.setResource("http://hl7.org/fhir/us/ecr/ValueSet/lotc");
					 currentLibrary.addRelatedArtifact(ra);
					 newLibrary = createNewVersionDataVS(currentLibrary);
					 //newLibrary.setVersion("1.0.0");
				 }
				 if (newRA == null) {
					 relatedArtifactList.add(newRA);
				 }
			 } else if (ra.getType().ordinal() == RelatedArtifact.RelatedArtifactType.DEPENDSON.ordinal()) {
				 RelatedArtifact newRA = new RelatedArtifact();
				 if (ra.getType().toString().equals("PlanDefinition")) {
					 ra.setResource("http://cts.nlm.nih.gov/fhir/PlanDefinition/2.16");
					 currentLibrary.addRelatedArtifact(ra);
					 newLibrary = createNewVersionData(currentLibrary);
					 //newLibrary.setVersion(null);
				 } else if (ra.getType().toString().equals("Library")) {
					 ra.setResource("http://cts.nlm.nih.gov/fhir/Library");
					 currentLibrary.addRelatedArtifact(ra);
					 newLibrary = createNewVersionData(currentLibrary);
					 //newLibrary.setVersion(null);
				 } else if (ra.getType().toString().equals("ValueSet")) {
					 ra.setResource("http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.4");
					 currentLibrary.addRelatedArtifact(ra);
					 newLibrary = createNewVersionDataVS(currentLibrary);
					 //newLibrary.setVersion(null);
				 }
				 if (newRA == null) {
					 relatedArtifactList.add(newRA);
				 }
			 }
		 }
		 return newLibrary;
	 }

	 private Library createNewVersionData(Library currentLibrary) {
		 Library newLibrary = new Library();

		 newLibrary.setUrl(currentLibrary.getUrl());
		 newLibrary.setName(currentLibrary.getName());
		 newLibrary.setTitle(currentLibrary.getTitle());
		 newLibrary.setDescription(currentLibrary.getDescription());
		 newLibrary.setStatus(Enumerations.PublicationStatus.ACTIVE);
		 //set id to null
		 newLibrary.setId("");
		 newLibrary.setVersion("draft-" + sdf.format(new Date()));
		 newLibrary.setRelatedArtifact(currentLibrary.getRelatedArtifact());
		 return newLibrary;
	 }

	private Library createNewVersionDataVS(Library currentLibrary) {
		Library newLibrary = new Library();

		newLibrary.setUrl(currentLibrary.getUrl());
		newLibrary.setName(currentLibrary.getName());
		newLibrary.setTitle(currentLibrary.getTitle());
		newLibrary.setDescription(currentLibrary.getDescription());
		newLibrary.setStatus(Enumerations.PublicationStatus.ACTIVE);
		newLibrary.setId("");
		newLibrary.setVersion("draft-" + sdf.format(new Date()));
		newLibrary.setContained(currentLibrary.getContained());
		return newLibrary;
	}

	@Operation(name = "$release", idempotent = true, type = Resource.class)
	@Description(shortDefinition = "$release", value = "Update an existing draft artifact to active")
	public Resource releaseResource(@OperationParam(name = "iIdType") IIdType iIdType) throws FHIRException {

		 Resource resource = read(iIdType);
		 String resourceVersionId = resource.getMeta().getVersionId();
		 if(!resourceVersionId.contains("draft")) {
			 throw new FHIRException("Resource found by " + iIdType + " is not a draft version.  Please select a draft resource.");
		 }
		 if(!resourceVersionId.startsWith("draft")) {
			 throw new FHIRException("Resource found by " + iIdType + " is not a draft version.  The version does not start with draft.");
		 }
		 String[] resourceIdArray = resourceVersionId.split("draft-");
		 String updatedResourceId = resourceIdArray[1];
		 resource.setId(updatedResourceId);

		 Bundle bundle = new Bundle();
		 bundle.getEntry().forEach(
			 entry -> {
				if(entry.hasResource() && entry.getResource() instanceof Library) {
					Enumerations.PublicationStatus status = ((Library) entry.getResource()).getStatus();
					if(!status.equals(Enumerations.PublicationStatus.ACTIVE)) {
						((Library) entry.getResource()).setStatus(Enumerations.PublicationStatus.ACTIVE);
					}
				}
			}
		 );

		 update(resource);

		 return resource;

	}

	@Operation(name = "$publish", idempotent = true, type = Resource.class)
	@Description(shortDefinition = "$publish", value = "Post a new artifact with active status")
	public Resource publishResource(@OperationParam(name = "iIdType") IIdType iIdType) throws FHIRException {

		Resource resource = read(iIdType);
		String resourceVersionId = resource.getMeta().getVersionId();
		if(!resourceVersionId.contains("draft")) {
			throw new FHIRException("Resource found by " + iIdType + " is not a draft version.  Please select a draft resource.");
		}
		if(!resourceVersionId.startsWith("draft")) {
			throw new FHIRException("Resource found by " + iIdType + " is not a draft version.  The version does not start with draft.");
		}
		String[] resourceIdArray = resourceVersionId.split("draft-");
		String updatedResourceId = resourceIdArray[1];
		resource.setId(updatedResourceId);

		Bundle bundle = new Bundle();
		bundle.getEntry().forEach(
			entry -> {
				if(entry.hasResource() && entry.getResource() instanceof Library) {
					Enumerations.PublicationStatus status = ((Library) entry.getResource()).getStatus();
					if(status.equals(Enumerations.PublicationStatus.ACTIVE)) {
						create(resource);
					}
				}
			}
		);

		return resource;

	}

	//@Override
	public Resource read(IIdType iIdType) {
		 return (Resource) daoRegistry.getResourceDao(iIdType.getResourceType()).read(iIdType);
	}

	//@Override
	public void create(Resource resource) {

		 daoRegistry.getResourceDao(resource.fhirType()).create(resource);
	}

	//@Override
	public void update(Resource resource) {

		 daoRegistry.getResourceDao(resource.fhirType()).update(resource);
	}

	//@Override
	public Bundle search(String s, Map<String, List<IQueryParameterType>> map) {
		return null;
	}
}
