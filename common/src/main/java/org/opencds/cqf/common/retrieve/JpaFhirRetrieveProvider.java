package org.opencds.cqf.common.retrieve;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.fhir.retrieve.SearchParamFhirRetrieveProvider;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterMap;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;

import ca.uhn.fhir.jpa.dao.DaoRegistry;
import ca.uhn.fhir.jpa.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.api.server.IBundleProvider;

public class JpaFhirRetrieveProvider extends SearchParamFhirRetrieveProvider {

    DaoRegistry registry;

    public JpaFhirRetrieveProvider(DaoRegistry registry, SearchParameterResolver searchParameterResolver) {
        super(searchParameterResolver);
        this.registry = registry;
    }

    @Override
    protected Iterable<Object> executeQueries(String dataType, List<SearchParameterMap> queries) {
        if (queries == null || queries.isEmpty()) {
            return Collections.emptyList();
        }

        List<Object> objects = new ArrayList<>();
        for (SearchParameterMap map : queries) {
            objects.addAll(executeQuery(dataType, map));
        }

        return objects;
    }

    protected Collection<Object> executeQuery(String dataType, SearchParameterMap map) {
        // TODO: Once HAPI breaks this out from the server dependencies
        // we can include it on its own.
        var hapiMap = new ca.uhn.fhir.jpa.searchparam.SearchParameterMap();
        try {

            var method = List.of(hapiMap.getClass().getMethods()).stream()
                    .filter(x -> x.getName().equals("put") && Modifier.isPrivate(x.getModifiers())).findFirst().get();
            method.setAccessible(true);

            for (var entry : map.entrySet()) {
                method.invoke(hapiMap, entry.getKey(), entry.getValue());
            }

        } catch (Exception e) {
            // TODO: Add logging.
        }

        IFhirResourceDao<?> dao = this.registry.getResourceDao(dataType);

        IBundleProvider bundleProvider = dao.search(hapiMap);
        if (bundleProvider.size() == null) {
            return resolveResourceList(bundleProvider.getResources(0, 10000));
        }
        if (bundleProvider.size() == 0) {
            return new ArrayList<>();
        }
        List<IBaseResource> resourceList = bundleProvider.getResources(0, bundleProvider.size());
        return resolveResourceList(resourceList);
    }

    public synchronized Collection<Object> resolveResourceList(List<IBaseResource> resourceList) {
        List<Object> ret = new ArrayList<>();
        for (IBaseResource res : resourceList) {
            Class<?> clazz = res.getClass();
            ret.add(clazz.cast(res));
        }
        // ret.addAll(resourceList);
        return ret;
    }
}