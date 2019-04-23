package org.opencds.cqf.config;

import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

public class ApplicationContext extends AnnotationConfigWebApplicationContext
{
    public ApplicationContext()
    {
        register(FhirServerConfigDstu3.class, FhirServerConfigCommon.class);
    }

}