package org.hl7.davinci.atr.server.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hl7.davinci.atr.server.util.FhirUtil;
import org.hl7.davinci.atr.server.util.TextConstants;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Group.GroupMemberComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

/**
 * The Class MALService.
 */
@Service
public class MALService {

	/** The patient dao. */
	@Autowired
	private IFhirResourceDao<Patient> patientDao;

	/** The coverage dao. */
	@Autowired
	private IFhirResourceDao<Coverage> coverageDao;

	/** The practitioner dao. */
	@Autowired
	private IFhirResourceDao<Practitioner> practitionerDao;

	/** The practitioner role dao. */
	@Autowired
	private IFhirResourceDao<PractitionerRole> practitionerRoleDao;

	/** The organization dao. */
	@Autowired
	private IFhirResourceDao<Organization> organizationDao;

	/** The group dao. */
	@Autowired
	private IFhirResourceDao<Group> groupDao;

	/** The fhir context. */
	@Autowired
	FhirContext fhirContext;

	/** The Constant logger. */
	private static final Logger logger = LoggerFactory.getLogger(MALService.class);

	/**
	 * Process add member to group.
	 *
	 * @param theParameters  the the parameters
	 * @param theId          the the id
	 * @param requestDetails the request details
	 * @return the group
	 */
	public Group processAddMemberToGroup(Parameters theParameters, IdType theId, RequestDetails requestDetails) {

		logger.info("In ProcessAdd member to Group Method");
		logger.info("RequestURL:::::{}", requestDetails.getFhirServerBase());
		String patientId = null;
		String attributeProviderId = null;
		String attributeProviderReferenceResource = null;
		String coverageId = null;
		Period attributionPeriod = null;

		Group group = groupDao.read(theId);
		if (theParameters.getParameter("memberId") != null && theParameters.getParameter("providerNpi") != null) {
			// Creating the TokenAndListParam object to add into the SearchParamMap
			SearchParameterMap patientParamMap = new SearchParameterMap();
			TokenAndListParam tokenParam = FhirUtil.createTokenAndListParam(theParameters, "memberId");
			patientParamMap.add(Patient.SP_IDENTIFIER, tokenParam);
			// Invoking the Patient Search Dao API.
			IBundleProvider bundle = patientDao.search(patientParamMap);
			logger.info("Received Bundle with Size:::::{}", bundle.getAllResources().size());
			for (IBaseResource iBaseResource : bundle.getAllResources()) {
				Resource resource = (Resource) iBaseResource;
				if (resource.fhirType().equals("Patient")) {
					logger.info("patientId::::::" + resource.getIdElement().getIdPart());
					patientId = resource.getIdElement().getIdPart();
				}
			}

			// Get Practitioner Id using the NPI details from received Parameters.
			// Creating the TokenAndListParam object to add into the SearchParamMap
			Map<String, String> providerMap = findProviderIdByIdentifier(theParameters);
			if (providerMap != null && !providerMap.isEmpty()) {
				for (Map.Entry<String, String> entry : providerMap.entrySet()) {
					attributeProviderId = entry.getValue();
					attributeProviderReferenceResource = entry.getKey();
				}
			}

			// Get Coverage Id using the MemberId details from received Parameters.
			// Creating the TokenAndListParam object to add into the SearchParamMap
			SearchParameterMap coverageParamMap = new SearchParameterMap();
			coverageParamMap.add(Coverage.SP_IDENTIFIER, tokenParam);
			// Invoking the Patient Search Dao API.
			IBundleProvider coverageBundle = coverageDao.search(coverageParamMap);
			logger.info("Received Bundle with Size:::::{}", coverageBundle.getAllResources().size());
			for (IBaseResource iBaseResource : coverageBundle.getAllResources()) {
				Resource resource = (Resource) iBaseResource;
				if (resource.fhirType().equals("Coverage")) {
					logger.info("coverageId::::::{}", resource.getIdElement().getIdPart());
					coverageId = resource.getIdElement().getIdPart();
				}
			}

			if (theParameters.getParameter("attributionPeriod") != null) {
				attributionPeriod = (Period) theParameters.getParameter("attributionPeriod");
			}
			
		} else if (theParameters.getParameter(TextConstants.PATIENT_REFERENCE) != null
				&& theParameters.getParameter(TextConstants.PROVIDER_REFERENCE) != null) {
			String patientMemberId = findPatientIdByReference(theParameters);
			if (StringUtils.isNotBlank(patientMemberId)) {
				patientId = patientMemberId;
				Map<String, String> providerMap = findProviderIdByReference(theParameters);
				if (providerMap != null && !providerMap.isEmpty()) {
					for (Map.Entry<String, String> entry : providerMap.entrySet()) {
						attributeProviderId = entry.getValue();
						attributeProviderReferenceResource = entry.getKey();
					}
				} else {
					throw new ResourceNotFoundException("Couldn't find any Providers with given providerReference");
				}
				String coverageResourceId = findCoverageIdByPatientId(patientId);
				if (StringUtils.isNotBlank(coverageResourceId)) {
					coverageId = coverageResourceId;
				}
			} else {
				throw new ResourceNotFoundException("Couldn't find any Patient with given patientReference");
			}
		} else {
			throw new UnprocessableEntityException(
					"Please provide memberId + providerNpi or patientReference + providerReference to $member-add.");
		}
		
		if (patientId != null && attributeProviderId != null) {
			logger.info(" patientMemberId :: " + patientId);
			logger.info(" attributePeriod :: " + attributionPeriod);
			logger.info(" attributeProviderReferenceResource :: " + attributeProviderReferenceResource);
			logger.info(" attributeProviderId :: " + attributeProviderId);
			logger.info(" coverageReference :: " + coverageId);
			if (attributionPeriod != null) {
				logger.info(" attributionPeriod.getStart() :: " + attributionPeriod.getStart());
				logger.info(" attributionPeriod.getEnd() :: " + attributionPeriod.getEnd());
			}
			addMemberToGroup(group, patientId, attributeProviderId, attributeProviderReferenceResource, coverageId,
					attributionPeriod);
			logger.info("After adding Member::::{}", fhirContext.newJsonParser().encodeResourceToString(group));
			groupDao.update(group);
			if (group == null) {
				throw new UnprocessableEntityException("Error while adding member to group");
			}
		} else {
			throw new ResourceNotFoundException(
					"No Patient or Provider found. Please provide valid Patient/Provider");
		}
		return group;
	}

	/**
	 * Find coverage id by patient id.
	 *
	 * @param id the id
	 * @return the string
	 */
	private String findCoverageIdByPatientId(String id) {
		String coverageId = null;
		Coverage coverage = coverageDao.read(new IdType(id));
		if (coverage != null) {
			coverageId = coverage.getIdElement().getIdPart();
		}
		return coverageId;
	}

	/**
	 * Find patient id by reference.
	 *
	 * @param theParameters the the parameters
	 * @return the string
	 */
	private String findPatientIdByReference(Parameters theParameters) {
		String patientId = null;
		Reference patientReference = (Reference) theParameters.getParameter(TextConstants.PATIENT_REFERENCE);
		System.out.println(" patientReference.getReferenceElement().getIdPart() "
				+ patientReference.getReferenceElement().getIdPart());
		System.out.println(" patientReference.getReference() " + patientReference.getReference());

		Patient patient = patientDao.read(patientReference.getReferenceElement());
		if (patient != null) {
			patientId = patient.getIdElement().getIdPart();
		}
		return patientId;
	}

	/**
	 * Find provider id by reference.
	 *
	 * @param theParameters the the parameters
	 * @return the map
	 */
	private Map<String, String> findProviderIdByReference(Parameters theParameters) {
		Map<String, String> providerMap = new HashMap<>();
		Reference providerReference = (Reference) theParameters.getParameter(TextConstants.PROVIDER_REFERENCE);
		String providerReferenceResource = providerReference.getReferenceElement().getResourceType();
		if (StringUtils.isNotBlank(providerReferenceResource)
				&& providerReferenceResource.equalsIgnoreCase("Practitioner")) {
			Practitioner practitioner = practitionerDao.read(providerReference.getReferenceElement());
			if (practitioner != null && !practitioner.isEmpty()) {
				providerMap.put("Practitioner", practitioner.getIdElement().getIdPart());
			}
		} else if (StringUtils.isNotBlank(providerReferenceResource)
				&& providerReferenceResource.equalsIgnoreCase("PractitionerRole")) {
			PractitionerRole practitionerRole = practitionerRoleDao.read(providerReference.getReferenceElement());
			if (practitionerRole != null && !practitionerRole.isEmpty()) {
				providerMap.put("PractitionerRole", practitionerRole.getIdElement().getIdPart());
			}
		} else if (StringUtils.isNotBlank(providerReferenceResource)
				&& providerReferenceResource.equalsIgnoreCase("Organization")) {
			Organization organization = organizationDao.read(providerReference.getReferenceElement());
			if (organization != null && !organization.isEmpty()) {
				providerMap.put("Organization", organization.getIdElement().getIdPart());
			}
		}
		return providerMap;
	}

	/**
	 * Find provider id by identifier.
	 *
	 * @param theParameters the the parameters
	 * @return the map
	 */
	private Map<String, String> findProviderIdByIdentifier(Parameters theParameters) {
		Map<String, String> providerMap = new HashMap<>();
		SearchParameterMap attrProviderParamMap = new SearchParameterMap();
		TokenAndListParam pracitionerTokenParam = FhirUtil.createTokenAndListParam(theParameters, "providerNpi");
		attrProviderParamMap.add("identifier", pracitionerTokenParam);
		IBundleProvider practitionerBundle = practitionerDao.search(attrProviderParamMap);
		if (practitionerBundle.getAllResources().isEmpty()) {
			IBundleProvider practitionerRoleBundle = practitionerRoleDao.search(attrProviderParamMap);
			if (practitionerRoleBundle.getAllResources().isEmpty()) {
				IBundleProvider organizationBundle = organizationDao.search(attrProviderParamMap);
				if (!organizationBundle.isEmpty()) {
					providerMap = addToMap(organizationBundle, providerMap);
				}
			} else {
				providerMap = addToMap(practitionerRoleBundle, providerMap);
			}
		} else {
			providerMap = addToMap(practitionerBundle, providerMap);
		}
		return providerMap;
	}

	/**
	 * Adds the to map.
	 *
	 * @param bundle      the bundle
	 * @param providerMap the provider map
	 * @return the map
	 */
	private Map<String, String> addToMap(IBundleProvider bundle, Map<String, String> providerMap) {
		for (IBaseResource iBaseResource : bundle.getAllResources()) {
			Resource resource = (Resource) iBaseResource;
			if (resource.fhirType().equals("Organization")) {
				providerMap.put("Organization", resource.getIdElement().getIdPart());
			}
			if (resource.fhirType().equals("Practitioner")) {
				providerMap.put("Practitioner", resource.getIdElement().getIdPart());
			}
			if (resource.fhirType().equals("PractitionerRole")) {
				providerMap.put("PractitionerRole", resource.getIdElement().getIdPart());
			}
		}
		return providerMap;
	}

	/**
	 * Adds the member to group.
	 *
	 * @param group                    the group
	 * @param patientMemberId          the patient member id
	 * @param providerId               the provider id
	 * @param attrProviderResourceName the attr provider resource name
	 * @param coverageId               the coverage id
	 * @param attributionPeriod        the attribution period
	 */
	private void addMemberToGroup(Group group, String patientMemberId, String providerId,
			String attrProviderResourceName, String coverageId, Period attributionPeriod) {
		try {
			List<GroupMemberComponent> memberList = new ArrayList<>();
			boolean isAttributionCoverageFound = false;
			boolean isMemberFound = false;
			if (group.hasMember()) {
				memberList = group.getMember();
				for (GroupMemberComponent memberGroup : new ArrayList<GroupMemberComponent>(memberList)) {
					// GroupMemberComponent memberGroup = iterator.next();
					String entityId = getEntityIdFromGroupMemberComponent(memberGroup);
					String attributeProviderId = getAttributeProviderIdFromGroupMemberComponent(memberGroup);
					if (entityId != null && attributeProviderId != null) {
						if (patientMemberId.equalsIgnoreCase(entityId)
								&& providerId.equalsIgnoreCase(attributeProviderId)) {
							isMemberFound = true;
							if (coverageId != null) {
								isAttributionCoverageFound = updateGroupMemberComponentCoverageReferenceExtension(
										memberGroup, coverageId, isAttributionCoverageFound);
							}
							if (attributionPeriod != null) {
								updateGroupMemberComponentAttributionPeriod(memberGroup, isAttributionCoverageFound,
										attributionPeriod);
							}
						}
					}
				}
				if (!isMemberFound) {
					GroupMemberComponent theGroupMemberComponent = FhirUtil.getGroupMemberComponent(patientMemberId,
							providerId, attrProviderResourceName, coverageId, attributionPeriod);
					if (theGroupMemberComponent != null) {
						memberList.add(theGroupMemberComponent);
						logger.info(" :: Adding one new GroupMemberComponent :: ");
						group.setMember(memberList);
					}
				}
			} else {
				List<GroupMemberComponent> newGroupMemberComponentList = null;
				GroupMemberComponent newGroupMemberComponent = FhirUtil.getGroupMemberComponent(patientMemberId,
						providerId, attrProviderResourceName, coverageId, attributionPeriod);
				if (newGroupMemberComponent != null && !newGroupMemberComponent.isEmpty()) {
					newGroupMemberComponentList = new ArrayList<>();
					newGroupMemberComponentList.add(newGroupMemberComponent);
					logger.info(" :: Adding new Member for first time for group :: ");
					group.setMember(newGroupMemberComponentList);
				}
			}
			if (group.hasMeta()) {
				if (group.getMeta().hasVersionId()) {
					String versionId = group.getMeta().getVersionId();
					int version = Integer.parseInt(versionId);
					version = version + 1;
					group.getMeta().setVersionId(String.valueOf(version));

				} else {
					group.getMeta().setVersionId("1");
				}
			} else {
				Meta meta = new Meta();
				meta.setVersionId("1");
				group.setMeta(meta);
			}
		} catch (Exception e) {
			logger.error("Exception in addMemberToGroup of GroupServiceImpl ", e);
		}
	}

	/**
	 * Gets the group member component.
	 *
	 * @param patientMemberId   the patient member id
	 * @param providerId        the provider id
	 * @param providerReference the provider reference
	 * @param coverageReference the coverage reference
	 * @param attributionPeriod the attribution period
	 * @return the group member component
	 */
	public static GroupMemberComponent getGroupMemberComponent(String patientMemberId, String providerId,
			String providerReference, String coverageReference, Period attributionPeriod) {
		GroupMemberComponent theGroupMemberComponent = new GroupMemberComponent();
		List<Extension> theMembeEextensionList = null;
		try {
			if (StringUtils.isNotBlank(patientMemberId)) {
				Reference theReference = FhirUtil.getReference(patientMemberId, "Patient");
				if (theReference != null) {
					theGroupMemberComponent.setEntity(theReference);
					BooleanType theBoolean = FhirUtil.getBooleanType(false);
					theGroupMemberComponent.setInactiveElement(theBoolean);
				}
			}
			theMembeEextensionList = FhirUtil.getGroupMemberComponentExtension(providerId, providerReference,
					coverageReference, TextConstants.NEW_TYPE);
			if (theMembeEextensionList != null && !theMembeEextensionList.isEmpty()) {
				theGroupMemberComponent.setExtension(theMembeEextensionList);
			}
			if (attributionPeriod != null) {
				Period thePeriod = FhirUtil.getPeriod(attributionPeriod.getStartElement(),
						attributionPeriod.getEndElement());
				theGroupMemberComponent.setPeriod(thePeriod);
			}
		} catch (Exception ex) {
			logger.error("\n Exception while setting getGroupMemberComponent in FhirUtility class ", ex);
		}
		return theGroupMemberComponent;
	}

	/**
	 * Gets the entity id from group member component.
	 *
	 * @param memberGroup the member group
	 * @return the entity id from group member component
	 */
	private String getEntityIdFromGroupMemberComponent(GroupMemberComponent memberGroup) {
		String entityId = null;
		try {
			if (memberGroup.hasEntity() && memberGroup.getEntity().hasReferenceElement()) {
				entityId = memberGroup.getEntity().getReferenceElement().getIdPart();
			}
		} catch (Exception e) {
			logger.info("Exception in getEntityIdFromGroupMemberComponent of GroupServiceImpl ", e);
		}
		return entityId;
	}

	/**
	 * Gets the attribute provider id from group member component.
	 *
	 * @param memberGroup the member group
	 * @return the attribute provider id from group member component
	 */
	private String getAttributeProviderIdFromGroupMemberComponent(GroupMemberComponent memberGroup) {
		String attributeProviderId = null;
		try {
			if (memberGroup.hasExtension(TextConstants.MEMBER_PROVIDER_SYSTEM)) {
				if (memberGroup.getExtensionByUrl(TextConstants.MEMBER_PROVIDER_SYSTEM).hasValue()) {
					Reference reference = (Reference) memberGroup
							.getExtensionByUrl(TextConstants.MEMBER_PROVIDER_SYSTEM).getValue();
					attributeProviderId = reference.getReferenceElement().getIdPart();
				}
			}
		} catch (Exception e) {
			logger.info("Exception in getAttributeProviderIdFromGroupMemberComponent of GroupServiceImpl ", e);
		}
		return attributeProviderId;
	}

	/**
	 * Update group member component coverage reference extension.
	 *
	 * @param memberGroup                the member group
	 * @param coverageId                 the coverage id
	 * @param isAttributionCoverageFound the is attribution coverage found
	 * @return true, if successful
	 */
	private boolean updateGroupMemberComponentCoverageReferenceExtension(GroupMemberComponent memberGroup,
			String coverageId, boolean isAttributionCoverageFound) {
		try {
			if (StringUtils.isNotBlank(coverageId)) {
				if (memberGroup.hasExtension(TextConstants.MEMBER_COVERAGE_SYSTEM)) {
					if (memberGroup.getExtensionByUrl(TextConstants.MEMBER_COVERAGE_SYSTEM).hasValue()) {
						Reference reference = (Reference) memberGroup
								.getExtensionByUrl(TextConstants.MEMBER_COVERAGE_SYSTEM).getValue();
						if (!coverageId.equalsIgnoreCase(reference.getReferenceElement().getIdPart())) {
							Reference coverageReference = FhirUtil.getReference(coverageId, "Coverage");
							memberGroup.getExtensionByUrl(TextConstants.MEMBER_COVERAGE_SYSTEM)
									.setValue(coverageReference);
							updateGroupMemberComponentChangeTypeExtension(memberGroup, TextConstants.CHANGE_TYPE);
							isAttributionCoverageFound = true;
						} else {
							updateGroupMemberComponentChangeTypeExtension(memberGroup, TextConstants.NOCHANGE_TYPE);
							logger.info(" Coverage nochange ");
							isAttributionCoverageFound = false;
						}
					} else {
						Reference coverageReference = FhirUtil.getReference(coverageId, "Coverage");
						memberGroup.getExtensionByUrl(TextConstants.MEMBER_COVERAGE_SYSTEM).setValue(coverageReference);
						updateGroupMemberComponentChangeTypeExtension(memberGroup, TextConstants.CHANGE_TYPE);
						isAttributionCoverageFound = true;
					}
				} else {
					if (memberGroup.hasExtension()) {
						List<Extension> extensionList = memberGroup.getExtension();
						Extension coverageExtension = FhirUtil.getExtensionForReference(coverageId, "Coverage",
								TextConstants.MEMBER_COVERAGE_SYSTEM);
						if (coverageExtension != null && !coverageExtension.isEmpty()) {
							extensionList.add(coverageExtension);
							updateGroupMemberComponentChangeTypeExtension(memberGroup, TextConstants.CHANGE_TYPE);
							isAttributionCoverageFound = true;
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception in updateGroupMemberComponentCoverageReferenceExtension of GroupServiceImpl ", e);
		}
		return isAttributionCoverageFound;
	}

	/**
	 * Update group member component change type extension.
	 *
	 * @param memberGroup the member group
	 * @param changeCode  the change code
	 */
	private void updateGroupMemberComponentChangeTypeExtension(GroupMemberComponent memberGroup, String changeCode) {
		try {
			if (StringUtils.isNotBlank(changeCode)) {
				if (memberGroup.hasExtension(TextConstants.MEMBER_CHANGETYPE_SYSTEM)) {
					CodeType codeType = FhirUtil.getCodeType(changeCode);
					memberGroup.getExtensionByUrl(TextConstants.MEMBER_CHANGETYPE_SYSTEM).setValue(codeType);
				} else {
					if (memberGroup.hasExtension()) {
						List<Extension> extensionList = memberGroup.getExtension();
						Extension codeExtension = FhirUtil.getExtensionForCodeType(changeCode);
						if (codeExtension != null && !codeExtension.isEmpty()) {
							extensionList.add(codeExtension);
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception in updateGroupMemberComponentChangeTypeExtension of GroupServiceImpl ", e);
		}
	}

	/**
	 * Update group member component attribution period.
	 *
	 * @param memberGroup                the member group
	 * @param isAttributionCoverageFound the is attribution coverage found
	 * @param attributionPeriod          the attribution period
	 */
	private void updateGroupMemberComponentAttributionPeriod(GroupMemberComponent memberGroup,
			boolean isAttributionCoverageFound, Period attributionPeriod) {
		try {
			if (attributionPeriod != null) {
				Date startOne = null;
				Date endOne = null;
				Date memberStart = null;
				Date memberEnd = null;
				if (attributionPeriod.hasStart()) {
					startOne = attributionPeriod.getStart();
				}
				if (attributionPeriod.hasEnd()) {
					endOne = attributionPeriod.getEnd();
				}
				if (memberGroup.hasPeriod()) {
					Period memberPeriod = memberGroup.getPeriod();
					if (memberPeriod.hasStart()) {
						memberStart = memberPeriod.getStart();
					}
					if (memberPeriod.hasEnd()) {
						memberEnd = memberPeriod.getEnd();
					}
					if (!startOne.equals(memberStart) || !endOne.equals(memberEnd)) {
						memberGroup.setPeriod(attributionPeriod);
						updateGroupMemberComponentChangeTypeExtension(memberGroup, TextConstants.CHANGE_TYPE);
					} else if (!isAttributionCoverageFound) {
						updateGroupMemberComponentChangeTypeExtension(memberGroup, TextConstants.NOCHANGE_TYPE);
					}
				} else {
					memberGroup.setPeriod(attributionPeriod);
					updateGroupMemberComponentChangeTypeExtension(memberGroup, TextConstants.CHANGE_TYPE);
				}
			}
		} catch (Exception e) {
			logger.error("Exception in updateGroupMemberComponentAttributionPeriod of GroupServiceImpl ", e);
		}
	}

	public Group processRemoveMemberToGroup(Parameters theParameters, String groupId){
		String patientMemberId = null;
		String attributeProviderId = null;
		String attributeProviderReferenceResource = null;
		String coverageReference = null;
		Period attributionPeriod = null;
		Group group = groupDao.read(new IdType(groupId));
		if (group != null && !group.isEmpty() && theParameters != null && !theParameters.isEmpty()) {
			if (theParameters.getParameter(TextConstants.MEMBER_ID) != null) {
				// Creating the TokenAndListParam object to add into the SearchParamMap
				SearchParameterMap patientParamMap = new SearchParameterMap();
				TokenAndListParam tokenParam = FhirUtil.createTokenAndListParam(theParameters, "memberId");
				patientParamMap.add(Patient.SP_IDENTIFIER, tokenParam);
				// Invoking the Patient Search Dao API.
				IBundleProvider bundle = patientDao.search(patientParamMap);
				logger.info("Received Bundle with Size:::::{}", bundle.getAllResources().size());
				for (IBaseResource iBaseResource : bundle.getAllResources()) {
					Resource resource = (Resource) iBaseResource;
					if (resource.fhirType().equals("Patient")) {
						logger.info("patientId::::::" + resource.getIdElement().getIdPart());
						patientMemberId = resource.getIdElement().getIdPart();
					}
				}
				if (patientMemberId == null) {
					throw new ResourceNotFoundException("Couldn't find any Patient with given memberId");
				}

				// Get Practitioner Id using the NPI details from received Parameters.
				// Creating the TokenAndListParam object to add into the SearchParamMap
				Map<String, String> providerMap = findProviderIdByIdentifier(theParameters);
				if (providerMap != null && !providerMap.isEmpty()) {
					for (Map.Entry<String, String> entry : providerMap.entrySet()) {
						attributeProviderId = entry.getValue();
						attributeProviderReferenceResource = entry.getKey();
					}
				}
				if (providerMap.isEmpty()) {
					throw new ResourceNotFoundException("Couldn't find any Providers with given providerNpi");
				}
			} else if (theParameters.getParameter(TextConstants.PATIENT_REFERENCE) != null) {
				String patientId = findPatientIdByReference(theParameters);
				if (StringUtils.isNotBlank(patientId)) {
					patientMemberId = patientId;
					if (theParameters.getParameter(TextConstants.PROVIDER_REFERENCE) != null) {
						Map<String, String> providerMap = findProviderIdByReference(theParameters);
						if (providerMap != null && !providerMap.isEmpty()) {
							for (Map.Entry<String, String> entry : providerMap.entrySet()) {
								attributeProviderId = entry.getValue();
								attributeProviderReferenceResource = entry.getKey();
							}
						} else {
							throw new ResourceNotFoundException(
									"Couldn't find any Providers with given providerReference");
						}
					}
					String coverageId = findCoverageIdByPatientId(patientId);
					if (StringUtils.isNotBlank(coverageId)) {
						coverageReference = coverageId;
					}
				} else {
					throw new ResourceNotFoundException("Couldn't find any Patient with given patientReference");
				}
			} else {
				throw new UnprocessableEntityException(
						"Please provide memberId + providerNpi or patientReference + providerReference to $member-add.");
			}
			if (theParameters.getParameter(TextConstants.ATTRIBUTION_PERIOD) != null) {
				attributionPeriod = (Period) theParameters.getParameter(TextConstants.ATTRIBUTION_PERIOD);
			}
			if (StringUtils.isNotBlank(patientMemberId)) {
				logger.info(" patientMemberId :: " + patientMemberId);
				logger.info(" attributeProviderId :: " + attributeProviderId);
				logger.info(" attributeProviderReferenceResource :: " + attributeProviderReferenceResource);
				logger.info(" coverageReference :: " + coverageReference);
				if (attributionPeriod != null) {
					logger.info(" attributionPeriod.getStart() :: " + attributionPeriod.getStart());
					logger.info(" attributionPeriod.getEnd() :: " + attributionPeriod.getEnd());
				}
				removeMemberFromGroup(group, patientMemberId, attributeProviderId, attributeProviderReferenceResource,
						coverageReference, attributionPeriod);
				
				groupDao.update(group);
				
			} else {
				throw new ResourceNotFoundException("No patient found ");
			}
		} else {
			throw new UnprocessableEntityException("No Parameters/Group  Not found!");
		}
		return group;
	}

	public void removeMemberFromGroup(Group group, String patientMemberId, String providerId,
			String providerReferenceResource, String coverageId, Period attributionPeriod){
		logger.info(" patientMemberId :: " + patientMemberId);
		logger.info(" providerId :: " + providerId);
		logger.info(" providerReferenceResource :: " + providerReferenceResource);
		logger.info(" coverageId :: " + coverageId);
		List<GroupMemberComponent> memberList = new ArrayList<>();
		boolean isGroupMemberRemoved = false;
		if (group.hasMember()) {
			memberList = group.getMember();
			for (GroupMemberComponent memberGroup : new ArrayList<GroupMemberComponent>(memberList)) {
				// GroupMemberComponent memberGroup = iterator.next();
				if (memberGroup.hasEntity() && memberGroup.getEntity().hasReferenceElement()) {
					String entityId = memberGroup.getEntity().getReferenceElement().getIdPart();
					logger.info(" entityId :: " + entityId);
					if (patientMemberId.equalsIgnoreCase(entityId)) {
						if (StringUtils.isNotBlank(providerId) && StringUtils.isNotBlank(providerReferenceResource)) {
							if (memberGroup.hasExtension(TextConstants.MEMBER_PROVIDER_SYSTEM)) {
								if (memberGroup.getExtensionByUrl(TextConstants.MEMBER_PROVIDER_SYSTEM).hasValue()) {
									Reference reference = (Reference) memberGroup
											.getExtensionByUrl(TextConstants.MEMBER_PROVIDER_SYSTEM).getValue();
									if (providerId.equalsIgnoreCase(reference.getReferenceElement().getIdPart())
											&& providerReferenceResource.equalsIgnoreCase(
													reference.getReferenceElement().getResourceType())) {
										if (StringUtils.isNotBlank(coverageId)) {
											if (memberGroup.hasExtension(TextConstants.MEMBER_COVERAGE_SYSTEM)) {
												if (memberGroup.getExtensionByUrl(TextConstants.MEMBER_COVERAGE_SYSTEM)
														.hasValue()) {
													Reference coverageReference = (Reference) memberGroup
															.getExtensionByUrl(TextConstants.MEMBER_COVERAGE_SYSTEM)
															.getValue();
													if (coverageId.equalsIgnoreCase(
															coverageReference.getReferenceElement().getIdPart())) {
														memberList.remove(memberGroup);
														isGroupMemberRemoved = true;
														logger.info(
																" Removing member from Group.member for memberId+providerNpi+attributionPeriod / "
																		+ "patientReference+providerReference+attributionPeriod. patientMemberId: "
																		+ patientMemberId + " providerId: " + providerId
																		+ " coverageId : " + coverageId);
													} else {
														throw new ResourceNotFoundException(
																" No coverage found for given attributionPeriod  "
																		+ coverageId);
													}
												}
											}
										} else {
											memberList.remove(memberGroup);
											isGroupMemberRemoved = true;
											logger.info(" Removing member from Group.member for memberId+providerNpi / "
													+ "patientReference+providerReference. patientMemberId: "
													+ patientMemberId + " providerId: " + providerId);
										}
									} else {
										throw new ResourceNotFoundException(
												" No provider found for given provider " + providerId);
									}
								}
							}
						} else {
							memberList.remove(memberGroup);
							isGroupMemberRemoved = true;
							logger.info(
									" Removing member from Group.member for memberId/patientReference. patientMemberId : "
											+ patientMemberId);
						}
						break;
					}
				}
			}
		} else {
			logger.error(" :: Group doesn't have any members ");
		}
		if (isGroupMemberRemoved) {
			if (group.hasMeta()) {
				if (group.getMeta().hasVersionId()) {
					String versionId = group.getMeta().getVersionId();
					int version = Integer.parseInt(versionId);
					version = version + 1;
					group.getMeta().setVersionId(String.valueOf(version));

				} else {
					group.getMeta().setVersionId("1");
				}
			} else {
				Meta meta = new Meta();
				meta.setVersionId("1");
				group.setMeta(meta);
			}
		} else {
			throw new UnprocessableEntityException("Group doesn't contain given memberId/patientReference");
		}
	}
}
