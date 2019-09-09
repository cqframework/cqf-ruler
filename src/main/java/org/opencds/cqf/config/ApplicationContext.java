package org.opencds.cqf.config;

import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

public class ApplicationContext extends AnnotationConfigWebApplicationContext
{
    public ApplicationContext()
    {
        register(FhirServerConfigR4.class, FhirServerConfigCommon.class);
    }

}