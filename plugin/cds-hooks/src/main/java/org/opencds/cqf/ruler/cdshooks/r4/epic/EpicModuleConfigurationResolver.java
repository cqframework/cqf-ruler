package org.opencds.cqf.ruler.cdshooks.r4.epic;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.util.BundleUtil;
import ca.uhn.fhir.util.UrlUtil;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.jetbrains.annotations.NotNull;
import org.opencds.cqf.ruler.cdshooks.r4.CdsHooksUtil;
import org.opencds.cqf.ruler.cdshooks.request.CdsHooksRequest;
import org.openjdk.jol.info.GraphLayout;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class EpicModuleConfigurationResolver {
	private static final String PATIENT = "Patient/{{context.patientId}}";
	private static final String ACTIVE_MEDICATION_ORDERS = "MedicationRequest?patient={{context.patientId}}&date=ge{{today() - 1 year - 1 month - 1 day}}&status=active,completed,stopped&category=community&intent=order&_include=MedicationRequest:medication";
	private static final String ACTIVE_CATEGORIZED_CONDITIONS = "Condition?patient={{context.patientId}}&category=health-concern,problem-list-item&clinical-status=active";
	private static final String UDS_LABS_POST = "Observation?subject={{context.patientId}}&category=laboratory&date=ge{{today() - 1 year}}";

	private static final List<String> URL_LIST = Arrays.asList(PATIENT, ACTIVE_MEDICATION_ORDERS, ACTIVE_CATEGORIZED_CONDITIONS, UDS_LABS_POST);

	private final IGenericClient prefetchClient;
	private final String patientId;
	private final List<String> medicationIds;

	// Thread-safe map for performance stats
	private final Map<String, MCLQueryResult> queryResultMap;

	private final FhirContext fhirContext;

	/**
	 * A shared executor so we don't create a new pool per request.
	 * Adjust the pool size as appropriate for your environment.
	 */
	private static final ExecutorService SHARED_EXECUTOR = Executors.newFixedThreadPool(
		8, // or some other sensible upper bound
		new ThreadFactory() {
			private final ThreadFactory delegate = Executors.defaultThreadFactory();
			@Override
			public Thread newThread(@NotNull Runnable r) {
				var t = delegate.newThread(r);
				t.setName("ModuleConfigurationResolver-PrefetchPool-" + t.getId());
				return t;
			}
		}
	);

	public EpicModuleConfigurationResolver(FhirContext fhirContext, Endpoint prefetchEndpoint, CdsHooksRequest request) {
		this.fhirContext = fhirContext;
		this.queryResultMap = new ConcurrentHashMap<>();

		// Initialize client
		this.prefetchClient = fhirContext.newRestfulGenericClient(prefetchEndpoint.getAddress());
		if (prefetchEndpoint.getHeader() != null) {
			var headerInterceptor = new AdditionalRequestHeadersInterceptor();
			for (HeaderInfo header : getHeaderNameValuePairs(prefetchEndpoint.getHeader())) {
				headerInterceptor.addHeaderValue(header.getName(), header.getValue());
			}
			this.prefetchClient.registerInterceptor(headerInterceptor);
		}

		String rawPatientId;
		List<MedicationRequest> draftOrders;
		if (request instanceof CdsHooksRequest.OrderSign) {
			// Extract patientId (strip "Patient/" prefix if present)
			rawPatientId = ((CdsHooksRequest.OrderSign) request).context.patientId;
			this.patientId = rawPatientId.replace("Patient/", "");
			// Draft orders -> gather medication references
			draftOrders = CdsHooksUtil.getDraftOrders(((CdsHooksRequest.OrderSign) request).context.draftOrders);
		} else {
			// Assuming OrderSelect
			// Extract patientId (strip "Patient/" prefix if present)
			rawPatientId = ((CdsHooksRequest.OrderSelect) request).context.patientId;
			this.patientId = rawPatientId.replace("Patient/", "");
			// Draft orders
			draftOrders = CdsHooksUtil.getDraftOrders(((CdsHooksRequest.OrderSelect) request).context.draftOrders);
		}

		// Gather medication references
		this.medicationIds = draftOrders.stream()
			.filter(MedicationRequest::hasMedicationReference)
			.map(order -> order.getMedicationReference().getReference())
			.collect(Collectors.toList());
	}

	/**
	 * Build a prefetch bundle containing the resources from the “normalized” URLs,
	 * in parallel, but omit duplicates.
	 */
	public Bundle getPrefetchBundle() {
		// We'll store resources in a Bundle with type=COLLECTION
		// (it doesn't represent a single search result)
		var prefetchBundle = new Bundle().setType(Bundle.BundleType.COLLECTION);

		// Keep track of which resource IDs we've added
		var seenIds = new HashSet<String>();

		var qts = new QueryTokenSubstitution(patientId, URL_LIST);
		var urls = qts.substituteTokens();
		if (urls.isEmpty()) {
			return prefetchBundle;
		}
		if (medicationIds != null && !medicationIds.isEmpty()) {
			urls.addAll(medicationIds);
		}

		// 1) Submit concurrency tasks for each URL
		var futures = urls.stream()
			.map(url -> CompletableFuture.supplyAsync(() -> resourceFromUrl(url), SHARED_EXECUTOR))
			.collect(Collectors.toList());

		// 2) Gather all results
		var allResults = futures.stream()
			.map(CompletableFuture::join)
			.collect(Collectors.toList());

		// 3) Flatten them into a single (initial) list of resources
		var allFlattenedResources = new ArrayList<IBaseResource>();
		for (IBaseResource baseResource : allResults) {
			if (baseResource instanceof Bundle) {
				var subBundle = (Bundle) baseResource;
				var subResources = BundleUtil.toListOfResources(fhirContext, subBundle);
				allFlattenedResources.addAll(subResources);
			}
			else if (baseResource instanceof Resource) {
				allFlattenedResources.add(baseResource);
			}
		}

		// Add them (distinctly) to the prefetchBundle
		for (IBaseResource resource : allFlattenedResources) {
			addIfNotDuplicate((Resource) resource, prefetchBundle, seenIds);
		}

		return prefetchBundle;
	}

	/**
	 * A helper to add a single Resource to a Bundle only if we haven't seen its ID yet.
	 * If the resource has no ID, you can decide how you want to handle that (in this example,
	 * we treat it as "unique every time").
	 */
	private void addIfNotDuplicate(Resource resource, Bundle targetBundle, Set<String> seenIds) {
		var id = resource.getIdElement().toUnqualifiedVersionless().getValue();
		if (!StringUtils.isBlank(id)) {
			if (!seenIds.contains(id)) {
				seenIds.add(id);
				targetBundle.addEntry().setResource(resource);
			}
		} else {
			// No ID => we treat it as unique. If you prefer skipping them, you could do so here.
			targetBundle.addEntry().setResource(resource);
		}
	}

	/**
	 * Retrieve a single resource or search bundle from the FHIR server for the given URL,
	 * tracking execution time in performanceMap.
	 */
	public IBaseResource resourceFromUrl(String theUrl) {
		var startTime = System.currentTimeMillis();
		int count = 0;
		var queryResult = new MCLQueryResult();
		try {
			var parts = UrlUtil.parseUrl(theUrl);
			var resourceType = parts.getResourceType();
			if (StringUtils.isEmpty(resourceType)) {
				throw new InvalidRequestException(
					Msg.code(2383) + "Failed to resolve " + theUrl + ". Url does not start with a resource type."
				);
			}
			var resourceId = parts.getResourceId();
			var matchUrl = parts.getParams();

			if (resourceId != null) {
				// Read a specific resource by ID
				var resource =  prefetchClient.read().resource(resourceType).withId(resourceId).execute();
				if (resource != null) {
					queryResult.setBytes(GraphLayout.parseInstance(resource).totalSize());
					count++;
				}
				return resource;
			} else if (matchUrl != null) {
				// Perform a search
				var queryMap = UrlUtil.parseQueryString(matchUrl);

				// Transform to Map<String, List<String>>
				var whereMap = new HashMap<String, List<String>>();
				queryMap.forEach((key, valueArray) -> {
					// Convert String[] -> List<String>
					whereMap.put(key, Arrays.asList(valueArray));
				});
				var searchResult = searchWithPagination(resourceType, whereMap, queryResult);
				if (searchResult.hasEntry()) {
					count = searchResult.getEntry().size();
				}
				return searchResult;
			} else {
				throw new InvalidRequestException(
					Msg.code(2384) + "Unable to translate url " + theUrl + " into a resource or a bundle."
				);
			}
		} finally {
			var duration = System.currentTimeMillis() - startTime;
			queryResult.setDuration(duration);
			queryResult.setCount(count);
			queryResultMap.put(theUrl, queryResult);
		}
	}

	/**
	 * Demonstrates fetching all pages of a search, if the server returns a multi-page Bundle.
	 * Combines them all into a single Bundle to return.
	 */
	private Bundle searchWithPagination(String resourceType, Map<String, List<String>> queryMap, MCLQueryResult queryResult) {
		// Start the search
		var search = prefetchClient.search().forResource(resourceType);

		// Add all parameters
		for (Map.Entry<String, List<String>> entry : queryMap.entrySet()) {
			search.whereMap(Collections.singletonMap(entry.getKey(), entry.getValue()));
		}

		// Execute initial search
		var result = search.returnBundle(Bundle.class).execute();

		// Collect results across multiple pages
		var allResources = new ArrayList<>(BundleUtil.toListOfResources(fhirContext, result));
		var bytes = GraphLayout.parseInstance(allResources).totalSize();
		queryResult.setBytes(bytes);

		var current = result;
		while (current.getLink(Bundle.LINK_NEXT) != null) {
			current = prefetchClient.loadPage().next(current).execute();
			allResources.addAll(BundleUtil.toListOfResources(fhirContext, current));
		}

		// Combine into a single Bundle
		var combined = new Bundle();
		combined.setType(Bundle.BundleType.SEARCHSET);
		allResources.forEach(r -> combined.addEntry().setResource((Resource) r));

		return combined;
	}

	/**
	 * Convert a list of string headers (e.g. "Key: Value") into a list of HeaderInfo objects.
	 */
	protected List<HeaderInfo> getHeaderNameValuePairs(List<StringType> headers) {
		var headerNameValuePairs = new ArrayList<HeaderInfo>();
		for (StringType header : headers) {
			// Split on the first colon
			var parts = header.getValue().split("\\s*:\\s*", 2);
			if (parts.length == 2) {
				headerNameValuePairs.add(new HeaderInfo(parts[0], parts[1]));
			}
		}
		return headerNameValuePairs;
	}

	public Map<String, MCLQueryResult> getQueryResultMap() { return queryResultMap; }

	/**
	 * Optional: Shut down the shared executor if desired (e.g., on app shutdown).
	 */
	public static void shutdownExecutor() {
		SHARED_EXECUTOR.shutdown();
		// Possibly await termination, etc.
	}

	// Simple header info data class
	static class HeaderInfo {
		private final String name;
		private final String value;

		public HeaderInfo(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}
	}
}
