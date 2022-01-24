package org.opencds.cqf.ruler.utility;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.rest.api.server.IBundleProvider;

public class TypedBundleProvider<T extends IBaseResource> implements IBundleProvider {
	
	private IBundleProvider myInnerProvider;

	public static <T extends IBaseResource> TypedBundleProvider<T> fromBundleProvider(IBundleProvider theBundleProvider) {
		return new TypedBundleProvider<>(theBundleProvider);
	}
	
	private TypedBundleProvider(IBundleProvider theInnerProvider) {
		myInnerProvider = checkNotNull(theInnerProvider);
	}

	@Override
	public IPrimitiveType<Date> getPublished() {
		return myInnerProvider.getPublished();
	}

	@Override
	public List<IBaseResource> getResources(int theFromIndex, int theToIndex) {
		return myInnerProvider.getResources(theFromIndex, theToIndex);
	}

	@Override
	public String getUuid() {
		return myInnerProvider.getUuid();
	}

	@Override
	public Integer preferredPageSize() {
		return myInnerProvider.preferredPageSize();
	}

	@Override
	public Integer size() {
		return myInnerProvider.size();
	}

	@SuppressWarnings("unchecked")
	public List<T> getResourcesTyped(int theFromIndex, int theToIndex) {
		return myInnerProvider.getResources(theFromIndex, theToIndex).stream().map(x -> (T)x).collect(Collectors.toList());
	}

	public List<T> getAllResourcesTyped() {
		List<T> retval = new ArrayList<>();

		Integer size = size();
		if (size == null) {
			throw new ConfigurationException("Attempt to request all resources from an asynchronous search result.  The SearchParameterMap for this search probably should have been synchronous.");
		}
		if (size > 0) {
			retval.addAll(getResourcesTyped(0, size));
		}
		return retval;
	}
}
