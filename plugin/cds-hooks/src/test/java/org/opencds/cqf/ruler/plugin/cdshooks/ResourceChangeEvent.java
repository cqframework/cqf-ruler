package org.opencds.cqf.ruler.plugin.cdshooks;

import ca.uhn.fhir.jpa.cache.IResourceChangeEvent;
import org.hl7.fhir.instance.model.api.IIdType;

import java.util.List;

public class ResourceChangeEvent implements IResourceChangeEvent {
	private List<IIdType> createdResourceIds;
	private List<IIdType> updatedResourceIds;
	private List<IIdType> deletedResourceIds;

	@Override
	public List<IIdType> getCreatedResourceIds() {
		return this.createdResourceIds;
	}

	public void setCreatedResourceIds(List<IIdType> createdResourceIds) {
		this.createdResourceIds = createdResourceIds;
	}

	@Override
	public List<IIdType> getUpdatedResourceIds() {
		return this.updatedResourceIds;
	}

	public void setUpdatedResourceIds(List<IIdType> updatedResourceIds) {
		this.updatedResourceIds = updatedResourceIds;
	}

	@Override
	public List<IIdType> getDeletedResourceIds() {
		return this.deletedResourceIds;
	}

	public void setDeletedResourceIds(List<IIdType> deletedResourceIds) {
		this.deletedResourceIds = deletedResourceIds;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}
}
