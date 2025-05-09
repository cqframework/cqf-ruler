package org.opencds.cqf.ruler.config;

import org.opencds.cqf.external.annotations.OnR4Condition;
import org.opencds.cqf.fhir.cr.hapi.config.r4.ApplyOperationConfig;
import org.opencds.cqf.fhir.cr.hapi.config.r4.CrR4Config;
import org.opencds.cqf.fhir.cr.hapi.config.r4.DataRequirementsOperationConfig;
import org.opencds.cqf.fhir.cr.hapi.config.r4.EvaluateOperationConfig;
import org.opencds.cqf.fhir.cr.hapi.config.r4.ExtractOperationConfig;
import org.opencds.cqf.fhir.cr.hapi.config.r4.PackageOperationConfig;
import org.opencds.cqf.fhir.cr.hapi.config.r4.PopulateOperationConfig;
import org.opencds.cqf.fhir.cr.hapi.config.r4.QuestionnaireOperationConfig;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Conditional({OnR4Condition.class})
@Import({
	RulerCrCommonConfig.class,
	CrR4Config.class,
	ApplyOperationConfig.class,
	DataRequirementsOperationConfig.class,
	EvaluateOperationConfig.class,
	ExtractOperationConfig.class,
	PackageOperationConfig.class,
	PopulateOperationConfig.class,
	QuestionnaireOperationConfig.class
})
public class RulerCrR4Config {}
