package org.opencds.cqf.ruler.cr.utility;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.model.TranslatedLibrary;
import org.cqframework.cql.elm.requirements.fhir.DataRequirementsProcessor;
import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_30_50;
import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_40_50;
import org.hl7.fhir.convertors.conv30_50.VersionConvertor_30_50;
import org.hl7.fhir.convertors.conv40_50.VersionConvertor_40_50;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.Measure;
import org.hl7.fhir.r5.model.Meta;

public class DataRequirements {
	private DataRequirements() {
	}

	private static final String EXTENSION_URL_PARAMETER = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-parameter";
	private static final String EXTENSION_URL_DATA_REQUIREMENT = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-dataRequirement";
	private static final String EXTENSION_URL_REFERENCE_CODE = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-directReferenceCode";
	private static final String EXTENSION_URL_LOGIC_DEFINITION = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-logicDefinition";
	private static final String EXTENSION_URL_COMPUTABLE_MEASURE = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/computable-measure-cqfm";
	private static final String EXTENSION_URL_FHIR_QUERY_PATTERN = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-fhirQueryPattern";

	public static Measure createMeasure(Measure measureToUse, LibraryManager libraryManager,
			TranslatedLibrary translatedLibrary, CqlTranslatorOptions options) {

		org.hl7.fhir.r5.model.Library moduleDefinitionLibrary = getModuleDefinitionLibrary(measureToUse, libraryManager, translatedLibrary,
				options);

		measureToUse.setDate(new Date());
		setMeta(measureToUse);
		clearMeasureExtensions(measureToUse, EXTENSION_URL_PARAMETER);
		clearMeasureExtensions(measureToUse, EXTENSION_URL_DATA_REQUIREMENT);
		clearMeasureExtensions(measureToUse, EXTENSION_URL_REFERENCE_CODE);
		clearMeasureExtensions(measureToUse, EXTENSION_URL_LOGIC_DEFINITION);
		clearRelatedArtifacts(measureToUse);
		setParameters(measureToUse, moduleDefinitionLibrary);
		setDataRequirements(measureToUse, moduleDefinitionLibrary);
		setDirectReferenceCode(measureToUse, moduleDefinitionLibrary);
		setLogicDefinition(measureToUse, moduleDefinitionLibrary);
		measureToUse.setRelatedArtifact(moduleDefinitionLibrary.getRelatedArtifact());

		return measureToUse;
	}

	public static org.hl7.fhir.r5.model.Library getModuleDefinitionLibrary(org.hl7.fhir.r5.model.Measure measureToUse,
			LibraryManager libraryManager,
			TranslatedLibrary translatedLibrary, CqlTranslatorOptions options) {

		Set<String> expressionList = getExpressions(measureToUse);
		DataRequirementsProcessor dqReqTrans = new DataRequirementsProcessor();

		return dqReqTrans.gatherDataRequirements(libraryManager, translatedLibrary, options, expressionList, true);
	}

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

	private static void clearMeasureExtensions(Measure measure, String extensionUrl) {
		List<Extension> extensionsToRemove = measure.getExtensionsByUrl(extensionUrl);
		measure.getExtension().removeAll(extensionsToRemove);
	}

	private static void clearRelatedArtifacts(Measure measure) {
		measure.getRelatedArtifact()
				.removeIf(r -> r.getType() == org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType.DEPENDSON);
	}

	private static void setLogicDefinition(Measure measureToUse, org.hl7.fhir.r5.model.Library moduleDefinitionLibrary) {
		moduleDefinitionLibrary.getExtension().forEach(extension -> {
			if (extension.getUrl().equalsIgnoreCase(EXTENSION_URL_LOGIC_DEFINITION)) {
				measureToUse.addExtension(extension);
			}
		});
	}

	private static void setDirectReferenceCode(Measure measureToUse,
			org.hl7.fhir.r5.model.Library moduleDefinitionLibrary) {
		moduleDefinitionLibrary.getExtension().forEach(extension -> {
			if (extension.getUrl().equalsIgnoreCase(EXTENSION_URL_REFERENCE_CODE)) {
				measureToUse.addExtension(extension);
			}
		});
	}

	private static void setDataRequirements(Measure measureToUse,
			org.hl7.fhir.r5.model.Library moduleDefinitionLibrary) {
		moduleDefinitionLibrary.getDataRequirement().forEach(dataRequirement -> {
			Extension dataReqExtension = new Extension();
			dataReqExtension.setUrl(EXTENSION_URL_DATA_REQUIREMENT);
			dataReqExtension.setValue(dataRequirement);
			measureToUse.addExtension(dataReqExtension);
		});
	}

	private static void setParameters(Measure measureToUse, org.hl7.fhir.r5.model.Library moduleDefinitionLibrary) {
		Set<String> parameterName = new HashSet<>();
		moduleDefinitionLibrary.getParameter().forEach(parameter -> {
			if (!parameterName.contains(parameter.getName())) {
				Extension parameterExtension = new Extension();
				parameterExtension.setUrl(EXTENSION_URL_PARAMETER);
				parameterExtension.setValue(parameter);
				measureToUse.addExtension(parameterExtension);
				parameterName.add(parameter.getName());
			}
		});
	}

	private static void setMeta(Measure measureToUse) {
		if (measureToUse.getMeta() == null) {
			measureToUse.setMeta(new Meta());
		}
		boolean hasProfileMarker = false;
		for (CanonicalType canonical : measureToUse.getMeta().getProfile()) {
			if (EXTENSION_URL_COMPUTABLE_MEASURE.equals(canonical.getValue())) {
				hasProfileMarker = true;
			}
		}
		if (!hasProfileMarker) {
			measureToUse.getMeta().addProfile(EXTENSION_URL_COMPUTABLE_MEASURE);
		}
	}

	public static org.hl7.fhir.dstu3.model.Library getModuleDefinitionLibraryDstu3(LibraryManager libraryManager,
			TranslatedLibrary translatedLibrary, CqlTranslatorOptions options) {
		org.hl7.fhir.r5.model.Library libraryR5 = getModuleDefinitionLibraryR5(libraryManager, translatedLibrary,
				options);

		BaseAdvisor_30_50  baseAdvisor_30_50= new BaseAdvisor_30_50();
		VersionConvertor_30_50 versionConvertor_30_50 = new VersionConvertor_30_50(baseAdvisor_30_50);
		org.hl7.fhir.dstu3.model.Library libraryDstu3 = null;
		libraryDstu3 = (org.hl7.fhir.dstu3.model.Library) versionConvertor_30_50.convertResource(libraryR5);
		// libraryR4 = this.addDataRequirementFhirQueries(libraryDstu3); // uncomment
		// this when engine fhir query generation available
		return libraryDstu3;
	}

	public static org.hl7.fhir.r4.model.Library getModuleDefinitionLibraryR4( LibraryManager libraryManager, TranslatedLibrary translatedLibrary,
																									  CqlTranslatorOptions options) {
		org.hl7.fhir.r5.model.Library libraryR5 = getModuleDefinitionLibraryR5(libraryManager, translatedLibrary,
			options);
		VersionConvertor_40_50 versionConvertor_30_50 = new VersionConvertor_40_50(new BaseAdvisor_40_50());
		org.hl7.fhir.r4.model.Library libraryR4 = (org.hl7.fhir.r4.model.Library) versionConvertor_30_50
				.convertResource(libraryR5);
		//libraryR4 = addDataRequirementFhirQueries(libraryR4);
		return libraryR4;
	}

//	private static org.hl7.fhir.r4.model.Library addDataRequirementFhirQueries(org.hl7.fhir.r4.model.Library library) {
//		List<org.hl7.fhir.r4.model.DataRequirement> dataReqs = library.getDataRequirement();
//
//		for (org.hl7.fhir.r4.model.DataRequirement drq : dataReqs) {
//			List<String> queries = fhirQueryGenerator.generateFhirQueries(drq, null,null, null, iBaseConformance);
//			for (String query : queries) {
//				org.hl7.fhir.r4.model.Extension ext = new org.hl7.fhir.r4.model.Extension();
//				ext.setUrl(EXTENSION_URL_FHIR_QUERY_PATTERN);
//				ext.setValue(new org.hl7.fhir.r4.model.StringType(query));
//				drq.getExtension().add(ext);
//			}
//		}
//		return library;
//	}
	public static org.hl7.fhir.r5.model.Library getModuleDefinitionLibraryR5(LibraryManager libraryManager,
			TranslatedLibrary translatedLibrary,
			CqlTranslatorOptions options) {
		DataRequirementsProcessor dqReqTrans = new DataRequirementsProcessor();
		return dqReqTrans.gatherDataRequirements(libraryManager, translatedLibrary, options, null, true);
	}

}
