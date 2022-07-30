package org.opencds.cqf.ruler.cdshooks;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.cache.IResourceChangeEvent;
import ca.uhn.fhir.jpa.cache.IResourceChangeListener;
import ca.uhn.fhir.jpa.cache.ResourceChangeEvent;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.opencds.cqf.ruler.cdshooks.discovery.DiscoveryResolutionR4;
import org.opencds.cqf.ruler.cdshooks.discovery.DiscoveryResolutionStu3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class CdsServicesCache implements IResourceChangeListener {
	private static final Logger logger = LoggerFactory.getLogger(CdsServicesCache.class);

	private AtomicReference<JsonArray> cdsServiceCache;
	private IFhirResourceDao planDefinitionDao;
	private DiscoveryResolutionR4 discoveryResolutionR4;
	private DiscoveryResolutionStu3 discoveryResolutionStu3;

	public CdsServicesCache(DaoRegistry daoRegistry) {
		this.planDefinitionDao = daoRegistry.getResourceDao("PlanDefinition");
		this.discoveryResolutionR4 = new DiscoveryResolutionR4(daoRegistry);
		this.discoveryResolutionStu3 = new DiscoveryResolutionStu3(daoRegistry);
		this.cdsServiceCache = new AtomicReference<>(new JsonArray());
	}

	public AtomicReference<JsonArray> getCdsServiceCache() {
		return this.cdsServiceCache;
	}

	public void clearCache() {
		this.cdsServiceCache = new AtomicReference<>(new JsonArray());
	}

	@Override
	public void handleInit(Collection<IIdType> collection) {
		handleChange(ResourceChangeEvent.fromCreatedResourceIds(collection));
	}

	@Override
	public void handleChange(IResourceChangeEvent iResourceChangeEvent) {
		if (iResourceChangeEvent == null) return;
		if (iResourceChangeEvent.getCreatedResourceIds() != null
				&& !iResourceChangeEvent.getCreatedResourceIds().isEmpty()) {
			insert(iResourceChangeEvent.getCreatedResourceIds());
		}
		if (iResourceChangeEvent.getUpdatedResourceIds() != null
				&& !iResourceChangeEvent.getUpdatedResourceIds().isEmpty()) {
			update(iResourceChangeEvent.getUpdatedResourceIds());
		}
		if (iResourceChangeEvent.getDeletedResourceIds() != null
				&& !iResourceChangeEvent.getDeletedResourceIds().isEmpty()) {
			delete(iResourceChangeEvent.getDeletedResourceIds());
		}
	}

	private void insert(List<IIdType> createdIds) {
		for (IIdType id : createdIds) {
			try {
				IBaseResource resource = planDefinitionDao.read(id);
				if (resource instanceof PlanDefinition) {
					cdsServiceCache.get().add(discoveryResolutionR4.resolveService((PlanDefinition) resource));
				} else if (resource instanceof org.hl7.fhir.dstu3.model.PlanDefinition) {
					cdsServiceCache.get().add(
							discoveryResolutionStu3.resolveService((org.hl7.fhir.dstu3.model.PlanDefinition) resource));
				}
			} catch (Exception e) {
				logger.info(String.format("Failed to create service for %s", id.getIdPart()));
			}
		}
	}

	private void update(List<IIdType> updatedIds) {
		try {
			delete(updatedIds);
			insert(updatedIds);
		} catch (Exception e) {
			logger.info(String.format("Failed to update service(s) for %s", updatedIds));
		}
	}

	private void delete(List<IIdType> deletedIds) {
		for (IIdType id : deletedIds) {
			for (int i = 0; i < cdsServiceCache.get().size(); i++) {
				if (((JsonObject) cdsServiceCache.get().get(i)).get("id").getAsString().equals(id.getIdPart())) {
					cdsServiceCache.get().remove(i);
					break;
				}
				else logger.info(String.format("Failed to delete service for %s", id.getIdPart()));
			}
		}
	}
}
