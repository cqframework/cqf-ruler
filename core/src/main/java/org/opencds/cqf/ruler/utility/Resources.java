package org.opencds.cqf.ruler.utility;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

public class Resources {

	private Resources() {
	}

	public static <T extends IBaseResource, I extends IIdType> T newResource(Class<T> theResourceClass,
			String theIdPart) {
		checkNotNull(theResourceClass);
		checkNotNull(theIdPart);
		checkArgument(!theIdPart.contains("/"), "theIdPart must be a simple id. Do not include resourceType or history");
		T resource = newResource(theResourceClass);

		@SuppressWarnings("unchecked")
		I id = (I) Ids.newId(theResourceClass, theIdPart);
		resource.setId(id);
		return resource;
	}

	public static <T extends IBaseResource> T newResource(Class<T> theResourceClass) {
		checkNotNull(theResourceClass);
		T resource = null;
		try {
			resource = (T) theResourceClass.getConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"theResourceClass must be a type with an empty default constructor to use this function");
		}

		return resource;
	}
}
