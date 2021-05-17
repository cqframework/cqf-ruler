package org.opencds.cqf.ruler.server.cql;

/* 
*  This file created from the HAPI FHIR JPA Server Starter project
*  https://github.com/hapifhir/hapi-fhir-jpaserver-starter 
*/

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class CqlConfigCondition implements Condition {

  @Override
  public boolean matches(ConditionContext theConditionContext, AnnotatedTypeMetadata theAnnotatedTypeMetadata) {
    String property = theConditionContext.getEnvironment().getProperty("hapi.fhir.cql_enabled");
    boolean enabled = Boolean.parseBoolean(property);
    return enabled;
  }
}
