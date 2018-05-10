package org.opencds.cqf.cdshooks.request;

import com.google.gson.JsonObject;
import org.opencds.cqf.cdshooks.providers.CdsHooksProviders;
import org.opencds.cqf.cdshooks.providers.Discovery;
import org.opencds.cqf.cdshooks.providers.DiscoveryItem;
import org.opencds.cqf.exceptions.InvalidPrefetchException;

import java.util.ArrayList;
import java.util.List;

public class Prefetch {

    private List<Object> resources;

    public Prefetch(JsonObject prefetchObject, CdsHooksProviders providers, String patientId) {
        Discovery discovery = providers.getDiscovery();
        resources = new ArrayList<>();

        if (prefetchObject == null || prefetchObject.size() == 0) {
            // resolve elements
            for (DiscoveryItem item : discovery.getItems()) {
                resources.addAll(providers.search(item, patientId));
            }
        }

        else if (prefetchObject.size() != discovery.getItems().size()) {
            // too many elements - error
            if (prefetchObject.size() > discovery.getItems().size()) {
                throw new InvalidPrefetchException(
                        String.format(
                                "Expecting %d fields in the prefetch. Found %d fields",
                                discovery.getItems().size(), prefetchObject.size()
                        )
                );
            }

            // resolve missing elements
            for (DiscoveryItem item : discovery.getItems()) {
                if (prefetchObject.get(item.getItemNo()) == null) {
                    // resolve element
                    resources.addAll(providers.search(item, patientId));
                }
            }
        }

        else {
            for (String key : prefetchObject.keySet()) {
                if (prefetchObject.get(key).isJsonNull()) {
                    continue;
                }
                JsonObject prefetchElement = JsonFieldResolution.getObjectField(prefetchObject, key, true);
                if (prefetchElement.has("resource") && prefetchElement.get("resource").isJsonNull()) {
                    continue;
                }
                resources.addAll(
                        CdsHooksHelper.parseResources(
                                JsonFieldResolution.getObjectField(prefetchElement, "resource", true), providers.getVersion()
                        )
                );
            }
        }
    }

    public List<Object> getResources() {
        return resources;
    }

}
