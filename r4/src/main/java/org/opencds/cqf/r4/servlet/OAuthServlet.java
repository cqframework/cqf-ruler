package org.opencds.cqf.r4.servlet;

import ca.uhn.fhir.context.FhirContext;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.http.entity.ContentType;
import org.opencds.cqf.cds.discovery.DiscoveryResolutionR4;
import org.opencds.cqf.common.config.HapiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

public class OAuthServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(CdsHooksServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.info(request.getRequestURI());
        if (request.getRequestURL().toString().endsWith(".well-known/smart-configuration")) {
//            set up json response

            this.setAccessControlHeaders(response);
            response.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
            response.getWriter().println(new GsonBuilder().setPrettyPrinting().create().toJson(getServices()));
        }
    }

    private JsonObject getServices() {
        return new DiscoveryResolutionR4(FhirContext.forR4().newRestfulGenericClient(HapiProperties.getServerAddress()))
                .resolve().getAsJson();
    }

    private void setAccessControlHeaders(HttpServletResponse resp) {
        if (HapiProperties.getCorsEnabled()) {
            resp.setHeader("Access-Control-Allow-Origin", HapiProperties.getCorsAllowedOrigin());
            resp.setHeader("Access-Control-Allow-Methods",
                    String.join(", ", Arrays.asList("GET", "HEAD", "POST", "OPTIONS")));
            resp.setHeader("Access-Control-Allow-Headers", String.join(", ", Arrays.asList("x-fhir-starter", "Origin",
                    "Accept", "X-Requested-With", "Content-Type", "Authorization", "Cache-Control")));
            resp.setHeader("Access-Control-Expose-Headers",
                    String.join(", ", Arrays.asList("Location", "Content-Location")));
            resp.setHeader("Access-Control-Max-Age", "86400");
        }
    }

}
