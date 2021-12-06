package org.opencds.cqf.ruler.plugin.cpg.utilities;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.IterableUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.execution.Context;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.rest.api.IVersionSpecificBundleFactory;

public interface ExecutionUtilities {
    // Adds the resources returned from the given expressions to a bundle
    public default IBaseResource bundle(FhirContext fhirContext, Context executionContext, String... expressionNames) {
        return bundle(fhirContext, executionContext, null, expressionNames);
    }
    // Adds the resources returned from the given expressions to a bundle
    @SuppressWarnings("unchecked")
    public default IBaseResource bundle(FhirContext fhirContext, Context executionContext, String theServerBase, String... expressionNames) {
        IVersionSpecificBundleFactory bundleFactory = fhirContext.newBundleFactory();
        List<IBaseResource> resources = new ArrayList<IBaseResource>();
        for (String expressionName : expressionNames) {
            Object result = executionContext.resolveExpressionRef(expressionName).evaluate(executionContext);
            for (Object element : (Iterable<Object>) result) {
                resources.add((IBaseResource) element);
            }
        }
        bundleFactory.addResourcesToBundle(resources, BundleTypeEnum.COLLECTION, theServerBase, null, null);
        return bundleFactory.getResourceBundle();
    }

    public default IBaseResource bundle(Iterable<IBaseResource> resources, FhirContext fhirContext) {
        return bundle(resources, fhirContext, null);
    }

    public default IBaseResource bundle(Iterable<IBaseResource> resources, FhirContext fhirContext, String theServerBase) {
        IVersionSpecificBundleFactory bundleFactory = fhirContext.newBundleFactory();
        bundleFactory.addResourcesToBundle(IterableUtils.toList(resources), BundleTypeEnum.COLLECTION, theServerBase, null, null);
        return bundleFactory.getResourceBundle();
    }
}
