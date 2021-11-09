package org.opencds.cqf.ruler;

import javax.servlet.ServletException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.jpa.starter.BaseJpaRestfulServer;

@Import(AppProperties.class)
public class Server extends BaseJpaRestfulServer {

    @Autowired
    AppProperties appProperties;

    private static final long serialVersionUID = 1L;

    public Server() {
        super();
    }

    @Override
    protected void initialize() throws ServletException {
        super.initialize();

        // Add your own customization here

    }
}
