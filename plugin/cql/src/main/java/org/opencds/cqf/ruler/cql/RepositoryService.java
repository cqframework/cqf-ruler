package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.cqframework.fhir.api.FhirDal;
import org.hl7.fhir.Uri;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.ContactDetail;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.openjdk.jmh.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

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

	/**
	 * Applies an approval to an existing artifact, regardless of status.
	 *
	 * @param requestDetails      the {@link RequestDetails RequestDetails}
	 * @param id						0..1 the {@link IdType IdType}, The logical id of the library to package.
*                            			The server must know the library (e.g. it is defined explicitly in the
	 *                            	server's libraries)
	 * @param url        			0..1	uri A canonical reference to a Library to package on the server.
	 *                            for an approval submission. If approvalDate is not
	 *                           	provided, the current date will be used.
	 * @param version					0..1	string The version of the Library.
	 * @param identifier				0..1	string(token) A business identifier of the Library.
	 * @param capability[]			0..*	string A desired capability of the resulting package. computable
	 *                              to include computable elements in packaged content; executable to
	 *                              include executable elements in packaged content; publishable to
	 *                              include publishable elements in packaged content.
	 * @param system-version[]		0..*	canonical Specifies a version to use for a system if the library
	 * 										or value set does not already specify which one to use. The format
	 * 										is the same as a canonical URL: [system]|[version] - e.g. http://loinc.org|2.56
	 * @param content-endpoint		0..* An endpoint for a service from which artifacts can be retrieved if they cannot
	 *               						be resolved on the local server.
	 * @return 							1..1 An IBaseResource that is the Bundle of collected/packaged resources
	 *
	 *
	 * IN	id		0..1	string
	 *
	 *
	 * IN	url		0..1	uri
	 * A canonical reference to a library. The server must know the library (e.g. it is defined explicitly in the server's libraries
	 *
	 * IN	version		0..1	string
	 * The version of the library to be used for packaging
	 *
	 * IN	identifier		0..1	string
	 * (token)
	 * A business identifier of the library to be packaged. The server must know the library and the identifier must resolve unambiguously to a single library on the server.
	 *
	 * IN	capability		0..*	string
	 * A desired capability of the resulting package. computable to include computable elements in packaged content, executable to include executable elements in packaged content, publishable to include publishable elements in packaged content.
	 *
	 * IN	offset		0..1	integer
	 * Paging support - where to start if a subset is desired (default = 0). Offset is number of records (not number of pages)
	 *
	 * IN	count		0..1	integer
	 * Paging support - how many resources should be provided in a partial page view. If count = 0, the client is asking how large the package is.
	 *
	 * IN	system-version		0..*	canonical
	 * Specifies a version to use for a system, if the library or value set does not already specify which one to use. The format is the same as a canonical URL: [system]|[version] - e.g. http://loinc.org|2.56
	 *
	 * IN	check-system-version		0..*	canonical
	 * Edge Case: Specifies a version to use for a system. If a library or value set specifies a different version, an error is returned instead of the package. The format is the same as a canonical URL: [system]|[version] - e.g. http://loinc.org|2.56
	 *
	 * IN	force-system-version		0..*	canonical
	 * Edge Case: Specifies a version to use for a system. This parameter overrides any specified version in the library and value sets (and any it depends on). The format is the same as a canonical URL: [system]|[version] - e.g. http://loinc.org|2.56. Note that this has obvious safety issues, in that it may result in a value set expansion giving a different list of codes that is both wrong and unsafe, and implementers should only use this capability reluctantly. It primarily exists to deal with situations where specifications have fallen into decay as time passes. If the value is override, the version used SHALL explicitly be represented in the expansion parameters
	 *
	 * IN	manifest		0..1	canonical
	 * Specifies an asset-collection library that defines version bindings for code systems referenced by the value set(s) being expanded. When specified, code systems identified as depends-on related artifacts in the library have the same meaning as specifying that code system version in the system-version parameter.
	 *
	 * IN	include-dependencies		0..1	boolean
	 * Specifies whether to include known (i.e. present on the server) dependencies of the library in the resulting package, recursively (default = true)
	 *
	 * IN	include-components		0..1	boolean
	 * Specifies whether to include known (i.e. present on the server) components of the library in the resulting package, recursively (default = true)
	 *
	 * IN	content-endpoint		0..1	Endoint
	 * An endpoint to use to access content (i.e. libraries) referenced by the PlanDefinition. If no content endpoint is supplied, the evaluation will attempt to retrieve content from the server on which the operation is being performed.
	 *
	 * IN	terminology-endpoint		0..1	Endpoint
	 * An endpoint to use to access terminology (i.e. valuesets, codesystems, and membership testing) referenced by the PlanDefinition. If no terminology endpoint is supplied, the evaluation will attempt to use the server on which the operation is being performed as the terminology server.
	 *
	 * OUT	return		1..1	Bundle
	 * The result of the packaging. Servers generating packages SHALL include all the dependency resources referenced by the library that are known to the server (if include-dependencies is true), and all the component resources referenced by the library that are known to the server (if include-components is true). For example, a measure repository SHALL include all the required library resources, but would not necessarily have the ValueSet resources referenced by the measure.
	 */
	@Operation(name = "$package", idempotent = true, global = true, type = MetadataResource.class)
	@Description(shortDefinition = "$package", value = "Package and existing bundle for release")
	public IBaseResource packageOperation(
		RequestDetails requestDetails,
		@IdParam IdType theId,
		@OperationParam(name = "url") List<CanonicalType> url,
		@OperationParam(name = "version") String version,
		@OperationParam(name = "identifier") String identifier,
		@OperationParam(name = "capability") List<String> capabilities,
		@OperationParam(name = "system-version") List<CanonicalType> systemVersions,
		@OperationParam(name = "content-endpoint") Endpoint contentEndpoint


		)
		throws FHIRException {
		FhirDal fhirDal = fhirDalFactory.create(requestDetails);
		return (IBaseResource) this.artifactProcessor.packageOperation(fhirDal, theId);
	}
}
