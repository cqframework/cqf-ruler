package org.opencds.cqf.ruler.server;

/* 
*  This file created from the HAPI FHIR JPA Server Starter project
*  https://github.com/hapifhir/hapi-fhir-jpaserver-starter 
*/

import ca.uhn.fhir.jpa.interceptor.validation.RepositoryValidatingInterceptor;

public interface IRepositoryValidationInterceptorFactory {
	RepositoryValidatingInterceptor buildUsingStoredStructureDefinitions();

	RepositoryValidatingInterceptor build();
}
