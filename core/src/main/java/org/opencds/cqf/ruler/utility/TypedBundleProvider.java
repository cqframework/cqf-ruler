package org.opencds.cqf.ruler.utility;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import ca.uhn.fhir.rest.api.server.IBundleProvider;

public class TypedBundleProvider<T extends IBaseResource> implements IBundleProvider {

	private IBundleProvider myInnerProvider;

	public static <T extends IBaseResource> TypedBundleProvider<T> fromBundleProvider(
			IBundleProvider theBundleProvider) {
		return new TypedBundleProvider<>(theBundleProvider);
	}

	private TypedBundleProvider(IBundleProvider theInnerProvider) {
		checkState(theInnerProvider.getNextPageId() == null,
				"TypedBundleProvider does not support paging and theInnerProvider has a next page.");
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

	@Override
	public List<IBaseResource> getAllResources() {
		var size = size();
		if (size == null) {
			return getResources(0, Integer.MAX_VALUE);
		} else {
			return getResources(0, size);
		}
	}

	@SuppressWarnings("unchecked")
	public List<T> getResourcesTyped(int theFromIndex, int theToIndex) {
		return myInnerProvider.getResources(theFromIndex, theToIndex).stream().map(x -> (T) x)
				.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	public List<T> getAllResourcesTyped() {
		return myInnerProvider.getAllResources().stream().map(x -> (T) x).collect(Collectors.toList());
	}

	/**
	 * Returns exactly one Resource. Throws an error if zero or more than one
	 * resource is found or if zero resources are found
	 * 
	 * @return the Resource found.
	 */
	public T single() {
		List<T> resources = getResourcesTyped(0, 2);
		checkState(!resources.isEmpty(), "No resources found");
		checkState(resources.size() == 1, "More than one resource found");
		return resources.get(0);
	}

	/**
	 * Returns the first Resource found, or null if no resources are found.
	 * 
	 * @return the first Resource found or null
	 */
	public T firstOrNull() {
		List<T> resources = getResourcesTyped(0, 1);

		if (resources.isEmpty()) {
			return null;
		}

		return resources.get(0);
	}
}
