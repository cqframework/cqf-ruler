package org.opencds.cqf.ruler.security.interceptor;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Base64;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.opencds.cqf.ruler.security.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;

@Interceptor
public class AuthenticationInterceptor implements org.opencds.cqf.ruler.api.Interceptor {
	private static final String METADATA_PATH = "/fhir/metadata";
	private final Logger myLog = LoggerFactory.getLogger(AuthenticationInterceptor.class);

	@Autowired
	private SecurityProperties securityProperties;

	@Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
	public boolean incomingRequestPostProcessed(RequestDetails theRequestDetails, HttpServletRequest theRequest,
			HttpServletResponse theResponse) throws AuthenticationException {
		// The METADATA_PATH is used as a probe in various deployment scenarios and
		// needs to be excluded to enable correct functionality of the probe(s) in the
		// case where basic auth is enabled.
		if (!securityProperties.getBasicAuth().isEnabled()
				|| theRequest.getRequestURI().equals(METADATA_PATH)) {
			return true;
		}

		String authHeader = theRequest.getHeader("Authorization");

		myLog.info("incoming request intercepted");
		// The format of the header must be:
		// Authorization: Basic [base64 of username:password]
		if (authHeader == null || !authHeader.startsWith("Basic ")) {
			throw new AuthenticationException("Missing or invalid Authorization header");
		}

		String base64 = authHeader.substring("Basic ".length());
		String base64decoded = new String(Base64.getDecoder().decode(base64), UTF_8);
		String[] parts = base64decoded.split(":");

		if (parts.length <= 1) {
			throw new AuthenticationException("Missing or invalid Authorization header");
		}

		String username = parts[0];
		String password = parts[1];

		if (!StringUtils.equals(username.trim(), securityProperties.getBasicAuth().getUsername().trim()) ||
				!StringUtils.equals(password.trim(), securityProperties.getBasicAuth().getPassword().trim())) {
			throw new AuthenticationException("Invalid username or password");
		}

		myLog.info("Authorization successful");

		// Return true to allow the request to proceed
		return true;
	}

}
