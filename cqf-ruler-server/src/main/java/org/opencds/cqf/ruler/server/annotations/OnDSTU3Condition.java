package org.opencds.cqf.ruler.server.annotations;

/* 
*  This file created from the HAPI FHIR JPA Server Starter project
*  https://github.com/hapifhir/hapi-fhir-jpaserver-starter 
*/

import ca.uhn.fhir.context.FhirVersionEnum;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class OnDSTU3Condition implements Condition {
  @Override
  public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata metadata) {
    FhirVersionEnum version = FhirVersionEnum.forVersionString(conditionContext.
      getEnvironment()
      .getProperty("hapi.fhir.fhir_version")
      .toUpperCase());

    return version == FhirVersionEnum.DSTU3;

  }
}
