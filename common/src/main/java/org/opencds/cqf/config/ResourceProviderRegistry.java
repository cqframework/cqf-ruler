package org.opencds.cqf.config;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.jpa.provider.BaseJpaResourceProvider;
import ca.uhn.fhir.rest.server.IResourceProvider;

public class ResourceProviderRegistry {
    private List<BaseJpaResourceProvider<? extends IBaseResource>>  providers;

    public ResourceProviderRegistry()
    {
        this.providers = new ArrayList<>();
    }

    public ResourceProviderRegistry(List<BaseJpaResourceProvider<? extends IBaseResource>>  providers) {
        this.providers = providers;
    }

    public synchronized List<BaseJpaResourceProvider<? extends IBaseResource>> getResourceProviders() {
        return this.providers;
    }

    public synchronized void unregister(IResourceProvider resourceProvider) {
        if (resourceProvider == null) {
            return;
        }

        if (resourceProvider instanceof BaseJpaResourceProvider<?>) {
            BaseJpaResourceProvider<?> jpaResourceProvider = (BaseJpaResourceProvider<?>)resourceProvider;
            this.unregister(jpaResourceProvider);
        }
        else {
            throw new IllegalArgumentException("resourceProvider is not a JpaResourceProvider");
        }
    }

    public synchronized void unregister(BaseJpaResourceProvider<? extends IAnyResource> resourceProvider)
    {
        if (resourceProvider == null) {
            return;
        }

        if (this.providers.contains(resourceProvider)) {
            this.providers.remove(resourceProvider);
        }
    }

    public synchronized void register(IResourceProvider resourceProvider) {
        if (resourceProvider == null) {
            return;
        }

        if (resourceProvider instanceof BaseJpaResourceProvider<?>) {
            BaseJpaResourceProvider<? extends IBaseResource> jpaResourceProvider = (BaseJpaResourceProvider<? extends IBaseResource>)resourceProvider;
            this.register(jpaResourceProvider);
        }
        else {
            throw new IllegalArgumentException("resourceProvider is not a JpaResourceProvider");
        }
    }

    public synchronized void register(BaseJpaResourceProvider<? extends IBaseResource> resourceProvider)
    {
        if (resourceProvider == null) {
            return;
        }

        String dataType = resourceProvider.getResourceType().getSimpleName();
        BaseJpaResourceProvider<?> oldProvider = this.resolve(dataType);
        if (oldProvider != null) {
            this.providers.remove(oldProvider);
        }

        this.providers.add(resourceProvider);
    }


    public synchronized BaseJpaResourceProvider<? extends IBaseResource> resolve(String dataType) {
        if (dataType == null) {
            throw new IllegalArgumentException("dataType can not be null");
        }

        for (BaseJpaResourceProvider<? extends IBaseResource> resourceProvider : this.providers) {
            if (resourceProvider.getResourceType().getSimpleName().toLowerCase().equals(dataType.toLowerCase())) {
                return resourceProvider;
            }
        }
        
        return null;
	}
}