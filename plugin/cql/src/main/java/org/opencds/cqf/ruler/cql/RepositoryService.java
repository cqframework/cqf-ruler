package org.opencds.cqf.ruler.cql;

import java.util.Date;

import org.cqframework.fhir.api.FhirDal;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.ContactDetail;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.ruler.cql.r4.ArtifactAssessment;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

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
	public Bundle approveOperation(
			RequestDetails requestDetails,
			@IdParam IdType theId,
			@OperationParam(name = "approvalDate", typeName = "Date") IPrimitiveType<Date> approvalDate,
			@OperationParam(name = "artifactCommentType") String artifactCommentType,
			@OperationParam(name = "artifactCommentText") String artifactCommentText,
			@OperationParam(name = "artifactCommentTarget") CanonicalType artifactCommentTarget,
			@OperationParam(name = "artifactCommentReference") CanonicalType artifactCommentReference,
			@OperationParam(name = "artifactCommentUser") Reference artifactCommentUser,
			@OperationParam(name = "endorser") ContactDetail endorser) throws UnprocessableEntityException {
				FhirDal fhirDal = this.fhirDalFactory.create(requestDetails);
				MetadataResource resource = (MetadataResource) fhirDal.read(theId);
		if (resource == null) {
			throw new ResourceNotFoundException(theId);
		}
		if(artifactCommentTarget != null){
			if(Canonicals.getUrl(artifactCommentTarget) != null
			&& !Canonicals.getUrl(artifactCommentTarget).equals(resource.getUrl())){
				throw new UnprocessableEntityException("ArtifactCommentTarget URL does not match URL of resource being approved.");
			}
			if(Canonicals.getVersion(artifactCommentTarget) != null
			&& !Canonicals.getVersion(artifactCommentTarget).equals(resource.getVersion())){
				throw new UnprocessableEntityException("ArtifactCommentTarget version does not match version of resource being approved.");
			}
		} else if(artifactCommentTarget == null){
			String target = "";
			String url = resource.getUrl();
			String version = resource.getVersion();
			if (url != null) {
				target += url;
			}
			if (version!=null) {
				if (url!=null) {
					target += "|";
				}
				target += version;
			}
			if(target!=null){
				artifactCommentTarget = new CanonicalType(target);
			}
		}
		ArtifactAssessment newAssessment = this.artifactProcessor.createApprovalAssessment(
			theId,
			artifactCommentType,
			artifactCommentText,
			artifactCommentTarget,
			artifactCommentReference,
			artifactCommentUser);
		MetadataResource approvedResource =  this.artifactProcessor.approve(resource, approvalDate, endorser, newAssessment);
		Bundle transactionBundle = new Bundle()
		.setType(Bundle.BundleType.TRANSACTION)
		.addEntry(createEntry(approvedResource));
		if(newAssessment != null && newAssessment.isValidArtifactComment()){
			transactionBundle.addEntry(createEntry(newAssessment));
		}
		return transaction(transactionBundle, requestDetails);
	}
	/**
	 * Creates a draft of an existing artifact if it has status Active.
	 *
	 * @param requestDetails      the {@link RequestDetails RequestDetails}
	 * @param theId					      the {@link IdType IdType}, always an argument for instance level operations
	 * @param version             new version in the form MAJOR.MINOR.PATCH
	 * TODO: should return OperationOutcome
	 * @return A transaction bundle result of the newly created resources
	 */
	@Operation(name = "$draft", idempotent = true, global = true, type = MetadataResource.class)
	@Description(shortDefinition = "$draft", value = "Create a new draft version of the reference artifact")
	public Bundle draftOperation(RequestDetails requestDetails, @IdParam IdType theId, @OperationParam(name = "version") String version)
		throws FHIRException {
		FhirDal fhirDal = this.fhirDalFactory.create(requestDetails);
		return transaction(this.artifactProcessor.createDraftBundle(theId, fhirDal, version));
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
	private BundleEntryComponent createEntry(IBaseResource theResource) {
		return new Bundle.BundleEntryComponent()
				.setResource((Resource) theResource)
				.setRequest(createRequest(theResource));
	}
	private BundleEntryRequestComponent createRequest(IBaseResource theResource) {
		Bundle.BundleEntryRequestComponent request = new Bundle.BundleEntryRequestComponent();
		if (theResource.getIdElement().hasValue()) {
			request
					.setMethod(Bundle.HTTPVerb.PUT)
					.setUrl(theResource.getIdElement().getValue());
		} else {
			request
					.setMethod(Bundle.HTTPVerb.POST)
					.setUrl(theResource.fhirType());
		}

		return request;
	}
}
