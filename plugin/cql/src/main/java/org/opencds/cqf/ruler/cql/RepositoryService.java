package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.cqframework.fhir.api.FhirDal;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.ContactDetail;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

public class RepositoryService extends DaoRegistryOperationProvider {

	@Autowired
	private JpaFhirDalFactory fhirDalFactory;

	@Autowired
	private KnowledgeArtifactProcessor artifactProcessor;

	/**
	 * Applies an approval to an existing artifact, regardless of status.
	 *
	 * @param requestDetails      the {@link RequestDetails RequestDetails}
	 * @param theId					      the {@link IdType IdType}, always an argument for instance level operations
	 * @param approvalDate        Optional Date parameter for indicating the date of approval
	 *                            for an approval submission. If approvalDate is not
	 *                           	provided, the current date will be used.
	 * @param artifactCommentType
	 * @param artifactCommentText
	 * @param artifactCommentTarget
	 * @param artifactCommentReference
	 * @param artifactCommentUser Optional ArtifactComment* arguments represent parts of a
	 *                            comment to beincluded as part of the approval. The
	 *                            artifactComment is a cqfm-artifactComment as defined here:
	 *                            http://hl7.org/fhir/us/cqfmeasures/STU3/StructureDefinition-cqfm-artifactComment.html
	 *                            A Parameters resource with a parameter for each element
	 *                            of the artifactComment Extension definition is
	 *                            used to represent the proper structure.
	 * @param endorser            A ContactDetail resource that represents the
	 *                            person that is providing the approval and comment.
	 * @return An IBaseResource that is the targeted resource, updated with the approval
	 */
	@Operation(name = "$approve", idempotent = true, global = true, type = MetadataResource.class)
	@Description(shortDefinition = "$approve", value = "Apply an approval to an existing artifact, regardless of status.")
	public MetadataResource approveOperation(
			RequestDetails requestDetails,
			@IdParam IdType theId,
			@OperationParam(name = "approvalDate", typeName = "Date") IPrimitiveType<Date> approvalDate,
			@OperationParam(name = "artifactCommentType") String artifactCommentType,
			@OperationParam(name = "artifactCommentText") String artifactCommentText,
			@OperationParam(name = "artifactCommentTarget") String artifactCommentTarget,
			@OperationParam(name = "artifactCommentReference") String artifactCommentReference,
			@OperationParam(name = "artifactCommentUser") String artifactCommentUser,
			@OperationParam(name = "endorser") ContactDetail endorser) throws UnprocessableEntityException {
				FhirDal fhirDal = this.fhirDalFactory.create(requestDetails);
		return this.artifactProcessor.approve(theId, approvalDate.getValue(), artifactCommentType,
				artifactCommentText, artifactCommentTarget, artifactCommentReference, artifactCommentUser, endorser, fhirDal);
	}

	@Operation(name = "$draft", idempotent = true, global = true, type = MetadataResource.class)
	@Description(shortDefinition = "$draft", value = "Create a new draft version of the reference artifact")
	public Library draftOperation(RequestDetails requestDetails, @IdParam IdType theId, @OperationParam(name = "version") String version)
		throws FHIRException {
		FhirDal fhirDal = this.fhirDalFactory.create(requestDetails);
		return (Library) this.artifactProcessor.draft(theId, fhirDal, version);
	}

	@Operation(name = "$release", idempotent = true, global = true, type = MetadataResource.class)
	@Description(shortDefinition = "$release", value = "Release an existing draft artifact")
	public Library releaseOperation(
		RequestDetails requestDetails,
		@IdParam IdType theId,
		@OperationParam(name = "version") String version,
		@OperationParam(name = "version-behavior") CodeType versionBehavior,
		@OperationParam(name = "latest-from-tx-server", typeName = "Boolean") IPrimitiveType<Boolean> latestFromTxServer)
		throws FHIRException {

		FhirDal fhirDal = this.fhirDalFactory.create(requestDetails);
		return (Library) this.artifactProcessor.releaseVersion(theId, version, versionBehavior, latestFromTxServer != null && latestFromTxServer.getValue(), fhirDal);
	}

	@Operation(name = "$revise", idempotent = true, global = true, type = MetadataResource.class)
	@Description(shortDefinition = "$revise", value = "Update an existing artifact in 'draft' status")
	public IBaseResource reviseOperation(RequestDetails requestDetails, @OperationParam(name = "resource") IBaseResource resource)
		throws FHIRException {

		FhirDal fhirDal = fhirDalFactory.create(requestDetails);
		return (IBaseResource)this.artifactProcessor.revise(fhirDal, (MetadataResource) resource);
	}
}
