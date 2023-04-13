package org.opencds.cqf.ruler.cr;

import org.opencds.cqf.ruler.cr.repo.RulerRepository;

import ca.uhn.fhir.rest.api.server.RequestDetails;

@FunctionalInterface
public interface IRepositoryFactory {
	RulerRepository create(RequestDetails theRequestDetails);
}
