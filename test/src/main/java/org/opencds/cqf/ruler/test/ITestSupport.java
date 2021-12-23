package org.opencds.cqf.ruler.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Charsets;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.parser.IParser;

public interface ITestSupport {

	default IBaseResource loadResource(String theLocation, FhirContext theFhirContext, DaoRegistry theDaoRegistry)
			throws IOException {
		String json = stringFromResource(theLocation);
		return loadResource("json", json, theFhirContext, theDaoRegistry);
	}

	@SuppressWarnings("unchecked")
	default IBaseResource loadResource(String encoding, String resourceString, FhirContext theFhirContext,
			DaoRegistry theDaoRegistry) throws IOException {
		IParser parser;
		switch (encoding.toLowerCase()) {
			case "json":
				parser = theFhirContext.newJsonParser();
				break;
			case "xml":
				parser = theFhirContext.newXmlParser();
				break;
			default:
				throw new RuntimeException(
						String.format("Expected encoding xml, or json.  %s is not a valid encoding", encoding));
		}
		IBaseResource resource = parser.parseResource(resourceString);
		if (theDaoRegistry == null) {
			return resource;
		}
		IFhirResourceDao<IBaseResource> dao = theDaoRegistry.getResourceDao(resource.getIdElement().getResourceType());
		if (dao == null) {
			return null;
		} else {
			dao.update(resource);
			return resource;
		}
	}

	default String stringFromResource(String theLocation) throws IOException {
		InputStream is = null;
		if (theLocation.startsWith(File.separator)) {
			is = new FileInputStream(theLocation);
		} else {
			DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
			Resource resource = resourceLoader.getResource(theLocation);
			is = resource.getInputStream();
		}
		return IOUtils.toString(is, Charsets.UTF_8);
	}

	public default Map<String, IBaseResource> uploadTests(String testDirectory, FhirContext fhirContext, DaoRegistry daoRegistry) throws URISyntaxException, IOException {
		URL url = this.getClass().getResource(testDirectory);
		File testDir = new File(url.toURI());
		return uploadTests(testDir.listFiles(), fhirContext, daoRegistry);
	}

	public default Map<String, IBaseResource> uploadTests(File[] files, FhirContext fhirContext, DaoRegistry daoRegistry) throws IOException {
		Map<String, IBaseResource> resources = new HashMap<String, IBaseResource>();
		for (File file : files) {
			// depth first
			if (file.isDirectory()) {
				resources.putAll(uploadTests(file.listFiles(), fhirContext, daoRegistry));
			}
		}
		for (File file : files) {
			if (file.isFile()) {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(new FileInputStream(file.getAbsolutePath())));
				String resourceString = reader.lines().collect(Collectors.joining(System.lineSeparator()));
				reader.close();
				IBaseResource resource = loadResource(FilenameUtils.getExtension(file.getAbsolutePath()), resourceString,
						fhirContext, daoRegistry);
				resources.put(resource.getIdElement().getIdPart(), resource);
			}
		}
		return resources;
	}

	@SuppressWarnings("unchecked")
	public default Object loadTransaction(
		DaoRegistry theDaoRegistry, String theLocation, FhirContext theFhirContext)
		throws IOException {
		String json = stringFromResource(theLocation);
		IBaseBundle resource = (IBaseBundle)theFhirContext.newJsonParser().parseResource(json);
		Object result = theDaoRegistry.getSystemDao().transaction(new SystemRequestDetails(), resource);
		return result;
	}
}
