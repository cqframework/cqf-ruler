package org.opencds.cqf.ruler.cql;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentType;
import org.opencds.cqf.ruler.behavior.DaoRegistryUser;
import org.opencds.cqf.ruler.utility.Libraries;
import org.opencds.cqf.ruler.utility.Searches;
import org.opencds.cqf.ruler.utility.Versions;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class JpaLibraryContentProvider
		implements LibraryContentProvider, DaoRegistryUser {
	protected final DaoRegistry daoRegistry;
	protected final RequestDetails requestDetails;

	public JpaLibraryContentProvider(DaoRegistry daoRegistry) {
		this(daoRegistry, null);
	}

	public JpaLibraryContentProvider(DaoRegistry daoRegistry, RequestDetails requestDetails) {
		this.daoRegistry = daoRegistry;
		this.requestDetails = requestDetails;
	}

	@Override
	public DaoRegistry getDaoRegistry() {
		return this.daoRegistry;
	}

	@Override
	public InputStream getLibraryContent(org.hl7.elm.r1.VersionedIdentifier libraryIdentifier,
			LibraryContentType libraryContentType) {
		// TODO: Support loading ELM - For now consider that the LibraryLoader has some
		// caching built in
		if (libraryContentType != LibraryContentType.CQL) {
			return null;
		}

		IBundleProvider libraries = search("Library", Searches.byName(libraryIdentifier.getId()), requestDetails);
		IBaseResource library = Versions.selectByVersion(libraries.getAllResources(), libraryIdentifier.getVersion(),
				Libraries::getVersion);

		if (library == null) {
			return null;
		}
		byte[] content = Libraries.getContent(library, "text/cql");
		if (content == null) {
			return null;
		}

		return new ByteArrayInputStream(content);
	}
}
