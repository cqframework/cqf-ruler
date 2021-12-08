package org.opencds.cqf.ruler.plugin.cql;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentType;
import org.opencds.cqf.ruler.plugin.utility.ResolutionUtilities;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class JpaLibraryContentProvider
	implements LibraryContentProvider, ResolutionUtilities, LibraryUtilities {
	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(LibraryContentProvider.class);

	private static Map<Class<? extends IBaseResource>, ContentFunctions> cachedContentFunctions = new HashMap<>();

	protected final IFhirResourceDao<?> libraryDao;
	protected final RequestDetails requestDetails;

	public JpaLibraryContentProvider(IFhirResourceDao<?> libraryDao) {
		this(libraryDao, null);
	}


	public JpaLibraryContentProvider(IFhirResourceDao<?> libraryDao, RequestDetails requestDetails) {
		this.libraryDao = libraryDao;
		this.requestDetails = requestDetails;
	}

	@Override
	public InputStream getLibraryContent(org.hl7.elm.r1.VersionedIdentifier libraryIdentifier,
			LibraryContentType libraryContentType) {
		// TODO: Support loading ELM - For now consider that the LibraryLoader has some caching built in
		if (libraryContentType != LibraryContentType.CQL) {
			return null;
		}

		IBaseResource library = this.resolveByNameAndVersion(this.libraryDao, libraryIdentifier.getId(), libraryIdentifier.getVersion(), this.requestDetails);
		if (library == null) {
			return null;
		}

		ContentFunctions cf = cachedContentFunctions.computeIfAbsent(libraryDao.getResourceType(), x -> this.getContentFunctions(this.libraryDao.getContext()));
		try {
			for (IBaseResource attachment : cf.getAttachments().apply(library)) {
				String contentType = cf.getContentType().apply(attachment);
				if (contentType != null && contentType.equals("text/cql")) {
					byte[] content = cf.getContent().apply(attachment);
					if (content != null) {
						return new ByteArrayInputStream(content);
					}
				}
			}
		} catch (Exception e) {
			ourLog.warn("Failed to parse Library source for VersionedIdentifier '" + libraryIdentifier + "'!"
				+ System.lineSeparator() + e.getMessage(), e);
		}

		return null;
	}
}
