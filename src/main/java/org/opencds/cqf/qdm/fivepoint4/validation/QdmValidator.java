package org.opencds.cqf.qdm.fivepoint4.validation;

import org.opencds.cqf.qdm.fivepoint4.exception.InconsistentId;
import org.opencds.cqf.qdm.fivepoint4.exception.InvalidResourceType;
import org.opencds.cqf.qdm.fivepoint4.exception.MissingId;
import org.opencds.cqf.qdm.fivepoint4.model.BaseType;
import org.opencds.cqf.qdm.fivepoint4.model.Id;
import org.opencds.cqf.qdm.fivepoint4.model.Patient;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class QdmValidator {

    public static void validateResourceId(Id resourceId, String requestId)
    {
        if (resourceId == null || resourceId.getValue() == null)
        {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Update failed: payload resource is missing id value",
                    new MissingId()
            );
        }
        if (!resourceId.getValue().equals(requestId))
        {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    String.format("Update failed: resource id (%s) doesn't match request id (%s)", resourceId.getValue(), requestId),
                    new InconsistentId()
            );
        }
    }

    public static void validateResourceTypeAndName(BaseType payloadResource, BaseType storedResource)
    {
        if (payloadResource.getResourceType() == null
                || !storedResource.getResourceType().equals(payloadResource.getResourceType())
                || !storedResource.getName().equals(payloadResource.getResourceType()))
        {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Update failed: invalid payload resourceType "
                            + (payloadResource.getResourceType() == null ? "null" : payloadResource.getResourceType()),
                    new InvalidResourceType()
            );
        }
    }

    public static void validatePatientTypeAndName(Patient payloadResource, Patient storedResource)
    {
        if (payloadResource.getResourceType() == null
                || !storedResource.getResourceType().equals(payloadResource.getResourceType())
                || !storedResource.getName().equals(payloadResource.getResourceType()))
        {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Update failed: invalid payload resourceType "
                            + (payloadResource.getResourceType() == null ? "null" : payloadResource.getResourceType()),
                    new InvalidResourceType()
            );
        }
    }
}
