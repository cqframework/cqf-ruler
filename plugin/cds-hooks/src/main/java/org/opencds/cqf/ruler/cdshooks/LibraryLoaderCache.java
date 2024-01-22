package org.opencds.cqf.ruler.cdshooks;

import ca.uhn.fhir.cr.common.HapiLibrarySourceProvider;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.cache.IResourceChangeEvent;
import ca.uhn.fhir.jpa.cache.IResourceChangeListener;
import ca.uhn.fhir.jpa.cache.ResourceChangeEvent;
import org.cqframework.cql.cql2elm.LibraryContentType;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.serializing.jackson.CqlLibraryReaderProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LibraryLoaderCache implements IResourceChangeListener {
	private static final Logger logger = LoggerFactory.getLogger(LibraryLoaderCache.class);
	private HapiLibrarySourceProvider sourceProvider;
	private CqlLibraryReaderProvider elmReaderProvider;
	private Map<String, Library> libraryCache;

	public LibraryLoaderCache(DaoRegistry daoRegistry) {
		this.sourceProvider = new HapiLibrarySourceProvider(daoRegistry);
		this.elmReaderProvider = new CqlLibraryReaderProvider();
		this.libraryCache = new HashMap<>();
	}

	public Map<String, Library> getLibraryCache() {
		return libraryCache;
	}

	@Override
	public void handleInit(Collection<IIdType> collection) {
		handleChange(ResourceChangeEvent.fromCreatedUpdatedDeletedResourceIds(new ArrayList<>(collection),
			Collections.emptyList(), Collections.emptyList()));
	}

	@Override
	public void handleChange(IResourceChangeEvent iResourceChangeEvent) {
		if (iResourceChangeEvent == null)
			return;
		if (iResourceChangeEvent.getCreatedResourceIds() != null
			&& !iResourceChangeEvent.getCreatedResourceIds().isEmpty()) {
			insert(iResourceChangeEvent.getCreatedResourceIds());
		}
		if (iResourceChangeEvent.getUpdatedResourceIds() != null
			&& !iResourceChangeEvent.getUpdatedResourceIds().isEmpty()) {
			update(iResourceChangeEvent.getUpdatedResourceIds());
		}
		if (iResourceChangeEvent.getDeletedResourceIds() != null
			&& !iResourceChangeEvent.getDeletedResourceIds().isEmpty()) {
			delete(iResourceChangeEvent.getDeletedResourceIds());
		}
	}

	private void getLibraryElmContent(String id) throws IOException {
		InputStream is;
		// TODO: Read CQL content
		for (var contentType : new LibraryContentType[]{LibraryContentType.XML, LibraryContentType.JSON}) {
			is = sourceProvider.getLibraryContent(new VersionedIdentifier().withId(id), contentType);
			if (is != null) {
				libraryCache.put(id, elmReaderProvider.create(contentType.mimeType()).read(is));
				return;
			}
		}
	}

	private void insert(List<IIdType> createdIds) {
		for (IIdType id : createdIds) {
			try {
				getLibraryElmContent(id.getIdPart());
			} catch (Exception e) {
				logger.info(String.format("Failed to find ELM content for %s", id.getIdPart()));
			}
		}
	}

	private void update(List<IIdType> updatedIds) {
		try {
			delete(updatedIds);
			insert(updatedIds);
		} catch (Exception e) {
			logger.info(String.format("Failed to update ELM content for %s", updatedIds));
		}
	}

	private void delete(List<IIdType> deletedIds) {
		for (IIdType id : deletedIds) {
			libraryCache.remove(id.getIdPart());
		}
	}
}
