package org.opencds.cqf.ruler.cdshooks;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.cr.common.HapiLibrarySourceProvider;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.util.BundleUtil;
import org.cqframework.cql.cql2elm.LibraryContentType;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.serializing.jackson.CqlLibraryReaderProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Interceptor
public class CDSHooksTransactionInterceptor {

	private Map<String, Library> libraryCache;
	private Map<VersionedIdentifier, List<Code>> valueSetCache;
	private HapiLibrarySourceProvider sourceProvider;
	private CqlLibraryReaderProvider elmReaderProvider;
	private FhirContext fhirContext = FhirContext.forR4Cached();

	public CDSHooksTransactionInterceptor(DaoRegistry daoRegistry) {
		this.sourceProvider = new HapiLibrarySourceProvider(daoRegistry);
		this.elmReaderProvider = new CqlLibraryReaderProvider();
		libraryCache = new HashMap<>();
		valueSetCache = new HashMap<>();
	}

	@Hook(Pointcut.STORAGE_TRANSACTION_PROCESSED)
	public void updateCache(RequestDetails requestDetails) throws IOException {
		// TODO: Ensure the transaction succeeded - this could be smarter by checking the operation outcome code (e.g. SUCCESSFUL_UPDATE_NO_CHANGE)
		if (requestDetails.getResource() instanceof IBaseBundle) {
			for (var vs : BundleUtil.toListOfResourcesOfType(fhirContext, (IBaseBundle) requestDetails.getResource(), ValueSet.class)) {
				VersionedIdentifier identifier = new VersionedIdentifier().withId(vs.getIdElement().getIdPart());
				valueSetCache.put(identifier, vs.getExpansion().getContains().stream().map(
					e -> new Code().withCode(e.getCode()).withSystem(e.getSystem())
				).collect(Collectors.toList()));
			}
			var libraries =  BundleUtil.toListOfResourcesOfType(fhirContext, (IBaseBundle) requestDetails.getResource(), org.hl7.fhir.r4.model.Library.class);
			for (var library : libraries) {
				String id = library.getIdElement().getIdPart();
				InputStream is;
				for (var contentType : new LibraryContentType[]{LibraryContentType.XML, LibraryContentType.JSON}) {
					is = sourceProvider.getLibraryContent(new org.hl7.elm.r1.VersionedIdentifier().withId(id), contentType);
					if (is != null) {
						libraryCache.put(id, elmReaderProvider.create(contentType.mimeType()).read(is));
					}
				}
			}
		}
	}

	public Map<String, Library> getLibraryCache() {
		return libraryCache;
	}

	public void setLibraryCache(Map<String, Library> libraryCache) {
		this.libraryCache = libraryCache;
	}

	public Map<VersionedIdentifier, List<Code>> getValueSetCache() {
		return valueSetCache;
	}

	public void setValueSetCache(Map<VersionedIdentifier, List<Code>> valueSetCache) {
		this.valueSetCache = valueSetCache;
	}
}
