package org.opencds.cqf.ruler;

import java.util.Map;

import javax.servlet.ServletException;

import org.opencds.cqf.ruler.api.OperationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.jpa.starter.BaseJpaRestfulServer;

@Import(AppProperties.class)
public class Server extends BaseJpaRestfulServer {

    private static Logger log = LoggerFactory.getLogger(Server.class);

    @Autowired
    AppProperties myAppProperties;

    @Autowired
    ApplicationContext myApplicationContext;

    private static final long serialVersionUID = 1L;

    public Server() {
        super();
    }

    @Override
    protected void initialize() throws ServletException {
        super.initialize();

        log.info("Loading operation providers from plugins");
        Map<String, OperationProvider> providers = myApplicationContext.getBeansOfType(OperationProvider.class);

        for (OperationProvider o : providers.values()){
            log.info("Registering {}", o.getClass().getName());
            this.registerProvider(o);

        }

        log.info("Loading interceptors from plugins");

    }
}
