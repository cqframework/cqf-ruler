package org.opencds.cqf.ruler.security.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@Interceptor
public class BasicAuthenticationInterceptor implements org.opencds.cqf.ruler.api.Interceptor {
	private final Logger myLog = LoggerFactory.getLogger(BasicAuthenticationInterceptor.class);


	@Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
	public boolean incomingRequestPostProcessed(RequestDetails theRequestDetails, HttpServletRequest theRequest,
															  HttpServletResponse theResponse) throws AuthenticationException {
		String authHeader = theRequest.getHeader("Authorization");

		System.out.println("incoming request");
		// The format of the header must be:
		// Authorization: Basic [base64 of username:password]
		if (authHeader == null || authHeader.startsWith("Basic ") == false) {
			throw new AuthenticationException(642 + "Missing or invalid Authorization header");
		}

		String base64 = authHeader.substring("Basic ".length());
		String base64decoded = new String(Base64.decodeBase64(base64));
		String[] parts = base64decoded.split(":");

		String username = parts[0];
		String password = parts[1];

		/*
		 * Here we test for a hardcoded username & password. This is
		 * not typically how you would implement this in a production
		 * system of course..
		 */
		if (!username.equals("someuser") || !password.equals("thepassword")) {
			throw new AuthenticationException(643 + "Invalid username or password");
		}

		// Return true to allow the request to proceed
		return true;
	}

}
