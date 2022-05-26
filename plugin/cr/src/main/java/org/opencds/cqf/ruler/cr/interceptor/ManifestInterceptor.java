package org.opencds.cqf.ruler.cr.interceptor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.opencds.cqf.ruler.behavior.DaoRegistryUser;
import org.opencds.cqf.ruler.utility.Canonicals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

@Interceptor
public class ManifestInterceptor implements org.opencds.cqf.ruler.api.Interceptor, DaoRegistryUser {
	private final Logger ourLog = LoggerFactory.getLogger(ManifestInterceptor.class);

	@Autowired
	private DaoRegistry myDaoRegistry;

	@Override
	public DaoRegistry getDaoRegistry() {
		return myDaoRegistry;
	}

	@Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
	public void interceptManifest(RequestDetails theRequestDetails) {
		if (theRequestDetails != null) {
			String manifest = theRequestDetails.getHeader("X-Manifest");
			ourLog.info("Request with manifest: {}", manifest);

			if(manifest != null) {
				processManifest(manifest, theRequestDetails);
			}

		}
	}

	//https://github.com/HL7/Content-Management-Infrastructure-IG/blob/main/input/pages/version-manifest.md#x-manifest-header
	private void processManifest(String manifest, RequestDetails theRequestDetails) {
		Library assetCollectionManifest = read(new IdType(manifest), theRequestDetails);
		if (assetCollectionManifest != null) {
			ourLog.info("Manifest library found : {}", assetCollectionManifest.getUrl());

			Map<String, String> urlVersionManifestMap = new HashMap<>();
			populateResourceVersionMap(assetCollectionManifest, urlVersionManifestMap);
			theRequestDetails.getUserData().put("manifest", urlVersionManifestMap);
		}
	}

	private Map<String, String> populateResourceVersionMap(Library library, Map<String, String> resourceVersionMap) {
		if (library.hasRelatedArtifact()) {
			library.getRelatedArtifact().forEach(item -> {
				String version = Canonicals.getVersion(item.getResource());
				String url = Canonicals.getUrl(item.getResource());

				if (StringUtils.isNotBlank(url)) {
					if (StringUtils.isNotBlank(version)) {
						resourceVersionMap.put(url, version);
					}
				}
			});
		}
		return resourceVersionMap;
	}

	@Override
	public FhirContext getFhirContext() {
		return DaoRegistryUser.super.getFhirContext();
	}
}
