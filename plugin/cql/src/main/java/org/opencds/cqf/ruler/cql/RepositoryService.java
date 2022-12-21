package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
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
	@Description(shortDefinition = "$release", value = "Release an existing draft artifact")
	public Library releaseOperation(RequestDetails requestDetails, @IdParam IdType theId)
		throws FHIRException {
		FhirDal fhirDal = this.fhirDalFactory.create(requestDetails);
		return (Library) this.artifactProcessor.release(theId, fhirDal);
	}
}
