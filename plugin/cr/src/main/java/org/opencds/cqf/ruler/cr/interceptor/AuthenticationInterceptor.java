package org.opencds.cqf.ruler.cr.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import org.apache.commons.lang3.StringUtils;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;


@Interceptor
public class AuthenticationInterceptor implements org.opencds.cqf.ruler.api.Interceptor {
	private final Logger myLog = LoggerFactory.getLogger(AuthenticationInterceptor.class);

	@Autowired
	private CrConfig crConfig;

	@Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
	public boolean incomingRequestPostProcessed(RequestDetails theRequestDetails, HttpServletRequest theRequest,
															  HttpServletResponse theResponse) throws AuthenticationException {
		if(crConfig.crProperties().getSecurityConfiguration().getEnabled()) {
			String authHeader = theRequest.getHeader("Authorization");

			myLog.info("incoming request intercepted");
			// The format of the header must be:
			// Authorization: Basic [base64 of username:password]
			if (authHeader == null || !authHeader.startsWith("Basic ")) {
				throw new AuthenticationException(642 + "Missing or invalid Authorization header");
			}

			String base64 = authHeader.substring("Basic ".length());
			String base64decoded = new String(Base64.getDecoder().decode(base64), UTF_8);
			String[] parts = base64decoded.split(":");

			if(parts.length <= 1) {
				throw new AuthenticationException(642 + "Missing or invalid Authorization header");
			}

			String username = parts[0];
			String password = parts[1];

			/*
			 * Here we test for a hardcoded username & password. This is
			 * not typically how you would implement this in a production
			 * system of course..
			 */

			if (!StringUtils.equals(username.trim(), crConfig.crProperties().getSecurityConfiguration().getUsername().trim()) ||
				!StringUtils.equals(password.trim(), crConfig.crProperties().getSecurityConfiguration().getPassword().trim())) {
				throw new AuthenticationException(643 + "Invalid username or password");
			}

			myLog.info("Authorization successful");
		}

		// Return true to allow the request to proceed
		return true;
	}

}
