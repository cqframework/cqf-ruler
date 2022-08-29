package org.opencds.cqf.ruler.cql;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.opencds.cqf.ruler.behavior.DaoRegistryUser;
import org.opencds.cqf.ruler.utility.Libraries;
import org.opencds.cqf.ruler.utility.Searches;
import org.opencds.cqf.ruler.utility.Versions;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class JpaLibrarySourceProvider
		implements LibrarySourceProvider, DaoRegistryUser {
	protected final DaoRegistry daoRegistry;
	protected final RequestDetails requestDetails;

	public JpaLibrarySourceProvider(DaoRegistry daoRegistry) {
		this(daoRegistry, null);
	}

	public JpaLibrarySourceProvider(DaoRegistry daoRegistry, RequestDetails requestDetails) {
		this.daoRegistry = daoRegistry;
		this.requestDetails = requestDetails;
	}

	@Override
	public DaoRegistry getDaoRegistry() {
		return this.daoRegistry;
	}

	// TODO: Support loading ELM - For now consider that the LibraryLoader has some
	// caching built in
	@Override
	public InputStream getLibrarySource(VersionedIdentifier versionedIdentifier) {
		String name = versionedIdentifier.getId();
		String version = versionedIdentifier.getVersion();
		List<IBaseResource> libraries = search(getClass("Library"), Searches.byName(name), requestDetails).getAllResources();
		IBaseResource library = Versions.selectByVersion(libraries, version,
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
