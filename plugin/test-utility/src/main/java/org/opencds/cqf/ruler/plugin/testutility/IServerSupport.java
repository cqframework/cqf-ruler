package org.opencds.cqf.ruler.plugin.testutility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.google.common.base.Charsets;

import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.parser.IParser;

public interface  IServerSupport {

  default IBaseResource loadResource(String theLocation, FhirContext theFhirContext, DaoRegistry theDaoRegistry) throws IOException {
    String json = stringFromResource(theLocation);
    return loadResource("json", json, theFhirContext, theDaoRegistry);
  }

  @SuppressWarnings("unchecked")
  default IBaseResource loadResource(String encoding, String resourceString, FhirContext theFhirContext, DaoRegistry theDaoRegistry) throws IOException {
    IParser parser;
    switch (encoding.toLowerCase()) {
      case "json": parser = theFhirContext.newJsonParser(); break;
      case "xml": parser = theFhirContext.newXmlParser(); break;
      default: throw new RuntimeException(String.format("Expected encoding xml, or json.  %s is not a valid encoding", encoding));
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
}
