package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.model.api.annotation.Description;
//import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.cqframework.fhir.api.FhirDal;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;

@SuppressWarnings("unchecked")
public class RepositoryService extends DaoRegistryOperationProvider {

	@Autowired
	private JpaFhirDalFactory fhirDalFactory;

	@Autowired
		private KnowledgeArtifactProcessor artifactProcessor;

	 SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");

	private static final String dateValid = "2022-01-01";
	private static final String dateValidAfter = "2022-06-01";
	private static final String dateInvalid = "bad-date";

	private static final RequestDetails requestDetails = new ServletRequestDetails();
	{
		requestDetails.addParameter("dateValid", new String[] { dateValid });
		requestDetails.addParameter("dateAfter", new String[] { dateValidAfter });
		requestDetails.addParameter("dateMultiple", new String[] { dateValid, dateValidAfter });
		requestDetails.addParameter("dateInvalid", new String[] { dateInvalid });
		requestDetails.addParameter("dateNull", new String[] { null });
		requestDetails.addParameter("dateEmpty", new String[] { "" });
	}

    public RepositoryService() {}

	 @Operation(name = "$draft")
	 @Description(shortDefinition = "$draft", value = "Create a new draft library version")
	 public Library draftOperation(RequestDetails requestDetails, @OperationParam(name = "currentLibrary") Library currentLibrary) throws FHIRException {

		 FhirDal fhirDal = (FhirDal) this.fhirDalFactory.create(requestDetails);

		 Library draftLibrary = (Library)this.artifactProcessor.newVersion(currentLibrary, fhirDal);

		return draftLibrary;
	 }

	@Operation(name = "$release", idempotent = false)
	@Description(shortDefinition = "$release", value = "Release an existing draft artifact")
	public Resource releaseOperation(@OperationParam(name="iIdtype") IdType iIdType) throws FHIRException {

		FhirDal fhirDal = (FhirDal) this.fhirDalFactory.create(requestDetails);

		Library draftLibrary = (Library) this.artifactProcessor.releaseVersion(iIdType, fhirDal);

		return draftLibrary;
	}

	@Operation(name = "$publish")
	@Description(shortDefinition = "$publish", value = "Post a new artifact with active status")
	public Resource publishVersion(RequestDetails requestDetails, @OperationParam(name = "iIdType") IdType iIdType) throws FHIRException {

		FhirDal fhirDal = (FhirDal) fhirDalFactory.create(requestDetails);

		Library draftLibrary = (Library) this.artifactProcessor.publishVersion(iIdType, fhirDal);

		return draftLibrary;

	}
}
