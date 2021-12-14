package org.opencds.cqf.ruler.plugin.testutility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import com.google.common.base.Charsets;

import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;

/**
 * This interface provides test utility functions for resolving FHIR resources
 * from a location.
 * 
 */
public interface ResolutionUtilities {

	/**
	 * Returns the Resource at the specified location.
	 * 
	 * @param <ResourceType> an IBaseResource type
	 * @param theResourceDao the DaoRegistry to use for resolution
	 * @param theLocation    the location of the Resource to resolve
	 * @param theFhirContext the FhirContext to use to parse the resource
	 * @return the Resource at the specified location
	 * @throws IOException if the location does not exist or is unaccessible
	 */
	@SuppressWarnings("unchecked")
	public default <ResourceType extends IBaseResource> ResourceType resolveByLocation(
			DaoRegistry theResourceDao, String theLocation, FhirContext theFhirContext)
			throws IOException {
		String json = stringFromResource(theLocation);
		IBaseResource resource = theFhirContext.newJsonParser().parseResource(json);
		IFhirResourceDao<IBaseResource> dao = theResourceDao.getResourceDao(resource.getIdElement().getResourceType());
		if (dao == null) {
			return null;
		} else {
			dao.update(resource);
			return (ResourceType) resource;
		}
	}

	@SuppressWarnings("unchecked")
	public default Object transactionByLocation(
		DaoRegistry theDaoRegistry, String theLocation, FhirContext theFhirContext)
		throws IOException {
		String json = stringFromResource(theLocation);
		IBaseBundle resource = (IBaseBundle)theFhirContext.newJsonParser().parseResource(json);
		Object result = theDaoRegistry.getSystemDao().transaction(new SystemRequestDetails(), resource);
		return result;
	}

	/**
	 * Returns a String representation of the Resource
	 * at the specified location.
	 * 
	 * @param theLocation the location of the Resource to resolve
	 * @return the string representation of the Resource
	 * @throws IOException if the location does not exist or is unaccessible
	 */
	public default String stringFromResource(String theLocation) throws IOException {
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
