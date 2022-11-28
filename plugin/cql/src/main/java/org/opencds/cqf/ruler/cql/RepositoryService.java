package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.cqframework.fhir.api.FhirDal;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.springframework.beans.factory.annotation.Autowired;

public class RepositoryService extends DaoRegistryOperationProvider {

	@Autowired
	private JpaFhirDalFactory fhirDalFactory;

	@Autowired
	private KnowledgeArtifactProcessor artifactProcessor;

	 @Operation(name = "$draft", idempotent = true, global = true, type = MetadataResource.class)
	 @Description(shortDefinition = "$draft", value = "Create a new draft version of the reference artifact")
	 public Library draftOperation(RequestDetails requestDetails, @IdParam IdType theId)
		 throws FHIRException {
		 FhirDal fhirDal = this.fhirDalFactory.create(requestDetails);
		 return (Library) this.artifactProcessor.draft(theId, fhirDal);
	 }

	@Operation(name = "$release", idempotent = true, global = true, type = MetadataResource.class)
	//@Operation(name = "$release", global = true)
	@Description(shortDefinition = "$release", value = "Release an existing draft artifact")
	public Library releaseOperation(RequestDetails requestDetails, @IdParam IdType theId)
	//@Operation(name = "$release")
	//@Description(shortDefinition = "$release", value = "Release an existing draft artifact")
	//public Library releaseOperation(RequestDetails requestDetails, @OperationParam(name = "specification") IdType theId)
		throws FHIRException {
		FhirDal fhirDal = this.fhirDalFactory.create(requestDetails);
		return (Library) this.artifactProcessor.releaseVersion(theId, fhirDal);
	}

	@Operation(name = "$publish")
	@Description(shortDefinition = "$publish", value = "Post a new artifact with active status")
	public Library publishVersion(RequestDetails requestDetails, @OperationParam(name = "specification") Library library)
		throws FHIRException {
		FhirDal fhirDal = fhirDalFactory.create(requestDetails);
		return (Library) this.artifactProcessor.publishVersion(fhirDal, library);
	}

	@Operation(name = "$revise")
	@Description(shortDefinition = "$revise", value = "Put an existing with active draft")
	public Library revise(RequestDetails requestDetails, @OperationParam(name = "specification") Library library)
		throws FHIRException {
		FhirDal fhirDal = fhirDalFactory.create(requestDetails);
		return (Library) this.artifactProcessor.revise(fhirDal, library);
	}
}