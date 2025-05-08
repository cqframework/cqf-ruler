package org.opencds.cqf.ruler.config;

import org.opencds.cqf.external.annotations.OnDSTU3Condition;
import org.opencds.cqf.external.cr.CrConfigCondition;
import org.opencds.cqf.fhir.cr.hapi.config.dstu3.ApplyOperationConfig;
import org.opencds.cqf.fhir.cr.hapi.config.dstu3.CrDstu3Config;
import org.opencds.cqf.fhir.cr.hapi.config.dstu3.DataRequirementsOperationConfig;
import org.opencds.cqf.fhir.cr.hapi.config.dstu3.EvaluateOperationConfig;
import org.opencds.cqf.fhir.cr.hapi.config.dstu3.PackageOperationConfig;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Conditional({OnDSTU3Condition.class, CrConfigCondition.class})
@Import({
	RulerCrCommonConfig.class,
	CrDstu3Config.class,
	ApplyOperationConfig.class,
	DataRequirementsOperationConfig.class,
	EvaluateOperationConfig.class,
	PackageOperationConfig.class
})
public class RulerCrDstu3Config {}
