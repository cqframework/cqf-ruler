package org.opencds.cqf.ruler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletException;

import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.opencds.cqf.ruler.api.Interceptor;
import org.opencds.cqf.ruler.api.MetadataExtender;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.capability.RulerJpaCapabilityStatementProvider;
import org.opencds.cqf.ruler.capability.RulerJpaConformanceProviderDstu2;
import org.opencds.cqf.ruler.capability.RulerJpaConformanceProviderDstu3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.jpa.starter.BaseJpaRestfulServer;
import ca.uhn.fhir.model.dstu2.resource.Conformance;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;

@Import(AppProperties.class)
public class Server extends BaseJpaRestfulServer {

    private static Logger log = LoggerFactory.getLogger(Server.class);

    @Autowired
    DaoConfig daoConfig;
    @Autowired
    ISearchParamRegistry searchParamRegistry;

    @SuppressWarnings("rawtypes")
    @Autowired
    IFhirSystemDao fhirSystemDao;

    @Autowired
    private IValidationSupport myValidationSupport;

    @Autowired
    AppProperties myAppProperties;

    @Autowired
    ApplicationContext myApplicationContext;

    private static final long serialVersionUID = 1L;

    public Server() {
        super();
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void initialize() throws ServletException {
        super.initialize();

        log.info("Loading metadata extenders from plugins");
        Map<String, MetadataExtender> extenders = myApplicationContext.getBeansOfType(MetadataExtender.class);
        for (MetadataExtender o : extenders.values()) {
            log.info("Found {} extender", o.getClass().getName());
        }

        FhirVersionEnum fhirVersion = fhirSystemDao.getContext().getVersion().getVersion();
        if (fhirVersion == FhirVersionEnum.DSTU2) {
            List<MetadataExtender<Conformance>> extenderList = extenders.values().stream()
                    .map(x -> (MetadataExtender<Conformance>) x).collect(Collectors.toList());
            RulerJpaConformanceProviderDstu2 confProvider = new RulerJpaConformanceProviderDstu2(this, fhirSystemDao,
                    daoConfig, extenderList);
            confProvider.setImplementationDescription("CQF RULER DSTU2 Server");
            setServerConformanceProvider(confProvider);
        } else {
            if (fhirVersion == FhirVersionEnum.DSTU3) {
                List<MetadataExtender<CapabilityStatement>> extenderList = extenders.values().stream()
                        .map(x -> (MetadataExtender<CapabilityStatement>) x).collect(Collectors.toList());
                RulerJpaConformanceProviderDstu3 confProvider = new RulerJpaConformanceProviderDstu3(this,
                        fhirSystemDao, daoConfig, searchParamRegistry, extenderList);
                confProvider.setImplementationDescription("CQF RULER DSTU3 Server");
                setServerConformanceProvider(confProvider);
            } else if (fhirVersion == FhirVersionEnum.R4) {
                List<MetadataExtender<IBaseConformance>> extenderList = extenders.values().stream()
                        .map(x -> (MetadataExtender<IBaseConformance>) x).collect(Collectors.toList());
                RulerJpaCapabilityStatementProvider confProvider = new RulerJpaCapabilityStatementProvider(this,
                        fhirSystemDao, daoConfig, searchParamRegistry, myValidationSupport, extenderList);
                confProvider.setImplementationDescription("CQF RULER R4 Server");
                setServerConformanceProvider(confProvider);
            } else if (fhirVersion == FhirVersionEnum.R5) {
                List<MetadataExtender<IBaseConformance>> extenderList = extenders.values().stream()
                        .map(x -> (MetadataExtender<IBaseConformance>) x).collect(Collectors.toList());
                RulerJpaCapabilityStatementProvider confProvider = new RulerJpaCapabilityStatementProvider(this,
                        fhirSystemDao, daoConfig, searchParamRegistry, myValidationSupport, extenderList);
                confProvider.setImplementationDescription("CQF RULER R5 Server");
                setServerConformanceProvider(confProvider);
            } else {
                throw new IllegalStateException();
            }
        }

        log.info("Loading operation providers from plugins");
        Map<String, OperationProvider> providers = myApplicationContext.getBeansOfType(OperationProvider.class);
        for (OperationProvider o : providers.values()) {
            log.info("Registering {}", o.getClass().getName());
            this.registerProvider(o);
        }

        log.info("Loading interceptors from plugins");
        Map<String, Interceptor> interceptors = myApplicationContext.getBeansOfType(Interceptor.class);
        for (Interceptor o : interceptors.values()) {
            log.info("Registering {} interceptor", o.getClass().getName());
            this.registerInterceptor(o);
        }
    }
}
