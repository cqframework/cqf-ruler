package org.opencds.cqf.config;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.instance.model.api.IAnyResource;

import ca.uhn.fhir.jpa.provider.BaseJpaResourceProvider;
import ca.uhn.fhir.rest.server.IResourceProvider;

public class ResourceProviderRegistry {
    private List<BaseJpaResourceProvider<? extends IAnyResource>>  providers;

    public ResourceProviderRegistry()
    {
        this.providers = new ArrayList<>();
    }

    public ResourceProviderRegistry(List<BaseJpaResourceProvider<? extends IAnyResource>>  providers) {
        this.providers = providers;
    }

    public synchronized List<BaseJpaResourceProvider<? extends IAnyResource>> getResourceProviders() {
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
            BaseJpaResourceProvider<? extends IAnyResource> jpaResourceProvider = (BaseJpaResourceProvider<? extends IAnyResource>)resourceProvider;
            this.register(jpaResourceProvider);
        }
        else {
            throw new IllegalArgumentException("resourceProvider is not a JpaResourceProvider");
        }
    }

    public synchronized void register(BaseJpaResourceProvider<? extends IAnyResource> resourceProvider)
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


    public synchronized BaseJpaResourceProvider<? extends IAnyResource> resolve(String dataType) {
        if (dataType == null) {
            throw new IllegalArgumentException("dataType can not be null");
        }

        for (BaseJpaResourceProvider<? extends IAnyResource> resourceProvider : this.providers) {
            if (resourceProvider.getResourceType().getSimpleName().toLowerCase().equals(dataType.toLowerCase())) {
                return resourceProvider;
            }
        }
        
        return null;
	}
}