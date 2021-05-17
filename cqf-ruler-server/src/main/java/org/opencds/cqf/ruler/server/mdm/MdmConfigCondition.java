package org.opencds.cqf.ruler.server.mdm;

/* 
*  This file created from the HAPI FHIR JPA Server Starter project
*  https://github.com/hapifhir/hapi-fhir-jpaserver-starter 
*/

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class MdmConfigCondition implements Condition {
  @Override
  public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata metadata) {
    String property = conditionContext.getEnvironment().getProperty("hapi.fhir.mdm_enabled");
    return Boolean.parseBoolean(property);
  }
}
