package org.opencds.cqf.ruler.cr.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Interceptor
public class RulerExceptionHandlingInterceptor implements org.opencds.cqf.ruler.api.Interceptor {
	private final Logger ourLog = LoggerFactory.getLogger(RulerExceptionHandlingInterceptor.class);

	@Hook(Pointcut.SERVER_HANDLE_EXCEPTION)
	public boolean handleException(
		RequestDetails theRequestDetails,
		BaseServerResponseException theException,
		HttpServletRequest theServletRequest,
		HttpServletResponse theServletResponse) throws IOException {


		theServletResponse.setStatus(theException.getStatusCode());

		// Provide a response ourself
		theServletResponse.setContentType("text/plain");
		theServletResponse.getWriter().append("Failed to process!");
		theServletResponse.getWriter().close();

		return false;
	}

}
