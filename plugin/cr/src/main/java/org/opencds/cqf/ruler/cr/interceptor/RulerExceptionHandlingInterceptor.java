package org.opencds.cqf.ruler.cr.interceptor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import org.apache.commons.lang3.StringUtils;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.opencds.cqf.ruler.behavior.DaoRegistryUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Interceptor
public class RulerExceptionHandlingInterceptor implements org.opencds.cqf.ruler.api.Interceptor, DaoRegistryUser {
	private final Logger myLog = LoggerFactory.getLogger(RulerExceptionHandlingInterceptor.class);

	@Autowired
	private DaoRegistry myDaoRegistry;

	@Override
	public DaoRegistry getDaoRegistry() {
		return myDaoRegistry;
	}

	@Override
	public FhirContext getFhirContext() {
		return DaoRegistryUser.super.getFhirContext();
	}

	@Hook(Pointcut.SERVER_HANDLE_EXCEPTION)
	public boolean handleException(RequestDetails theRequestDetails, BaseServerResponseException theException,
		HttpServletRequest theServletRequest, HttpServletResponse theServletResponse) throws IOException {

		IBaseOperationOutcome operationOutcome = theException.getOperationOutcome();
		if(operationOutcome == null || theException instanceof AuthenticationException) {
			throw theException;
		} else {
			Throwable causedBy = getCause(theException);
			String actualCause = causedBy.getMessage();

			if (StringUtils.isNotBlank(actualCause)) {
				if (getFhirContext().getVersion().getVersion() == FhirVersionEnum.R4) {
					org.hl7.fhir.r4.model.OperationOutcome outcome = (org.hl7.fhir.r4.model.OperationOutcome) operationOutcome;
					if (outcome != null && !outcome.getIssue().isEmpty()) {
						org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent comp = outcome.getIssue().get(0);
						if (comp != null && comp.getDiagnostics() != null &&
							comp.getDiagnostics().contains(actualCause)) {
							comp.setDiagnostics(actualCause);
							System.out.println("actual cause:" + actualCause);
							operationOutcome = outcome;
						}
					}
				} else if (getFhirContext().getVersion().getVersion() == FhirVersionEnum.DSTU3) {
					org.hl7.fhir.dstu3.model.OperationOutcome outcome = (org.hl7.fhir.dstu3.model.OperationOutcome) operationOutcome;
					if (!outcome.getIssue().isEmpty()) {
						org.hl7.fhir.dstu3.model.OperationOutcome.OperationOutcomeIssueComponent comp = outcome.getIssue().get(0);
						if (comp != null && comp.getDiagnostics() != null &&
							comp.getDiagnostics().contains(actualCause)) {
							comp.setDiagnostics(actualCause);
							operationOutcome = outcome;
						}
					}
				}
			}

			theServletResponse.setStatus(theException.getStatusCode());
			theServletResponse.setContentType("text/json");
			theServletResponse.getWriter().append(
				getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(operationOutcome));
			theServletResponse.getWriter().close();
		}
		return false;
	}

	private Throwable getCause(Throwable e) {
		Throwable cause = null;
		Throwable result = e;

		while (null != (cause = result.getCause()) && (result != cause)) {
			result = cause;
		}
		return result;
	}
}
