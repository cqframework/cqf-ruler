package org.opencds.cqf.ruler.cr.interceptor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
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
				Map<String, String> urlVersionManifestMap = new HashMap<>();
				try {
					processManifest(manifest, theRequestDetails, urlVersionManifestMap);
				} catch (Exception e) {
					ourLog.info("Manifest processing failed : {}", e.getMessage());
				}
				if (!urlVersionManifestMap.isEmpty()) {
					theRequestDetails.getUserData().put("manifest", urlVersionManifestMap);
				}
			}

		}
	}

	//https://github.com/HL7/Content-Management-Infrastructure-IG/blob/main/input/pages/version-manifest.md#x-manifest-header
	private void processManifest(String manifest, RequestDetails theRequestDetails, Map<String, String> urlVersionManifestMap) {

		if (getFhirContext().getVersion().getVersion() == FhirVersionEnum.R4) {
			Library manifestLibrary = read(new IdType(manifest), theRequestDetails);
			if (manifestLibrary != null) {
				populateResourceVersionMapR4(manifestLibrary, urlVersionManifestMap);
			}
		} else if (getFhirContext().getVersion().getVersion() == FhirVersionEnum.DSTU3) {
			org.hl7.fhir.dstu3.model.Library manifestLibrary = read(new IdType(manifest), theRequestDetails);
			if (manifestLibrary != null) {
				populateResourceVersionMapDstu3(manifestLibrary, urlVersionManifestMap);
			}
		}
	}

	private void populateResourceVersionMapR4(Library library, Map<String, String> resourceVersionMap) {
		ourLog.info("Manifest library found : {}", library.getUrl());
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
	}

	private void populateResourceVersionMapDstu3(org.hl7.fhir.dstu3.model.Library library,
																			 Map<String, String> resourceVersionMap) {
		ourLog.info("Manifest library found : {}", library.getUrl());
		if (library.hasRelatedArtifact()) {
			library.getRelatedArtifact().forEach(item -> {
				String version = Canonicals.getVersion(item.getResource().getReference());
				String url = Canonicals.getUrl(item.getResource().getReference());

				if (StringUtils.isNotBlank(url)) {
					if (StringUtils.isNotBlank(version)) {
						resourceVersionMap.put(url, version);
					}
				}
			});
		}
	}

	@Override
	public FhirContext getFhirContext() {
		return DaoRegistryUser.super.getFhirContext();
	}
}
