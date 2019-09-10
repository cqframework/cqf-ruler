package org.opencds.cqf.r4.config;

import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

public class ApplicationContext extends AnnotationConfigWebApplicationContext
{
    public ApplicationContext()
    {
        register(FhirServerConfigR4.class, FhirServerConfigCommon.class);
    }

}