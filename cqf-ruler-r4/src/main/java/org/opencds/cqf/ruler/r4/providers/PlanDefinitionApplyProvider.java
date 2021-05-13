package org.opencds.cqf.ruler.r4.providers;

import java.io.IOException;

import javax.inject.Inject;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.opencds.cqf.ruler.common.exceptions.NotImplementedException;
import org.opencds.cqf.ruler.common.config.HapiProperties;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.evaluator.activitydefinition.r4.ActivityDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.opencds.cqf.ruler.r4.builders.AttachmentBuilder;
import org.opencds.cqf.ruler.r4.builders.CarePlanActivityBuilder;
import org.opencds.cqf.ruler.r4.builders.CarePlanBuilder;
import org.opencds.cqf.ruler.r4.builders.ExtensionBuilder;
import org.opencds.cqf.ruler.r4.builders.JavaDateBuilder;
import org.opencds.cqf.ruler.r4.builders.ReferenceBuilder;
import org.opencds.cqf.ruler.r4.builders.RelatedArtifactBuilder;
import org.opencds.cqf.ruler.r4.builders.RequestGroupActionBuilder;
import org.opencds.cqf.ruler.r4.builders.RequestGroupBuilder;
import org.opencds.cqf.ruler.common.dal.RulerDal;
import org.opencds.cqf.ruler.r4.helpers.CanonicalHelper;
import org.opencds.cqf.ruler.r4.helpers.ContainedHelper;
import org.opencds.cqf.cql.evaluator.plandefinition.r4.OperationParametersParser;
import org.opencds.cqf.cql.evaluator.plandefinition.r4.PlanDefinitionProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;

@Component
public class PlanDefinitionApplyProvider {
  protected PlanDefinitionProcessor planDefinitionProcessor;
  protected IFhirResourceDao<PlanDefinition> planDefinitionDao;
  protected OperationParametersParser operationParametersParser;

  protected static final Logger logger = LoggerFactory.getLogger(PlanDefinitionApplyProvider.class);

  @Inject
  public PlanDefinitionApplyProvider(FhirDal fhirDal, FhirContext fhirContext, ActivityDefinitionProcessor activityDefinitionProcessor,
    LibraryProcessor libraryProcessor, IFhirResourceDao<PlanDefinition> planDefinitionDao, AdapterFactory adapterFactory, FhirTypeConverter fhirTypeConverter) {
    this.planDefinitionDao = planDefinitionDao;
    operationParametersParser = new OperationParametersParser(adapterFactory, fhirTypeConverter);
    this.planDefinitionProcessor = new PlanDefinitionProcessor(fhirContext, fhirDal, libraryProcessor, activityDefinitionProcessor, operationParametersParser);
  }

  public IFhirResourceDao<PlanDefinition> getDao() {
    return this.planDefinitionDao;
  }

  @Operation(name = "$apply", idempotent = true, type = PlanDefinition.class)
  public CarePlan applyPlanDefinition(
      @IdParam IdType theId,
      @OperationParam(name = "subject") String patientId,
      @OperationParam(name = "encounter") String encounterId,
      @OperationParam(name = "practitioner") String practitionerId,
      @OperationParam(name = "organization") String organizationId,
      @OperationParam(name = "userType") String userType,
      @OperationParam(name = "userLanguage") String userLanguage,
      @OperationParam(name = "userTaskContext") String userTaskContext,
      @OperationParam(name = "setting") String setting,
      @OperationParam(name = "settingContext") String settingContext,
      @OperationParam(name = "mergeNestedCarePlans") boolean mergeNestedCarePlans,
      @OperationParam(name = "parameters") IBaseParameters parameters,
      @OperationParam(name = "useServerData") boolean useServerData,
      @OperationParam(name = "data") IBaseBundle bundle,
      @OperationParam(name = "prefetchData") IBaseParameters prefetchData,
      @OperationParam(name = "dataEndpoint") IBaseResource dataEndpoint,
      @OperationParam(name = "contentEndpoint") IBaseResource contentEndpoint,
      @OperationParam(name = "terminologyEndpoint") IBaseResource terminologyEndpoint)
      throws IOException, FHIRException {
        IBaseParameters resultParameters = planDefinitionProcessor.apply(theId, patientId, encounterId, practitionerId, organizationId, userType, userLanguage, userTaskContext, setting, settingContext, mergeNestedCarePlans, parameters, useServerData, bundle, prefetchData, dataEndpoint, contentEndpoint, terminologyEndpoint);
        if (resultParameters != null) {
          IBaseResource returnResource = operationParametersParser.getResourceChild(resultParameters, "return");
          if (returnResource == null || !(returnResource instanceof CarePlan)) {
            logger.debug("Return resource was not a CarePlan resource.");
            throw new RuntimeException(String.format("Return resource must be instance of CarePlan, PlanDefinitionProcessor apply did returned: %s", returnResource));
          }
          logger.info("Returning CarePlan result from PlanDefinitionProcessor apply.");
          return (CarePlan) returnResource;
        }
        logger.info("Null result from PlanDefinitionProcessor apply");
        return null;
      }
}
