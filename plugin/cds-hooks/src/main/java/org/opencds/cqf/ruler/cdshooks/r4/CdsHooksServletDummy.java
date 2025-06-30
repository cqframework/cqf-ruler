package org.opencds.cqf.ruler.cdshooks.r4;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.http.entity.ContentType;
import org.opencds.cqf.external.AppProperties;
import org.opencds.cqf.ruler.cdshooks.CdsServicesCache;
import org.opencds.cqf.ruler.cdshooks.response.ErrorHandling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configurable
public class CdsHooksServletDummy extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(CdsHooksServletDummy.class);

	private final AppProperties appProperties;
	private final CdsServicesCache cdsServicesCache;

	@Autowired public CdsHooksServletDummy(AppProperties appProperties, CdsServicesCache cdsServicesCache) {
		this.appProperties = appProperties;
		this.cdsServicesCache = cdsServicesCache;
	}

	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
		ErrorHandling.setAccessControlHeaders(resp, appProperties);
		resp.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
		resp.setHeader("X-Content-Type-Options", "nosniff");
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {

		logger.info(request.getRequestURI());
		if (!request.getRequestURL().toString().endsWith("/cds-services") &&
			!request.getRequestURL().toString().endsWith("/cds-services/")) {
			logger.error(request.getRequestURI());
			throw new ServletException("This servlet is not configured to handle GET requests other than /cds-services.");
		}

		ErrorHandling.setAccessControlHeaders(response, appProperties);
		response.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
		response.setHeader("Access-Control-Allow-Origin", "*");

		// Return the services JSON
		var servicesJson = new GsonBuilder().setPrettyPrinting().create().toJson(getServices());
		response.getWriter().println(servicesJson);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.getWriter().println(CdsHooksUtil.emptyCards());
	}

	private JsonObject getServices() {
		var services = new JsonObject();
		services.add("services", this.cdsServicesCache.getCdsServiceCache().get());
		return services;
	}
}
