package org.opencds.cqf.config;

import ca.uhn.fhir.jpa.provider.BaseJpaProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RequestInterceptor implements WebRequestInterceptor {

    private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(RequestInterceptor.class);

    @Override
    public void afterCompletion(@Nullable WebRequest request, @Nullable Exception exception) throws Exception {
        org.slf4j.MDC.remove(BaseJpaProvider.REMOTE_ADDR);
        org.slf4j.MDC.remove(BaseJpaProvider.REMOTE_UA);
    }

    @Override
    public void postHandle(@Nullable WebRequest request, @Nullable ModelMap map) throws Exception {
        // nothing
    }

    @Override
    public void preHandle(@Nonnull WebRequest request) throws Exception {
        String[] xffArr = request.getHeaderValues("x-forwarded-for");
        StringBuilder builder = new StringBuilder();
        for (String enums : makeSafe(xffArr)) {
            if (builder.length() > 0) {
                builder.append(" / ");
            }
            builder.append(enums);
        }

        String xff = builder.toString();
        org.slf4j.MDC.put(BaseJpaProvider.REMOTE_ADDR, xff);

        String userAgent = StringUtils.defaultString(request.getHeader("user-agent"));
        org.slf4j.MDC.put(BaseJpaProvider.REMOTE_UA, userAgent);

        ourLog.trace("User agent is: {}", userAgent);
    }

    private String[] makeSafe(String[] refugee) {
        return refugee == null ? new String[]{} : refugee;
    }
}
