package org.opencds.cqf.ruler.atr;

import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.ruler.atr.service.MalService;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;


public class MalProvider extends DaoRegistryOperationProvider {
	@Autowired
	private MalService malService;

	/**
	 * Member add.
	 *
	 * @param groupId the group id
	 * @param requestDetails the request details
	 * @param request the request
	 * @param response the response
	 */
	@Description(shortDefinition = "Add new Member to the Group",
			value = "Implements the $member-add operation")
	@Operation(idempotent = true, name = "$member-add", type = Group.class)
	public Group memberAdd(@IdParam IdType groupId, @ResourceParam Parameters theParameters,
			RequestDetails requestDetails) {
		return malService.processAddMemberToGroup(theParameters, groupId, requestDetails);
	}


	@Description(shortDefinition = "Remove Member from the Group",
			value = "Implements the $member-remove operation")
	@Operation(idempotent = true, name = "$member-remove")
	public Group memberRemove(@IdParam IdType groupId, @ResourceParam Parameters theParameters,
			RequestDetails requestDetails) {
		return malService.processRemoveMemberToGroup(theParameters, groupId.getIdPart());
	}
}
