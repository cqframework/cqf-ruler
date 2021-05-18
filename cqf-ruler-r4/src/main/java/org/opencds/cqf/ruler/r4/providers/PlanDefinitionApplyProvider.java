package org.opencds.cqf.ruler.r4.providers;

import javax.inject.Inject;

import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.evaluator.activitydefinition.r4.ActivityDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.opencds.cqf.cql.evaluator.plandefinition.r4.OperationParametersParser;
import org.opencds.cqf.cql.evaluator.plandefinition.r4.PlanDefinitionProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;

@Component
public class PlanDefinitionApplyProvider {
  protected PlanDefinitionProcessor planDefinitionProcessor;
  protected IFhirResourceDao<PlanDefinition> planDefinitionDao;
  protected OperationParametersParser operationParametersParser;
  private FhirContext fhirContext;

  protected static final Logger logger = LoggerFactory.getLogger(PlanDefinitionApplyProvider.class);

  @Inject
  public PlanDefinitionApplyProvider(FhirDal fhirDal, FhirContext fhirContext, ActivityDefinitionProcessor activityDefinitionProcessor,
    LibraryProcessor libraryProcessor, IFhirResourceDao<PlanDefinition> planDefinitionDao, org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory adapterFactory, FhirTypeConverter fhirTypeConverter) {
    this.planDefinitionDao = planDefinitionDao;
    this.fhirContext = fhirContext;
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
      @OperationParam(name = "mergeNestedCarePlans") BooleanType mergeNestedCarePlans,
      @OperationParam(name = "parameters") Parameters parameters,
      @OperationParam(name = "useServerData") BooleanType useServerData,
      @OperationParam(name = "data") Bundle bundle,
      @OperationParam(name = "prefetchData") Parameters prefetchData,
      @OperationParam(name = "dataEndpoint") Endpoint dataEndpoint,
      @OperationParam(name = "contentEndpoint") Endpoint contentEndpoint,
      @OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint) {
        Boolean mergeNestedCarePlansTemp = null;
        Boolean useServerDataTemp = null;
        if (mergeNestedCarePlans!= null && mergeNestedCarePlans.hasValue()) {
          mergeNestedCarePlansTemp = mergeNestedCarePlans.booleanValue();
        }
        if (useServerData!= null && useServerData.hasValue()) {
          useServerDataTemp = useServerData.booleanValue();
        }
        
        IBaseParameters resultParameters = planDefinitionProcessor.apply(theId, patientId, encounterId, practitionerId, organizationId, userType, userLanguage, userTaskContext, setting, settingContext, mergeNestedCarePlansTemp, parameters, useServerDataTemp, bundle, prefetchData, dataEndpoint, contentEndpoint, terminologyEndpoint);
        if (resultParameters != null) {
          IBaseResource returnResource = ((Parameters)resultParameters).getParameter().stream().filter(x -> x.getName().equals("return")).findFirst().get().getResource();
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
