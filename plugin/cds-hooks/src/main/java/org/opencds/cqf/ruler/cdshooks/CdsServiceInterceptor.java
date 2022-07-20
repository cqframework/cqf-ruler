package org.opencds.cqf.ruler.cdshooks;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.opencds.cqf.ruler.cdshooks.discovery.DiscoveryResolutionR4;
import org.opencds.cqf.ruler.cdshooks.discovery.DiscoveryResolutionStu3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

@Interceptor
public class CdsServiceInterceptor implements org.opencds.cqf.ruler.api.Interceptor {
	private static final Logger logger = LoggerFactory.getLogger(CdsServiceInterceptor.class);

	private AtomicReference<JsonArray> cdsServiceCache;
	private DiscoveryResolutionR4 discoveryResolutionR4;
	private DiscoveryResolutionStu3 discoveryResolutionStu3;

	public CdsServiceInterceptor(DaoRegistry daoRegistry) {
		this.discoveryResolutionR4 = new DiscoveryResolutionR4(daoRegistry);
		this.discoveryResolutionStu3 = new DiscoveryResolutionStu3(daoRegistry);
		this.cdsServiceCache = new AtomicReference<>(new JsonArray());
	}

	public AtomicReference<JsonArray> getCdsServiceCache() {
		return this.cdsServiceCache;
	}

	@Hook(Pointcut.STORAGE_PRECOMMIT_RESOURCE_CREATED)
	public void insert(IBaseResource resource) {
		try {
			if (resource instanceof PlanDefinition) {
				cdsServiceCache.get().add(discoveryResolutionR4.resolveService((PlanDefinition) resource));
			} else if (resource instanceof org.hl7.fhir.dstu3.model.PlanDefinition) {
				cdsServiceCache.get().add(discoveryResolutionStu3.resolveService((org.hl7.fhir.dstu3.model.PlanDefinition) resource));
			}
		} catch (Exception e) {
			logger.info(String.format("Failed to create service for %s", resource.getIdElement().getIdPart()));
		}
	}

	@Hook(Pointcut.STORAGE_PRECOMMIT_RESOURCE_UPDATED)
	public void update(IBaseResource oldResource, IBaseResource newResource) {
		try {
			if (newResource instanceof PlanDefinition || newResource instanceof org.hl7.fhir.dstu3.model.PlanDefinition) {
				delete(oldResource);
				insert(newResource);
			}
		} catch (Exception e) {
			logger.info(String.format("Failed to update service for %s", newResource.getIdElement().getIdPart()));
		}
	}

	@Hook(Pointcut.STORAGE_PRECOMMIT_RESOURCE_DELETED)
	public void delete(IBaseResource resource) {
		try {
			for (int i = 0; i<cdsServiceCache.get().size(); i++) {
				if (((JsonObject) cdsServiceCache.get().get(i)).get("id").getAsString().equals(resource.getIdElement().getIdPart())) {
					cdsServiceCache.get().remove(i);
					break;
				}
			}
		} catch (Exception e) {
			logger.info(String.format("Failed to delete service for %s", resource.getIdElement().getIdPart()));
		}
	}
}
