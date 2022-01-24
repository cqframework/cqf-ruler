package org.opencds.cqf.ruler.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.ruler.utility.DaoRegistryUser;
import org.opencds.cqf.ruler.utility.FhirContextUser;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.parser.IParser;

public interface ResourceLoader extends FhirContextUser, DaoRegistryUser {

	default Object loadTransaction(String theLocation)
			throws IOException {
		IBaseBundle resource = (IBaseBundle)readResource(theLocation);
		return transaction(resource, new SystemRequestDetails());
	}

	default IBaseResource readResource(String theLocation)
			throws IOException {
		String resourceString = stringFromResource(theLocation);
		if (theLocation.endsWith("json")) {
			return parseResource("json", resourceString);
		}
		else {
			return parseResource("xml", resourceString);
		}
	}

	public default IBaseResource parseResource(String encoding, String resourceString) {
		IParser parser;
		switch (encoding.toLowerCase()) {
			case "json":
				parser = getFhirContext().newJsonParser();
				break;
			case "xml":
				parser = getFhirContext().newXmlParser();
				break;
			default:
				throw new IllegalArgumentException(
						String.format("Expected encoding xml, or json.  %s is not a valid encoding", encoding));
		}
		
		return parser.parseResource(resourceString);
	}

	default IBaseResource loadResource(String theLocation)
			throws IOException {
		String resourceString = stringFromResource(theLocation);
		if (theLocation.endsWith("json")) {
			return loadResource("json", resourceString);
		}
		else {
			return loadResource("xml", resourceString);
		}
	}

	default IBaseResource loadResource(String encoding, String resourceString) throws IOException {
		IBaseResource resource = parseResource(encoding, resourceString);
		if (getDaoRegistry() == null) {
			return resource;
		}

		update(resource);
		return resource;
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
		return IOUtils.toString(is, StandardCharsets.UTF_8);
	}

	default Map<String, IBaseResource> uploadTests(String testDirectory) throws URISyntaxException, IOException {
		URL url = this.getClass().getResource(testDirectory);
		File testDir = new File(url.toURI());
		if(!testDir.exists()) {
			throw new IllegalArgumentException(String.format("test directory %s does not exist.", testDirectory));
		}
		return uploadTests(testDir.listFiles());
	}

	default Map<String, IBaseResource> uploadTests(File[] files)
			throws IOException {
		Map<String, IBaseResource> resources = new HashMap<>();
		for (File file : files) {
			// depth first
			if (file.isDirectory()) {
				resources.putAll(uploadTests(file.listFiles()));
			}
		}
		for (File file : files) {
			if (file.isFile()) {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(new FileInputStream(file.getAbsolutePath()), StandardCharsets.UTF_8));
				String resourceString = reader.lines().collect(Collectors.joining(System.lineSeparator()));
				reader.close();
				IBaseResource resource = loadResource(FilenameUtils.getExtension(file.getAbsolutePath()), resourceString);
				resources.put(resource.getIdElement().getIdPart(), resource);
			}
		}
		return resources;
	}
}