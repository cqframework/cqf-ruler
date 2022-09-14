package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.cqframework.fhir.api.FhirDal;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Library;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;

@SuppressWarnings("unchecked")
public class RepositoryService extends DaoRegistryOperationProvider {

	@Autowired
	private JpaFhirDalFactory fhirDalFactory;

	 SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");

    public RepositoryService() {}

	 @Operation(name = "$draft", idempotent = true, type = Library.class)
	 @Description(shortDefinition = "$draft", value = "Create a new draft library version")
	 public Library draftOperation(RequestDetails requestDetails, @OperationParam(name = "currentLibrary") Library currentLibrary) throws FHIRException {
		FhirDal fhirDal = (FhirDal) this.fhirDalFactory.create(requestDetails);

		KnowledgeArtifactProcessor processor = new KnowledgeArtifactProcessor();
		//TODO: Autowire the processor?
		 Library draftLibrary = (Library)processor.newVersion(currentLibrary, fhirDal);

		return draftLibrary;
	 }
}
