package org.opencds.cqf.ruler;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletException;

import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.opencds.cqf.external.AppProperties;
import org.opencds.cqf.external.BaseJpaRestfulServer;
import org.opencds.cqf.external.mdm.MdmConfig;
import org.opencds.cqf.ruler.api.CustomResourceRegisterer;
import org.opencds.cqf.ruler.api.Interceptor;
import org.opencds.cqf.ruler.api.MetadataExtender;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.capability.ExtensibleJpaCapabilityStatementProvider;
import org.opencds.cqf.ruler.capability.ExtensibleJpaConformanceProviderDstu2;
import org.opencds.cqf.ruler.capability.ExtensibleJpaConformanceProviderDstu3;
import org.opencds.cqf.ruler.config.BeanFinderConfig;
import org.opencds.cqf.ruler.config.ServerProperties;
import org.opencds.cqf.ruler.config.TesterUIConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.batch2.jobs.config.Batch2JobsConfig;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.batch2.JpaBatch2Config;
import ca.uhn.fhir.jpa.subscription.channel.config.SubscriptionChannelConfig;
import ca.uhn.fhir.jpa.subscription.match.config.SubscriptionProcessorConfig;
import ca.uhn.fhir.jpa.subscription.match.config.WebsocketDispatcherConfig;
import ca.uhn.fhir.jpa.subscription.submit.config.SubscriptionSubmitterConfig;
import ca.uhn.fhir.model.dstu2.resource.Conformance;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;

@Import({
		ServerProperties.class,
		SubscriptionSubmitterConfig.class,
		SubscriptionProcessorConfig.class,
		SubscriptionChannelConfig.class,
		WebsocketDispatcherConfig.class,
		MdmConfig.class,
		JpaBatch2Config.class,
		Batch2JobsConfig.class,
		TesterUIConfig.class,
		BeanFinderConfig.class })
public class Server extends BaseJpaRestfulServer {
	private static final long serialVersionUID = 1L;

	private static Logger log = LoggerFactory.getLogger(Server.class);

	@Autowired
	DaoConfig myDaoConfig;

	@Autowired
	ISearchParamRegistry mySearchParamRegistry;

	@SuppressWarnings("rawtypes")
	@Autowired
	IFhirSystemDao myFhirSystemDao;

	@Autowired
	private IValidationSupport myValidationSupport;

	@Autowired
	ApplicationContext applicationContext;

	@Autowired
	AppProperties myAppProperties;

	@Autowired
	ServerProperties myServerProperties;

	public Server() {
		super();
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void initialize() throws ServletException {
		super.initialize();

		log.info("Registering custom resources");
		Map<String, CustomResourceRegisterer> customerResourceRegisterer = applicationContext.getBeansOfType(CustomResourceRegisterer.class);
		for (CustomResourceRegisterer r : customerResourceRegisterer.values()) {
			log.info("Registering {}", r.getClass().getName());
			r.register(myFhirSystemDao.getContext());
			r.getResourceProviders(myFhirSystemDao.getContext()).forEach(provider ->
				registerProvider(provider));
//			Collection<ResourceBinding> col = getResourceBindings();
//			log.info(getResourceBindings().stream().anyMatch(rb -> rb.getResourceName().equalsIgnoreCase("ArtifactAssessment"))? "true": "false");
		}

		log.info("Loading operation providers from plugins");
		Map<String, OperationProvider> providers = applicationContext.getBeansOfType(OperationProvider.class);
		for (OperationProvider o : providers.values()) {
			log.info("Registering {}", o.getClass().getName());
			this.registerProvider(o);
		}

		log.info("Loading interceptors from plugins");
		Map<String, Interceptor> interceptors = applicationContext.getBeansOfType(Interceptor.class);
		for (Interceptor o : interceptors.values()) {
			log.info("Registering {} interceptor", o.getClass().getName());
			this.registerInterceptor(o);
		}

		log.info("Loading metadata extenders from plugins");
		Map<String, MetadataExtender> extenders = applicationContext.getBeansOfType(MetadataExtender.class);
		for (MetadataExtender o : extenders.values()) {
			log.info("Found {} extender", o.getClass().getName());
		}

		FhirVersionEnum fhirVersion = myFhirSystemDao.getContext().getVersion().getVersion();

		String implementationDescription = myServerProperties.getImplementation_description();
		if (fhirVersion == FhirVersionEnum.DSTU2) {
			List<MetadataExtender<Conformance>> extenderList = extenders.values().stream()
					.map(x -> (MetadataExtender<Conformance>) x).collect(Collectors.toList());
			ExtensibleJpaConformanceProviderDstu2 confProvider = new ExtensibleJpaConformanceProviderDstu2(this,
					myFhirSystemDao,
					myDaoConfig, extenderList);
			confProvider.setImplementationDescription(firstNonNull(implementationDescription, "CQF RULER DSTU2 Server"));
			setServerConformanceProvider(confProvider);
		} else {
			if (fhirVersion == FhirVersionEnum.DSTU3) {
				List<MetadataExtender<CapabilityStatement>> extenderList = extenders.values().stream()
						.map(x -> (MetadataExtender<CapabilityStatement>) x).collect(Collectors.toList());
				ExtensibleJpaConformanceProviderDstu3 confProvider = new ExtensibleJpaConformanceProviderDstu3(this,
						myFhirSystemDao, myDaoConfig, mySearchParamRegistry, extenderList);
				confProvider
						.setImplementationDescription(firstNonNull(implementationDescription, "CQF RULER DSTU3 Server"));
				setServerConformanceProvider(confProvider);
			} else if (fhirVersion == FhirVersionEnum.R4) {
				List<MetadataExtender<IBaseConformance>> extenderList = extenders.values().stream()
						.map(x -> (MetadataExtender<IBaseConformance>) x).collect(Collectors.toList());
				ExtensibleJpaCapabilityStatementProvider confProvider = new ExtensibleJpaCapabilityStatementProvider(this,
						myFhirSystemDao, myDaoConfig, mySearchParamRegistry, myValidationSupport, extenderList);
				confProvider.setImplementationDescription(firstNonNull(implementationDescription, "CQF RULER R4 Server"));
				setServerConformanceProvider(confProvider);
			} else if (fhirVersion == FhirVersionEnum.R5) {
				List<MetadataExtender<IBaseConformance>> extenderList = extenders.values().stream()
						.map(x -> (MetadataExtender<IBaseConformance>) x).collect(Collectors.toList());
				ExtensibleJpaCapabilityStatementProvider confProvider = new ExtensibleJpaCapabilityStatementProvider(this,
						myFhirSystemDao, myDaoConfig, mySearchParamRegistry, myValidationSupport, extenderList);
				confProvider.setImplementationDescription(firstNonNull(implementationDescription, "CQF RULER R5 Server"));
				setServerConformanceProvider(confProvider);
			} else {
				throw new IllegalStateException();
			}
		}

	}
}
