package org.opencds.cqf.ruler.server.cql;

/* 
*  This file created from the HAPI FHIR JPA Server Starter project
*  https://github.com/hapifhir/hapi-fhir-jpaserver-starter 
*/

import ca.uhn.fhir.cql.config.CqlR4Config;
import org.opencds.cqf.ruler.server.annotations.OnR4Condition;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;

@Conditional({OnR4Condition.class, CqlConfigCondition.class})
@Import({CqlR4Config.class})
public class StarterCqlR4Config {
}
