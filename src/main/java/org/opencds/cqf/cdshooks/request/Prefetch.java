package org.opencds.cqf.cdshooks.request;

import com.google.gson.JsonObject;
import org.opencds.cqf.cdshooks.providers.CdsHooksProviders;
import org.opencds.cqf.exceptions.InvalidPrefetchException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Prefetch {

    private List<Object> resources;
    private JsonObject prefetchObject;
    private Set<String> prefetchUrls;

    public Prefetch() { }

    public Prefetch(JsonObject prefetchObject) {
        this.prefetchObject = prefetchObject;
    }

    public List<Object> getResources(CdsHooksProviders.FhirVersion version) {
        if (resources == null) {
            resources = new ArrayList<>();
            // prefetchUrls haven't been initialized
            if (prefetchUrls == null) {
                throw new RuntimeException("prefetch urls must be initialized before resolving prefetch resources");
            }
            // missing fields
            if (prefetchObject == null && !prefetchUrls.isEmpty()) {
                throw new InvalidPrefetchException("Missing prefetch fields: " + prefetchUrls.toString());
            }
            // mismatched number of fields
            if (prefetchObject != null && prefetchObject.size() != prefetchUrls.size()) {
                throw new InvalidPrefetchException(String.format("Expecting %d fields in the prefetch. Found %d fields", prefetchUrls.size(), prefetchObject.size()));
            }
            // parse prefetch JSON
            if (prefetchObject != null) {
                for (String key : prefetchObject.keySet()) {
                    if (prefetchObject.get(key).isJsonNull()) {
                        continue;
                    }
                    JsonObject prefetchElement = JsonFieldResolution.getObjectField(prefetchObject, key, true);
                    if (prefetchElement.get("resource").isJsonNull()) {
                        continue;
                    }
                    resources.addAll(
                            CdsHooksHelper.parseResources(
                                    JsonFieldResolution.getObjectField(prefetchElement, "resource", true), version
                            )
                    );
                }
            }
        }
        return resources;
    }
    public void setResources(List<Object> resources) {
        this.resources = resources;
    }
    public void setPrefetchUrls(Set<String> prefetchUrls) {
        this.prefetchUrls = prefetchUrls;
    }
}
