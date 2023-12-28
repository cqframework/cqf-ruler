package org.opencds.cqf.ruler.cr.utility;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;

import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_30_50;
import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_40_50;
import org.hl7.fhir.convertors.conv30_50.VersionConvertor_30_50;
import org.hl7.fhir.convertors.conv40_50.VersionConvertor_40_50;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r5.model.Measure;
import org.opencds.cqf.cql.engine.fhir.exception.FhirVersionMisMatchException;
import org.opencds.cqf.cql.engine.fhir.retrieve.BaseFhirQueryGenerator;
import org.opencds.cqf.cql.engine.fhir.retrieve.R4FhirQueryGenerator;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataRequirements {
	private static final Logger logger = LoggerFactory.getLogger(DataRequirements.class);

	private DataRequirements() {
	}

	private static final String EXTENSION_URL_FHIR_QUERY_PATTERN = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-fhirQueryPattern";

	private static Set<String> getExpressions(Measure measureToUse) {
		Set<String> expressionSet = new HashSet<>();
		measureToUse.getSupplementalData().forEach(supData -> {
			expressionSet.add(supData.getCriteria().getExpression());
		});
		measureToUse.getGroup().forEach(groupMember -> {
			groupMember.getPopulation().forEach(population -> {
				expressionSet.add(population.getCriteria().getExpression());
			});
			groupMember.getStratifier().forEach(stratifier -> {
				expressionSet.add(stratifier.getCriteria().getExpression());
			});
		});
		return expressionSet;
	}



	private static SubjectContext getContextForSubject(Type subject) {
		String contextType = "Patient";

		if (subject instanceof CodeableConcept) {
			for (Coding c : ((CodeableConcept) subject).getCoding()) {
				if ("http://hl7.org/fhir/resource-types".equals(c.getSystem())) {
					contextType = c.getCode();
				}
			}
		}
		return new SubjectContext(contextType, String.format("{{context.%sId}}", contextType.toLowerCase()));
	}

	private static org.hl7.fhir.r4.model.Library addDataRequirementFhirQueries(org.hl7.fhir.r4.model.Library library,
																										SearchParameterResolver searchParameterResolver, TerminologyProvider terminologyProvider, ModelResolver modelResolver,
																										CqlCompilerOptions cqlOptions, IBaseConformance capStatement) {
		List<org.hl7.fhir.r4.model.DataRequirement> dataReqs = library.getDataRequirement();

		try {
			BaseFhirQueryGenerator fhirQueryGenerator = new R4FhirQueryGenerator(searchParameterResolver,
				terminologyProvider, modelResolver);

			if (cqlOptions.getCqlEngineOptions().getPageSize() != null) {
				fhirQueryGenerator.setPageSize(cqlOptions.getCqlEngineOptions().getPageSize());
			}
			fhirQueryGenerator.setExpandValueSets(cqlOptions.getCqlEngineOptions().shouldExpandValueSets());

			Integer maxCodesPerQuery = cqlOptions.getCqlEngineOptions().getMaxCodesPerQuery();
			if (maxCodesPerQuery != null && maxCodesPerQuery > 0) {
				fhirQueryGenerator.setMaxCodesPerQuery(cqlOptions.getCqlEngineOptions().getMaxCodesPerQuery());
			}

			Integer queryBatchThreshold = cqlOptions.getCqlEngineOptions().getQueryBatchThreshold();
			if (queryBatchThreshold != null && queryBatchThreshold > 0) {
				fhirQueryGenerator.setQueryBatchThreshold(cqlOptions.getCqlEngineOptions().getQueryBatchThreshold());
			}

			Map<String, Object> contextValues = new HashMap<String, Object>();
			SubjectContext contextValue = getContextForSubject(library.getSubject());
			if (contextValue != null) {
				contextValues.put(contextValue.getContextType(), contextValue.getContextValue());
			}

			for (org.hl7.fhir.r4.model.DataRequirement drq : dataReqs) {
				// TODO: Support DataRequirement-level subject overrides
				List<String> queries = fhirQueryGenerator.generateFhirQueries(drq, null, contextValues, null, capStatement);
				for (String query : queries) {
					org.hl7.fhir.r4.model.Extension ext = new org.hl7.fhir.r4.model.Extension();
					ext.setUrl(EXTENSION_URL_FHIR_QUERY_PATTERN);
					ext.setValue(new org.hl7.fhir.r4.model.StringType(query));
					drq.getExtension().add(ext);
				}
			}
		} catch (FhirVersionMisMatchException e) {
			logger.debug("Error attempting to generate FHIR queries: {}", e.getMessage());
		}

		return library;
	}

	public static org.hl7.fhir.dstu3.model.Library getModuleDefinitionLibraryDstu3(org.hl7.fhir.dstu3.model.Measure measureToUse,
			LibraryManager libraryManager, CompiledLibrary translatedLibrary, CqlTranslatorOptions options, Map<String, Object> parameters) {
		VersionConvertor_30_50 versionConvertor_30_50 = new VersionConvertor_30_50(new BaseAdvisor_30_50());
		org.hl7.fhir.r5.model.Measure r5Measure = (org.hl7.fhir.r5.model.Measure) versionConvertor_30_50
				.convertResource(measureToUse);

		org.hl7.fhir.r5.model.Library effectiveDataRequirements =
			getModuleDefinitionLibraryR5(r5Measure, libraryManager, translatedLibrary, options, parameters);

		org.hl7.fhir.dstu3.model.Library stu3EffectiveDataRequirements =
			(org.hl7.fhir.dstu3.model.Library) versionConvertor_30_50.convertResource(effectiveDataRequirements);

		// TODO: Support dataRequirementFhirQueries in STU3
		return stu3EffectiveDataRequirements;
	}

	public static org.hl7.fhir.dstu3.model.Library getModuleDefinitionLibraryDstu3(LibraryManager libraryManager,
			CompiledLibrary translatedLibrary, CqlTranslatorOptions options, Map<String, Object> parameters) {
		org.hl7.fhir.r5.model.Library libraryR5 = getModuleDefinitionLibraryR5(libraryManager, translatedLibrary,
				options, parameters);

		BaseAdvisor_30_50 baseAdvisor_30_50 = new BaseAdvisor_30_50();
		VersionConvertor_30_50 versionConvertor_30_50 = new VersionConvertor_30_50(baseAdvisor_30_50);
		org.hl7.fhir.dstu3.model.Library libraryDstu3 =
			(org.hl7.fhir.dstu3.model.Library) versionConvertor_30_50.convertResource(libraryR5);

		// TODO: Support dataRequirementFhirQueries in STU3
		return libraryDstu3;
	}

	public static org.hl7.fhir.r4.model.Library getModuleDefinitionLibraryR4(org.hl7.fhir.r4.model.Measure measureToUse,
			LibraryManager libraryManager, CompiledLibrary translatedLibrary, CqlOptions cqlOptions,
			SearchParameterResolver searchParameterResolver, TerminologyProvider terminologyProvider, ModelResolver modelResolver,
			IBaseConformance capStatement, Map<String, Object> parameters) {

		VersionConvertor_40_50 versionConvertor_40_50 = new VersionConvertor_40_50(new BaseAdvisor_40_50());
		org.hl7.fhir.r5.model.Measure r5Measure = (org.hl7.fhir.r5.model.Measure) versionConvertor_40_50.convertResource(measureToUse);

		org.hl7.fhir.r5.model.Library effectiveDataRequirements =
			getModuleDefinitionLibraryR5(r5Measure, libraryManager, translatedLibrary, cqlOptions.getCqlTranslatorOptions(), parameters);

			org.hl7.fhir.r4.model.Library r4EffectiveDataRequirements = (org.hl7.fhir.r4.model.Library) versionConvertor_40_50
				.convertResource(effectiveDataRequirements);
		r4EffectiveDataRequirements = addDataRequirementFhirQueries(r4EffectiveDataRequirements, searchParameterResolver,
				terminologyProvider, modelResolver, cqlOptions, capStatement);
		return r4EffectiveDataRequirements;
	}

	public static org.hl7.fhir.r4.model.Library getModuleDefinitionLibraryR4(LibraryManager libraryManager,
			CompiledLibrary translatedLibrary, CqlOptions cqlOptions, SearchParameterResolver searchParameterResolver,
			TerminologyProvider terminologyProvider, ModelResolver modelResolver, IBaseConformance capStatement,
			Map<String, Object> parameters) {

		org.hl7.fhir.r5.model.Library libraryR5 = getModuleDefinitionLibraryR5(libraryManager, translatedLibrary,
			cqlOptions.getCqlTranslatorOptions(), parameters);

		VersionConvertor_40_50 versionConvertor_40_50 = new VersionConvertor_40_50(new BaseAdvisor_40_50());

		org.hl7.fhir.r4.model.Library libraryR4 = (org.hl7.fhir.r4.model.Library) versionConvertor_40_50.convertResource(libraryR5);

		libraryR4 = addDataRequirementFhirQueries(libraryR4, searchParameterResolver, terminologyProvider, modelResolver,
				cqlOptions, capStatement);

		return libraryR4;
	}

	public static org.hl7.fhir.r5.model.Library getModuleDefinitionLibraryR5(LibraryManager libraryManager,
			 CompiledLibrary translatedLibrary, CqlTranslatorOptions options, Map<String, Object> parameters) {
		DataRequirementsProcessor dqReqTrans = new DataRequirementsProcessor();

		return dqReqTrans.gatherDataRequirements(libraryManager, translatedLibrary, options, null,
			parameters, true, true);
	}

	public static org.hl7.fhir.r5.model.Library getModuleDefinitionLibraryR5(org.hl7.fhir.r5.model.Measure measureToUse,
			LibraryManager libraryManager, CompiledLibrary translatedLibrary, CqlTranslatorOptions options, Map<String, Object> parameters) {
		Set<String> expressionList = getExpressions(measureToUse);
		DataRequirementsProcessor dqReqTrans = new DataRequirementsProcessor();

		return dqReqTrans.gatherDataRequirements(libraryManager, translatedLibrary, options, expressionList,
			parameters, true, true);
	}
}
