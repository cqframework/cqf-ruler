package org.hl7.davinci.atr.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hl7.davinci.atr.server.service.MALService;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

/**
 * The Class MALProvider.
 */
public class MALProvider extends DaoRegistryOperationProvider{

	/** The Constant logger. */
	private static final Logger logger = LoggerFactory.getLogger(MALProvider.class);
	
	/** The group dao. */
	@Autowired
	private IFhirResourceDao<Group> groupDao;
	
	@Autowired
	private MALService malService;
	
	/**
	 * Member add.
	 *
	 * @param groupId the group id
	 * @param requestDetails the request details
	 * @param request the request
	 * @param response the response
	 */
	@Description(shortDefinition = "Add new Member to the Group", value = "Implements the $member-add operation")
	@Operation(idempotent = true, name = "$member-add",type = Group.class,manualResponse=true, manualRequest=true)
	public MethodOutcome memberAdd(@IdParam IdType groupId,@ResourceParam Parameters theParameters, RequestDetails requestDetails, HttpServletRequest request,
			HttpServletResponse response) {
		MethodOutcome retVal = new MethodOutcome();
		if (request.getHeader("Content-Type") != null
				&& request.getHeader("Content-Type").equals("application/fhir+json")) {
			Group updatedGroup = malService.processAddMemberToGroup(theParameters, groupId,requestDetails);
			if (updatedGroup != null) {
				response.setStatus(201);
				retVal.setId(new IdType("Group", updatedGroup.getIdElement().getIdPart(),
						updatedGroup.getMeta().getVersionId()));
			}
		} else {
			throw new UnprocessableEntityException("Invalid header values!");
		}
		return retVal;
	}
	
	
	
	@Description(shortDefinition = "Add new Member to the Group", value = "Implements the $member-add operation")
	@Operation(idempotent = true, name = "$member-remove",type = Group.class,manualResponse=true, manualRequest=true)
	public MethodOutcome memberRemove(@IdParam IdType groupId,@ResourceParam Parameters theParameters, RequestDetails requestDetails, HttpServletRequest request,
			HttpServletResponse response) {
		MethodOutcome retVal = new MethodOutcome();
		if (request.getHeader("Content-Type") != null
				&& request.getHeader("Content-Type").equals("application/fhir+json")) {
			Group updatedGroup = malService.processRemoveMemberToGroup(theParameters, groupId.getIdPart());
			if (updatedGroup != null) {
				response.setStatus(201);
				retVal.setId(new IdType("Group", updatedGroup.getIdElement().getIdPart(),
						updatedGroup.getMeta().getVersionId()));
			}
		} else {
			throw new UnprocessableEntityException("Invalid header values!");
		}
		return retVal;
	}
}
