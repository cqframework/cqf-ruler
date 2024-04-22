package org.opencds.cqf.ruler.casereporting.r4;


import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.PropertyUtilsBean;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Element;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.FilterOperator;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactApproveVisitor;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactDraftVisitor;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactPackageVisitor;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactReleaseVisitor;
import org.opencds.cqf.ruler.casereporting.IBaseSerializer;
import org.opencds.cqf.ruler.provider.HapiFhirRepositoryProvider;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.cr.repo.HapiFhirRepository;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDaoValueSet;
import ca.uhn.fhir.jpa.validation.ValidatorResourceFetcher;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.parser.path.EncodeContextPath;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class RepositoryService extends HapiFhirRepositoryProvider {

	@Autowired
	private KnowledgeArtifactProcessor artifactProcessor;

	private AdapterFactory adapterFactory = AdapterFactory.forFhirVersion(FhirVersionEnum.R4);

	/**
	 * Applies an approval to an existing artifact, regardless of status.
	 *
	 * @param requestDetails      the {@link RequestDetails RequestDetails}
	 * @param theId					the {@link IdType IdType}, always an argument for instance level operations
	 * @param approvalDate        Optional Date parameter for indicating the date of approval
	 *                            for an approval submission. If approvalDate is not
	 *                           	provided, the current date will be used.
	 * @param artifactAssessmentType
	 * @param artifactAssessmentSummary
	 * @param artifactAssessmentTarget
	 * @param artifactAssessmentRelatedArtifact
	 * @param artifactAssessmentAuthor Optional ArtifactComment* arguments represent parts of a
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
			@OperationParam(name = "artifactAssessmentType") String artifactAssessmentType,
			@OperationParam(name = "artifactAssessmentSummary") String artifactAssessmentSummary,
			@OperationParam(name = "artifactAssessmentTarget") CanonicalType artifactAssessmentTarget,
			@OperationParam(name = "artifactAssessmentRelatedArtifact") CanonicalType artifactAssessmentRelatedArtifact,
			@OperationParam(name = "artifactAssessmentAuthor") Reference artifactAssessmentAuthor) throws UnprocessableEntityException {
				var repository = this.getRepository(requestDetails);
				var resource = (MetadataResource)SearchHelper.readRepository(repository, theId);
		if (resource == null) {
			throw new ResourceNotFoundException(theId);
		}
		if (artifactAssessmentTarget != null) {
			if (Canonicals.getUrl(artifactAssessmentTarget) != null
			&& !Canonicals.getUrl(artifactAssessmentTarget).equals(resource.getUrl())) {
				throw new UnprocessableEntityException("ArtifactAssessmentTarget URL does not match URL of resource being approved.");
			}
			if (Canonicals.getVersion(artifactAssessmentTarget) != null
			&& !Canonicals.getVersion(artifactAssessmentTarget).equals(resource.getVersion())) {
				throw new UnprocessableEntityException("ArtifactAssessmentTarget version does not match version of resource being approved.");
			}
		} else if (artifactAssessmentTarget == null) {
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
				artifactAssessmentTarget = new CanonicalType(target);
			}
		}
		var params = new Parameters();
		if (approvalDate != null && approvalDate.hasValue()) {
			params.addParameter("approvalDate", new DateType(approvalDate.getValue()));
		}
		if (artifactAssessmentType != null) {
			params.addParameter("artifactAssessmentType", artifactAssessmentType);
		}
		if (artifactAssessmentTarget != null) {
			params.addParameter("artifactAssessmentTarget", artifactAssessmentTarget);
		}
		if (artifactAssessmentSummary != null) {
			params.addParameter("artifactAssessmentSummary", artifactAssessmentSummary);
		}
		if (artifactAssessmentRelatedArtifact != null) {
			params.addParameter("artifactAssessmentRelatedArtifact", artifactAssessmentRelatedArtifact);
		}
		if (artifactAssessmentAuthor != null) {
			params.addParameter("artifactAssessmentAuthor", artifactAssessmentAuthor);
		}
		var adapter = adapterFactory.createKnowledgeArtifactAdapter(resource);
		var visitor = new KnowledgeArtifactApproveVisitor();
		return((Bundle)adapter.accept(visitor, repository, params));
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
		var repository = this.getRepository(requestDetails);
		var resource = (MetadataResource)SearchHelper.readRepository(repository, theId);
		if (resource == null) {
			throw new ResourceNotFoundException(theId);
		}
		var params = new Parameters().addParameter("version", new StringType(version));
		var adapter = adapterFactory.createKnowledgeArtifactAdapter(resource);
		var visitor = new KnowledgeArtifactDraftVisitor();
		return((Bundle)adapter.accept(visitor, repository, params));
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
		@OperationParam(name = "requireNonExperimental") CodeType requireNonExperimental,
		@OperationParam(name = "releaseLabel") String releaseLabel)
		throws FHIRException {
		HapiFhirRepository repository = this.getRepository(requestDetails);
    var resource = (MetadataResource)SearchHelper.readRepository(repository, theId);
		if (resource == null) {
			throw new ResourceNotFoundException(theId);
		}
		var params = new Parameters();
		if (version != null) {
			params.addParameter("version", version);
		}
		if (versionBehavior != null ) {
			params.addParameter("versionBehavior", versionBehavior);
		}
		if (latestFromTxServer != null && latestFromTxServer.hasValue()) {
			params.addParameter("latestFromTxServer", latestFromTxServer.getValue());
		}
		if (requireNonExperimental != null) {
			params.addParameter("requireNonExperimental", requireNonExperimental);
		}
		if (releaseLabel != null ) {
			params.addParameter("releaseLabel", releaseLabel);
		}
		var adapter = adapterFactory.createKnowledgeArtifactAdapter(resource);
		try {
			var visitor = new KnowledgeArtifactReleaseVisitor();
			var retval = (Bundle)adapter.accept(visitor, repository, params);
			forEachMetadataResource(
				retval.getEntry(), 
				(r) -> {
					if (r != null) {
						adapterFactory.createKnowledgeArtifactAdapter(r).getRelatedArtifact()
						.forEach(ra -> {
							KnowledgeArtifactProcessor.checkIfValueSetNeedsCondition(null, (RelatedArtifact)ra, repository);
						});
					}
				}, 
				repository);
			return retval;
		} catch (Exception e) {
			throw new UnprocessableEntityException(e.getMessage());
		}
	}
	private void forEachMetadataResource(List<BundleEntryComponent> entries, Consumer<MetadataResource> callback, Repository repository) {
		entries.stream()
			.map(entry -> entry.getResponse().getLocation())
			.map(location -> {
				switch (location.split("/")[0]) {
					case "ActivityDefinition":
						return repository.read(ActivityDefinition.class, new IdType(location));
					case "Library":
						return repository.read(Library.class, new IdType(location));
					case "Measure":
						return repository.read(Measure.class, new IdType(location));
					case "PlanDefinition":
						return repository.read(PlanDefinition.class, new IdType(location));
					case "ValueSet":
						return repository.read(ValueSet.class, new IdType(location));
					default:
						return null;
				}
			})
			.forEach(callback);
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
		var repository = this.getRepository(requestDetails);
		var resource = (MetadataResource)SearchHelper.readRepository(repository, theId);
		if (resource == null) {
			throw new ResourceNotFoundException(theId);
		}
		var params = new Parameters();
		if (manifest != null) {
			params.addParameter("manifest", manifest);
		}
		if (artifactEndpointConfiguration != null) {
			params.addParameter().setName("artifactEndpointConfiguration").addPart(artifactEndpointConfiguration);
		}
		if (offset != null && offset.hasValue()) {
			params.addParameter("offset", new IntegerType(offset.getValue()));
		}
		if (offset != null && offset.hasValue()) {
			params.addParameter("offset", new IntegerType(offset.getValue()));
		}
		if (count != null && count.hasValue()) {
			params.addParameter("count", new IntegerType(count.getValue()));
		}
		if (packageOnly != null && packageOnly.hasValue()) {
			params.addParameter("packageOnly", packageOnly.getValue());
		}
		if (capability != null) {
			capability.forEach(c -> params.addParameter("capability", c));
		}
		if (artifactVersion != null) {
			artifactVersion.forEach(a -> params.addParameter("artifactVersion", a));
		}
		if (checkArtifactVersion != null) {
			checkArtifactVersion.forEach(c -> params.addParameter("checkArtifactVersion", c));
		}
		if (forceArtifactVersion != null) {
			forceArtifactVersion.forEach(f -> params.addParameter("forceArtifactVersion", f));
		}
		if (include != null) {
			include.forEach(i -> params.addParameter("include", i));
		}
		var adapter = adapterFactory.createKnowledgeArtifactAdapter(resource);
		var visitor = new KnowledgeArtifactPackageVisitor();
		var retval = (Bundle)adapter.accept(visitor, repository, params);
		retval.getEntry().stream()
			.map(e -> (MetadataResource)e.getResource())
			.filter(r -> {
				var id1 = r.getResourceType().toString() + "/" + r.getIdPart();
				var id2 = theId.getValue();
				return id1.equals(id2);
			})
			.findFirst()
			.ifPresent(m -> {
				KnowledgeArtifactProcessor.handleValueSetReferenceExtensions(m, retval.getEntry(), repository);
			});
			return retval;
	}

	@Operation(name = "$revise", idempotent = true, global = true, type = MetadataResource.class)
	@Description(shortDefinition = "$revise", value = "Update an existing artifact in 'draft' status")
	public IBaseResource reviseOperation(RequestDetails requestDetails, @OperationParam(name = "resource") IBaseResource resource)
		throws FHIRException {
		var repository = this.getRepository(requestDetails);
		return (IBaseResource)this.artifactProcessor.revise(repository, (MetadataResource) resource);
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
		var ctx = this.getFhirContext();
		if (ctx != null) {
			var fhirValidator = ctx.newValidator();
			fhirValidator.setValidateAgainstStandardSchema(false);
			fhirValidator.setValidateAgainstStandardSchematron(false);
			var npm = new NpmPackageValidationSupport(ctx);
			try {
				npm.loadPackageFromClasspath("classpath:hl7.fhir.us.ecr-2.1.0.tgz");
			} catch (IOException e) {
				throw new InternalErrorException("Could not load package");
			}
			var chain = new ValidationSupportChain(
				npm,
				new DefaultProfileValidationSupport(ctx),
				new InMemoryTerminologyServerValidationSupport(ctx),
				new CommonCodeSystemsTerminologyService(ctx)
			);
			var instanceValidatorModule = new FhirInstanceValidator(chain);
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
		var repository = this.getRepository(requestDetails);
		var sourceId = new IdType(source);
		var theSourceResource = SearchHelper.readRepository(repository,sourceId);
		if (theSourceResource == null || !(theSourceResource instanceof MetadataResource)) {
			throw new UnprocessableEntityException("Source resource must exist and be a Knowledge Artifact type.");
		}
		var targetId = new IdType(target);
		var theTargetResource = SearchHelper.readRepository(repository,targetId);
		if (theTargetResource == null || !(theTargetResource instanceof MetadataResource)) {
			throw new UnprocessableEntityException("Target resource must exist and be a Knowledge Artifact type.");
		}
		if (theSourceResource.getClass() != theTargetResource.getClass()) {
			throw new UnprocessableEntityException("Source and target resources must be of the same type.");
		}
		var dao = (IFhirResourceDaoValueSet<ValueSet>)this.getDaoRegistry().getResourceDao(ValueSet.class);
		return this.artifactProcessor.artifactDiff((MetadataResource)theSourceResource, (MetadataResource)theTargetResource, this.getFhirContext(), repository, compareComputable == null ? false : compareComputable.getValue(), compareExecutable == null ? false : compareExecutable.getValue(), dao, null);
	}
	@Operation(name = "$create-changelog", idempotent = true, global = true, type = MetadataResource.class)
	@Description(shortDefinition = "$create-changelog", value = "Create a changelog object which can be easily rendered into a table")
	public IBaseResource flattenDiffParametersToChangeLogJSON(RequestDetails requestDetails, 
		@OperationParam(name = "source") String source,
		@OperationParam(name = "target") String target) {
			// 1) Create Diff Parameters Object as input
			var cache = new KnowledgeArtifactProcessor.diffCache();
			var dao = (IFhirResourceDaoValueSet<ValueSet>)this.getDaoRegistry().getResourceDao(ValueSet.class);
			var repository = this.getRepository(requestDetails);
			var sourceId = new IdType(source);
			var theSourceResource = (MetadataResource) SearchHelper.readRepository(repository,sourceId);
			if (theSourceResource == null || !(theSourceResource instanceof Library)) {
				throw new UnprocessableEntityException("Source resource must exist and be a Library.");
			}
			var targetId = new IdType(target);
			var theTargetResource = (MetadataResource) SearchHelper.readRepository(repository,targetId);
			if (theTargetResource == null || !(theTargetResource instanceof Library)) {
				throw new UnprocessableEntityException("Target resource must exist and be a Libary.");
			}
			var targetAdapter = AdapterFactory.forFhirVersion(FhirVersionEnum.R4).createKnowledgeArtifactAdapter(theTargetResource);
			var diffParameters = this.artifactProcessor.artifactDiff(theSourceResource, theTargetResource, this.getFhirContext(), repository, true, true, dao, cache);
			var manifestUrl = targetAdapter.getUrl();
			var changelog = new ChangeLog(manifestUrl);

			// 2) Recursively process the Parameters into a flat ChangeLog
			processChanges(diffParameters.getParameter(), changelog, cache, manifestUrl);

			// 3) Handle the Conditions and Priorities which are in RelatedArtifact changes
			var relatedArtifactOperations = diffParameters.getParameter().stream()
			.filter(p -> p.getName().equals("operation"))
			.filter(p -> {
				var path = getPathParameterNoBase(p);
				return path.isPresent() && path.get().contains("relatedArtifact[");
			}).collect(Collectors.toList());
			handleRelatedArtifacts((Library)theSourceResource,(Library)theTargetResource, changelog, relatedArtifactOperations, cache);
			
			// 4) Generate the output JSON
			var bin = new Binary();
			var mapper = new ObjectMapper()
				.setSerializationInclusion(JsonInclude.Include.NON_NULL)
				.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
      SimpleModule module = new SimpleModule("IBaseSerializer", new Version(1, 0, 0, null, null, null));
      module.addSerializer(IBase.class, new IBaseSerializer(this.getFhirContext()));
      mapper.registerModule(module);
			try {
				var jsonString = mapper.writeValueAsString(changelog);
				bin.setContent(jsonString.getBytes(Charset.forName("UTF-8")));
			} catch (JsonProcessingException e) {
				// TODO: handle exception
				bin.setContent(e.getMessage().getBytes(Charset.forName("UTF-8")));
			}
			
			return bin;
		}
		private void handleRelatedArtifacts(Library source, Library target, ChangeLog changeLog, List<ParametersParameterComponent> parameters, KnowledgeArtifactProcessor.diffCache cache) {
			// need to modify the diff function to change the order of the relatedartifacts and things
			// in the diff cache resources so that the parameters paths make sense

			// orrrrr
			// just modify the diff function to also append the relatedArtifact target URL to all the parameters?
			for(var change: parameters) {
				if (change.getName().equals("operation")) {
						var type = getStringParameter(change, "type")
							.orElseThrow(() -> new UnprocessableEntityException("Type must be provided when adding an operation to the ChangeLog"));
						var maybePath = getPathParameterNoBase(change);
						if (maybePath.isPresent() 
						&& maybePath.get().contains("elatedArtifact")) {
							var path = maybePath.get();
							var originalValue = getParameter(change, "previousValue").map(o -> (Object)o);
							var newValue = getParameter(change, "value").map(o->(Object)o);
							try {
								if (originalValue.isEmpty()) {
									originalValue = Optional.ofNullable((new PropertyUtilsBean()).getProperty(source, path));
								}
							} catch (Exception e) {
								// TODO: handle exception
								var message = e.getMessage();
							}
							String relatedArtifactTargetCanonical = null;
							List<Extension> newConditions = new ArrayList<>();
							List<Extension> oldConditions = new ArrayList<>();
							List<Extension> newPriority = new ArrayList<>();
							List<Extension> oldPriority = new ArrayList<>();
							if (newValue.isPresent()) {
								if (newValue.get() instanceof RelatedArtifact) {
									relatedArtifactTargetCanonical = ((RelatedArtifact) newValue.get()).getResource();
									newConditions = ((RelatedArtifact) newValue.get()).getExtensionsByUrl(KnowledgeArtifactProcessor.valueSetConditionUrl);
									newPriority = ((RelatedArtifact) newValue.get()).getExtensionsByUrl(KnowledgeArtifactProcessor.valueSetPriorityUrl);
								} else if (newValue.get() instanceof IPrimitiveType 
								&& path.contains("[")) {
									var matcher = Pattern
										.compile("relatedArtifact\\[(\\d+)\\]")
										.matcher(path);
										if (matcher.find()) {
											var pathToRelatedArtifact = matcher.group();
											try {
													newValue = Optional.ofNullable((new PropertyUtilsBean()).getProperty(target, pathToRelatedArtifact));
											} catch (Exception e) {
												// TODO: handle exception
												var message = e.getMessage();
											}
											if (newValue.isPresent() && newValue.get() instanceof RelatedArtifact) {
												relatedArtifactTargetCanonical = ((RelatedArtifact) newValue.get()).getResource();
												newConditions = ((RelatedArtifact) newValue.get()).getExtensionsByUrl(KnowledgeArtifactProcessor.valueSetConditionUrl);
												newPriority = ((RelatedArtifact) newValue.get()).getExtensionsByUrl(KnowledgeArtifactProcessor.valueSetPriorityUrl);
											}
										}
								}
							}
							if (originalValue.isPresent()) {
								if (originalValue.get() instanceof RelatedArtifact) {
									relatedArtifactTargetCanonical = ((RelatedArtifact) originalValue.get()).getResource();
									oldConditions = ((RelatedArtifact) originalValue.get()).getExtensionsByUrl(KnowledgeArtifactProcessor.valueSetConditionUrl);
									oldPriority = ((RelatedArtifact) originalValue.get()).getExtensionsByUrl(KnowledgeArtifactProcessor.valueSetPriorityUrl);
								} else if (originalValue.get() instanceof IPrimitiveType 
								&& path.contains("[")) {
									var matcher = Pattern
										.compile("relatedArtifact\\[(\\d+)\\]")
										.matcher(path);
										if (matcher.find()) {
											var pathToRelatedArtifact = matcher.group();
											try {
												originalValue = Optional.ofNullable((new PropertyUtilsBean()).getProperty(source, pathToRelatedArtifact));
											} catch (Exception e) {
												// TODO: handle exception
												var message = e.getMessage();
											}
											if (originalValue.isPresent() && originalValue.get() instanceof RelatedArtifact) {
												relatedArtifactTargetCanonical = ((RelatedArtifact) originalValue.get()).getResource();
												oldConditions = ((RelatedArtifact) originalValue.get()).getExtensionsByUrl(KnowledgeArtifactProcessor.valueSetConditionUrl);
												oldPriority = ((RelatedArtifact) originalValue.get()).getExtensionsByUrl(KnowledgeArtifactProcessor.valueSetPriorityUrl);
											}
										}
								}
							}
							if (relatedArtifactTargetCanonical != null) {
								final var finalCanonical = relatedArtifactTargetCanonical;
								final var finalOriginal = originalValue;
								final var finalNew = newValue;
								var page = changeLog.pages.stream()
									.filter(p -> (p.oldData instanceof ChangeLog.ValueSetChild)).map(p -> (ChangeLog.Page<ChangeLog.ValueSetChild>)p)
									.filter(p -> p.oldData.codes.stream().anyMatch(c -> c.memberOid != null && c.memberOid.equals(Canonicals.getIdPart(finalCanonical))) 
									|| p.newData.codes.stream().anyMatch(c -> c.memberOid != null && c.memberOid.equals(Canonicals.getUrl(finalCanonical))))
									.findAny();
								if (page.isPresent()) {
									var page2 = page.get();
										if (page2.oldData != null && page2.oldData instanceof ChangeLog.ValueSetChild) {
											if (type.equals("delete" ) || type.equals("replace")) {
												var isConditionInOld = ((ChangeLog.ValueSetChild)page2.oldData).codes.stream().anyMatch(c -> c.memberOid != null && c.memberOid.equals(Canonicals.getIdPart(finalCanonical)));
													if (isConditionInOld) {
														for (var condition: oldConditions) {
															var val = ((CodeableConcept)condition.getValue()).getCodingFirstRep();
															((ChangeLog.ValueSetChild)page2.oldData).grouperList.stream()
															.filter(g -> g.memberOid.equals(finalCanonical)).findAny()
															.ifPresent(g -> {
																g.conditions.add(new ChangeLog.ValueSetChild.Code(
																	val.getId(), 
																	val.getSystem(), 
																	val.getCode(), 
																	val.getVersion(), 
																	val.getDisplay(), 
																	null, 
																	new ChangeLog.ChangeLogOperation(type, path, finalNew.orElse(null), finalOriginal.orElse(null))));
															});
														}
														if (oldPriority.size() > 1) {
															throw new UnprocessableEntityException("too many priorities");
														} else if(oldPriority.size() > 0) {
																var val = ((CodeableConcept)oldPriority.get(0).getValue()).getCodingFirstRep();
															((ChangeLog.ValueSetChild)page2.oldData).grouperList.stream()
															.filter(g -> g.memberOid.equals(finalCanonical)).findAny()
															.ifPresent(g -> {
																g.priority.value = val.getCode();
																g.priority.operation = new ChangeLog.ChangeLogOperation(type, path, finalNew.orElse(null), finalOriginal.orElse(null));
															});
														}
											} else {
												// throw new UnprocessableEntityException("whaa");
												var whaaa = "whaa";
											}
											}
											if (type.equals("insert") || type.equals("replace")) {
												var isConditionInNew = ((ChangeLog.ValueSetChild)page2.newData).codes.stream().anyMatch(c -> c.memberOid != null && c.memberOid.equals(Canonicals.getIdPart(finalCanonical)));
												if (isConditionInNew) {
													for (var condition: newConditions) {
														var val = ((CodeableConcept)condition.getValue()).getCodingFirstRep();
														((ChangeLog.ValueSetChild)page2.newData).grouperList.stream().filter(g -> g.memberOid.equals(finalCanonical)).findAny()
														.ifPresent(g -> {
															g.conditions.add(new ChangeLog.ValueSetChild.Code(
																val.getId(), 
																val.getSystem(), 
																val.getCode(), 
																val.getVersion(), 
																val.getDisplay(), 
																null, 
																new ChangeLog.ChangeLogOperation(type, path, finalNew.orElse(null), finalOriginal.orElse(null))));
														});												
													}
													if (newPriority.size() > 1) {
														throw new UnprocessableEntityException("too many priorities");
													} else if(newPriority.size() > 0) {
															var val = ((CodeableConcept)newPriority.get(0).getValue()).getCodingFirstRep();
														((ChangeLog.ValueSetChild)page2.newData).grouperList.stream()
														.filter(g -> g.memberOid.equals(finalCanonical)).findAny()
														.ifPresent(g -> {
															g.priority.value = val.getCode();
															g.priority.operation = new ChangeLog.ChangeLogOperation(type, path, finalNew.orElse(null), finalOriginal.orElse(null));
														});
													}
												} else {
													var whaa = "whaaa";
												}
											}
										}
								}
							}
						}
				}
			}
		}
		private void processChanges(List<ParametersParameterComponent> changes, ChangeLog changelog, KnowledgeArtifactProcessor.diffCache cache, String url) {
			// 1) Get the source and target resources so we can pull additional info as necessary
			var resources = cache.getResourcesForUrl(url);
			var resourceType = Canonicals.getResourceType(url);
			// Check if the resource pair was already processed
			var wasPageAlreadyProcessed = changelog.getPage(url).isPresent();
			if(!resources.isEmpty() && !wasPageAlreadyProcessed) {
				final MetadataResource sourceResource = resources.get(0).isSource ? resources.get(0).resource : (resources.size() > 1 ? resources.get(1).resource : null);
				final MetadataResource targetResource = resources.get(0).isSource ? (resources.size() > 1 ? resources.get(1).resource : null) : resources.get(0).resource;
				
				// 2) Generate a page for each resource pair based on ResourceType
				var page = changelog.getPage(url).orElseGet(() -> {
					switch (resourceType) {
						case "ValueSet":
							return changelog.addPage((ValueSet)sourceResource, (ValueSet)targetResource, cache);
						case "Library":
							return changelog.addPage((Library)sourceResource, (Library)targetResource);
						case "PlanDefinition":
							return changelog.addPage((PlanDefinition)sourceResource, (PlanDefinition)targetResource);					
						default:
							throw new UnprocessableEntityException("Unknown resource type: " + resourceType);
					}
				});
				for (var change: changes) {
					if (change.hasName() && !change.getName().equals("operation")
					&& change.hasResource() && change.getResource() instanceof Parameters) {
						// Nested Parameters objects get recursively processed
						processChanges(((Parameters)change.getResource()).getParameter(), changelog, cache, change.getName());						
					} else if (change.getName().equals("operation")) {
						// 3) For each operation get the relevant parameters
						var type = getStringParameter(change, "type")
							.orElseThrow(() -> new UnprocessableEntityException("Type must be provided when adding an operation to the ChangeLog"));
						var newValue = getParameter(change, "value");
						var path = getPathParameterNoBase(change);
						var originalValue = getParameter(change, "previousValue").map(o -> (Object)o);
						// try to extract the original value from the
						// source object if not present in the Diff 
						// Parameters object
						try {
							if (originalValue.isEmpty()) {
								originalValue = Optional.ofNullable((new PropertyUtilsBean()).getProperty(sourceResource, path.get()));
							}
						} catch (Exception e) {
							// TODO: handle exception
							// var message = e.getMessage();
							throw new InternalErrorException("Could not process path: " + path + ": " + e.getMessage());
						}

						// 4) Add a new operation to the ChangeLog 
						page.addOperation(type, path.orElse(null), newValue.orElse(null), originalValue.orElse(null), changelog);
					} else {
						// 5) Ignore the changelog entries for deleted or not owned entries
						var thing = change;
						var name = change.getName();
						var hasResource = change.hasResource();
						var getResource = change.getResource();
						var instanceofparam = change.getResource() instanceof Parameters;
					}
				}
			}
		}
		private Optional<String> getPathParameterNoBase(ParametersParameterComponent change) {
			return getStringParameter(change, "path").map(p -> {
				var e = new EncodeContextPath(p);
				var removeBase = removeBase(e);
				return removeBase;
			});
		}
		private String removeBase(EncodeContextPath path) {
			return path.getPath().subList(1, path.getPath().size())
			.stream()
			.map(t -> t.toString())
			.collect(Collectors.joining("."));
		}
		private Optional<String> getStringParameter(ParametersParameterComponent part, String name) {
			return part.getPart().stream()
			.filter(p -> p.getName().equalsIgnoreCase(name))
			.filter(p -> p.getValue() instanceof IPrimitiveType)
			.map(p -> (IPrimitiveType)p.getValue())
			.map(s -> (String)s.getValue())
			.findAny();
		}
		private Optional<IBase> getParameter(ParametersParameterComponent part, String name) {
			return part.getPart().stream()
			.filter(p -> p.getName().equalsIgnoreCase(name))
			.filter(p -> p.hasValue())
			.map(p -> (IBase)p.getValue())
			.findAny();
		}
}
