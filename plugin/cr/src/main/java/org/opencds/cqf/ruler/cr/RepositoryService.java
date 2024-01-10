package org.opencds.cqf.ruler.cr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.cqframework.fhir.api.FhirDal;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.ruler.cr.r4.ArtifactAssessment;
import org.opencds.cqf.ruler.cr.r4.CRMIReleaseExperimentalBehavior.CRMIReleaseExperimentalBehaviorCodes;
import org.opencds.cqf.ruler.cr.r4.CRMIReleaseVersionBehavior.CRMIReleaseVersionBehaviorCodes;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDaoValueSet;
import ca.uhn.fhir.jpa.validation.ValidatorResourceFetcher;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.validation.FhirValidator;

public class RepositoryService extends DaoRegistryOperationProvider {

	@Autowired
	private JpaFhirDalFactory fhirDalFactory;

	@Autowired
	private KnowledgeArtifactProcessor artifactProcessor;

	/**
	 * Applies an approval to an existing artifact, regardless of status.
	 *
	 * @param requestDetails      the {@link RequestDetails RequestDetails}
	 * @param theId					the {@link IdType IdType}, always an argument for instance level operations
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
			@OperationParam(name = "artifactCommentUser") Reference artifactCommentUser) throws UnprocessableEntityException {
				FhirDal fhirDal = this.fhirDalFactory.create(requestDetails);
				MetadataResource resource = (MetadataResource) fhirDal.read(theId);
		if (resource == null) {
			throw new ResourceNotFoundException(theId);
		}
		if (artifactCommentTarget != null) {
			if (Canonicals.getUrl(artifactCommentTarget) != null
			&& !Canonicals.getUrl(artifactCommentTarget).equals(resource.getUrl())) {
				throw new UnprocessableEntityException("ArtifactCommentTarget URL does not match URL of resource being approved.");
			}
			if (Canonicals.getVersion(artifactCommentTarget) != null
			&& !Canonicals.getVersion(artifactCommentTarget).equals(resource.getVersion())) {
				throw new UnprocessableEntityException("ArtifactCommentTarget version does not match version of resource being approved.");
			}
		} else if (artifactCommentTarget == null) {
			String target = "";
			String url = resource.getUrl();
			String version = resource.getVersion();
			if (url != null) {
				target += url;
			}
			if (version != null) {
				if (url != null) {
					target += "|";
				}
				target += version;
			}
			if (target != null) {
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
		MetadataResource approvedResource =  this.artifactProcessor.approve(resource, approvalDate, newAssessment);
		Bundle transactionBundle = new Bundle()
			.setType(Bundle.BundleType.TRANSACTION)
			.addEntry(createEntry(approvedResource));
		if (newAssessment != null && newAssessment.isValidArtifactComment()) {
			transactionBundle.addEntry(createEntry(newAssessment));
		}
		return transaction(transactionBundle, requestDetails);
	}
	/**
	 * Creates a draft of an existing artifact if it has status Active.
	 *
	 * @param requestDetails      the {@link RequestDetails RequestDetails}
	 * @param theId					the {@link IdType IdType}, always an argument for instance level operations
	 * @param version             new version in the form MAJOR.MINOR.PATCH
	 * @return A transaction bundle result of the newly created resources
	 */
	@Operation(name = "$draft", idempotent = true, global = true, type = MetadataResource.class)
	@Description(shortDefinition = "$draft", value = "Create a new draft version of the reference artifact")
	public Bundle draftOperation(RequestDetails requestDetails, @IdParam IdType theId, @OperationParam(name = "version") String version)
		throws FHIRException {
		FhirDal fhirDal = this.fhirDalFactory.create(requestDetails);
		return transaction(this.artifactProcessor.createDraftBundle(theId, fhirDal, version));
	}
	/**
	 * Sets the status of an existing artifact to Active if it has status Draft.
	 *
	 * @param requestDetails      the {@link RequestDetails RequestDetails}
	 * @param theId					      the {@link IdType IdType}, always an argument for instance level operations
	 * @param version             new version in the form MAJOR.MINOR.PATCH
	 * @param versionBehavior     how to handle differences between the user-provided and incumbernt versions
	 * @param latestFromTxServer  whether or not to query the TxServer if version information is missing from references
	 * @return A transaction bundle result of the updated resources
	 */
	@Operation(name = "$release", idempotent = true, global = true, type = MetadataResource.class)
	@Description(shortDefinition = "$release", value = "Release an existing draft artifact")
	public Bundle releaseOperation(
		RequestDetails requestDetails,
		@IdParam IdType theId,
		@OperationParam(name = "version") String version,
		@OperationParam(name = "versionBehavior") CodeType versionBehavior,
		@OperationParam(name = "latestFromTxServer", typeName = "Boolean") IPrimitiveType<Boolean> latestFromTxServer,
		@OperationParam(name = "requireNonExperimental") CodeType requireNonExpermimental,
		@OperationParam(name = "releaseLabel") String releaseLabel)
		throws FHIRException {
		CRMIReleaseVersionBehaviorCodes versionBehaviorCode;
		CRMIReleaseExperimentalBehaviorCodes experimentalBehaviorCode;
		try {
			versionBehaviorCode = versionBehavior == null ? CRMIReleaseVersionBehaviorCodes.NULL : CRMIReleaseVersionBehaviorCodes.fromCode(versionBehavior.getCode());
			experimentalBehaviorCode = requireNonExpermimental == null ? CRMIReleaseExperimentalBehaviorCodes.NULL : CRMIReleaseExperimentalBehaviorCodes.fromCode(requireNonExpermimental.getCode());
		} catch (FHIRException e) {
			throw new UnprocessableEntityException(e.getMessage());
		}
		FhirDal fhirDal = this.fhirDalFactory.create(requestDetails);
		return transaction(this.artifactProcessor.createReleaseBundle(
			theId, 
			releaseLabel, 
			version,
			versionBehaviorCode,
			latestFromTxServer != null && latestFromTxServer.getValue(),
			experimentalBehaviorCode,
			fhirDal));
	}

	@Operation(name = "$package", idempotent = true, global = true, type = MetadataResource.class)
	@Description(shortDefinition = "$package", value = "Package an artifact and components / dependencies")
	public Bundle packageOperation(
		RequestDetails requestDetails,
		@IdParam IdType theId,
		// TODO: $package - should capability be CodeType?
		@OperationParam(name = "capability") List<String> capability,
		@OperationParam(name = "artifactVersion") List<CanonicalType> artifactVersion,
		@OperationParam(name = "checkArtifactVersion") List<CanonicalType> checkArtifactVersion,
		@OperationParam(name = "forceArtifactVersion") List<CanonicalType> forceArtifactVersion,
		// TODO: $package - should include be CodeType?
		@OperationParam(name = "include") List<String> include,
		@OperationParam(name = "manifest") CanonicalType manifest,
		@OperationParam(name = "offset", typeName = "Integer") IPrimitiveType<Integer> offset,
		@OperationParam(name = "count", typeName = "Integer") IPrimitiveType<Integer> count,
		@OperationParam(name = "packageOnly", typeName = "Boolean") IPrimitiveType<Boolean> packageOnly,
		@OperationParam(name = "artifactEndpointConfiguration") ParametersParameterComponent artifactEndpointConfiguration,
		@OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint
		) throws FHIRException {
		FhirDal fhirDal = this.fhirDalFactory.create(requestDetails);
		List<ParametersParameterComponent> artifactEndpointParts = Optional.ofNullable(artifactEndpointConfiguration).map(config -> config.getPart()).orElse(new ArrayList<ParametersParameterComponent>());
		String artifactRoute = artifactEndpointParts.stream().filter(part -> part.getName().equals("artifactRoute")).map(part -> ((UriType)part.getValue()).getValue()).findAny().orElse(null);
		String endpointUri = artifactEndpointParts.stream().filter(part -> part.getName().equals("endpointUri")).map(part -> ((UriType)part.getValue()).getValue()).findAny().orElse(null);
		Endpoint artifactEndpoint = artifactEndpointParts.stream().filter(part -> part.getName().equals("endpoint")).map(part -> (Endpoint)part.getResource()).findAny().orElse(null);
		return this.artifactProcessor.createPackageBundle(
			theId,
			fhirDal,
			capability,
			include,
			artifactVersion,
			checkArtifactVersion,
			forceArtifactVersion,
			count != null ? count.getValue() : null,
			offset != null ? offset.getValue() : null,
			artifactRoute,
			endpointUri,
			artifactEndpoint,
			terminologyEndpoint,
			packageOnly != null ? packageOnly.getValue() : null
		);
	}

	@Operation(name = "$revise", idempotent = true, global = true, type = MetadataResource.class)
	@Description(shortDefinition = "$revise", value = "Update an existing artifact in 'draft' status")
	public IBaseResource reviseOperation(RequestDetails requestDetails, @OperationParam(name = "resource") IBaseResource resource)
		throws FHIRException {

		FhirDal fhirDal = fhirDalFactory.create(requestDetails);
		return (IBaseResource)this.artifactProcessor.revise(fhirDal, (MetadataResource) resource);
	}

	@Operation(name = "$validate", idempotent = true, global = true, type = MetadataResource.class)
	@Description(shortDefinition = "$validate", value = "Validate a bundle")
	public OperationOutcome validateOperation(RequestDetails requestDetails, 
		@OperationParam(name = "resource") IBaseResource resource,
		@OperationParam(name = "mode") CodeType mode,
		@OperationParam(name = "profile") String profile
	)
		throws FHIRException {
		if (mode != null) {
			throw new NotImplementedOperationException("'mode' Parameter not implemented yet.");
		}
		if (profile != null) {
			throw new NotImplementedOperationException("'profile' Parameter not implemented yet.");
		}
		if (resource == null) {
			throw new UnprocessableEntityException("A FHIR resource must be provided for validation");
		}
		FhirContext ctx = this.getFhirContext();
		if (ctx != null) {
			FhirValidator fhirValidator = ctx.newValidator();
			fhirValidator.setValidateAgainstStandardSchema(false);
			fhirValidator.setValidateAgainstStandardSchematron(false);
			NpmPackageValidationSupport npm = new NpmPackageValidationSupport(ctx);
			try {
				npm.loadPackageFromClasspath("classpath:hl7.fhir.us.ecr-2.1.0.tgz");
			} catch (IOException e) {
				throw new InternalErrorException("Could not load package");
			}
			ValidationSupportChain chain = new ValidationSupportChain(
				npm,
				new DefaultProfileValidationSupport(ctx),
				new InMemoryTerminologyServerValidationSupport(ctx),
				new CommonCodeSystemsTerminologyService(ctx)
			);
			FhirInstanceValidator instanceValidatorModule = new FhirInstanceValidator(chain);
			instanceValidatorModule.setValidatorResourceFetcher(new ValidatorResourceFetcher(ctx, chain, getDaoRegistry()));
			fhirValidator.registerValidatorModule(instanceValidatorModule);
			return (OperationOutcome) fhirValidator.validateWithResult(resource, null).toOperationOutcome();
		} else {
			throw new InternalErrorException("Could not load FHIR Context");
		}
	}

	@Operation(name = "$artifact-diff", idempotent = true, global = true, type = MetadataResource.class)
	@Description(shortDefinition = "$artifact-diff", value = "Diff two knowledge artifacts")
	public Parameters crmiArtifactDiff(RequestDetails requestDetails, 
		@OperationParam(name = "source") String source,
		@OperationParam(name = "target") String target,
		@OperationParam(name = "compareExecutable", typeName = "Boolean") IPrimitiveType<Boolean> compareExecutable,
		@OperationParam(name = "compareComputable", typeName = "Boolean") IPrimitiveType<Boolean> compareComputable
	) throws UnprocessableEntityException, ResourceNotFoundException{
		FhirDal fhirDal = fhirDalFactory.create(requestDetails);
		IBaseResource theSourceResource = fhirDal.read(new IdType(source));
		if (theSourceResource == null || !(theSourceResource instanceof MetadataResource)) {
			throw new UnprocessableEntityException("Source resource must exist and be a Knowledge Artifact type.");
		}
		IBaseResource theTargetResource = fhirDal.read(new IdType(target));
		if (theTargetResource == null || !(theTargetResource instanceof MetadataResource)) {
			throw new UnprocessableEntityException("Target resource must exist and be a Knowledge Artifact type.");
		}
		if (theSourceResource.getClass() != theTargetResource.getClass()) {
			throw new UnprocessableEntityException("Source and target resources must be of the same type.");
		}
		IFhirResourceDaoValueSet<ValueSet> dao = (IFhirResourceDaoValueSet<ValueSet>)this.getDaoRegistry().getResourceDao(ValueSet.class);
		return this.artifactProcessor.artifactDiff((MetadataResource)theSourceResource,(MetadataResource)theTargetResource,this.getFhirContext(),fhirDal,compareComputable == null ? false : compareComputable.getValue(), compareExecutable == null ? false : compareExecutable.getValue(),dao);
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
