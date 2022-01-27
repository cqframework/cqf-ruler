package org.opencds.cqf.ruler.cr.common;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.model.TranslatedLibrary;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.Measure;
import org.hl7.fhir.r5.model.Meta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.cqframework.cql.elm.requirements.fhir.DataRequirementsProcessor;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommonDataRequirementsUtility {
	private static final Logger logger = LoggerFactory.getLogger(CommonDataRequirementsUtility.class);

	private static String EXTENSION_URL_PARAMETER = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-parameter";
	private static String EXTENSION_URL_DATA_REQUIREMENT = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-dataRequirement";
	private static String EXTENSION_URL_REFERENCE_CODE = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-directReferenceCode";
	private static String EXTENSION_URL_LOGIC_DEFINITION = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-logicDefinition";
	private static String EXTENSION_URL_COMPUTABLE_MEASURE = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/computable-measure-cqfm";


	public Measure createMeasure(Measure measureToUse, LibraryManager libraryManager, TranslatedLibrary translatedLibrary, CqlTranslatorOptions options) {

		Library moduleDefinitionLibrary = getModuleDefinitionLibrary(measureToUse, libraryManager, translatedLibrary, options);

		measureToUse.setDate(new Date());
		setMeta(measureToUse, moduleDefinitionLibrary);
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

	public Library getModuleDefinitionLibrary(org.hl7.fhir.r5.model.Measure measureToUse, LibraryManager libraryManager, TranslatedLibrary translatedLibrary, CqlTranslatorOptions options) {

		Set<String> expressionList = getExpressions(measureToUse);
		DataRequirementsProcessor dqReqTrans = new DataRequirementsProcessor();

		Library library = dqReqTrans.gatherDataRequirements(libraryManager, translatedLibrary, options, expressionList, true);
		return library;
	}

	public Library getModuleDefinitionLibrary(LibraryManager libraryManager, TranslatedLibrary translatedLibrary, CqlTranslatorOptions options) {
		DataRequirementsProcessor dqReqTrans = new DataRequirementsProcessor();
		Library library = dqReqTrans.gatherDataRequirements(libraryManager, translatedLibrary, options, null, true);

		return library;
	}

	private Set<String> getExpressions(Measure measureToUse) {
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

	private void clearMeasureExtensions(Measure measure, String extensionUrl) {
		List<Extension> extensionsToRemove = measure.getExtensionsByUrl(extensionUrl);
		measure.getExtension().removeAll(extensionsToRemove);
	}

	private void clearRelatedArtifacts(Measure measure) {
		measure.getRelatedArtifact().removeIf(r -> r.getType() == org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType.DEPENDSON);
	}

	private void setLogicDefinition(Measure measureToUse, org.hl7.fhir.r5.model.Library moduleDefinitionLibrary) {
		moduleDefinitionLibrary.getExtension().forEach(extension -> {
			if (extension.getUrl().equalsIgnoreCase(EXTENSION_URL_LOGIC_DEFINITION)) {
				measureToUse.addExtension(extension);
			}
		});
	}

	private void setDirectReferenceCode(Measure measureToUse, org.hl7.fhir.r5.model.Library moduleDefinitionLibrary) {
		moduleDefinitionLibrary.getExtension().forEach(extension -> {
			if (extension.getUrl().equalsIgnoreCase(EXTENSION_URL_REFERENCE_CODE)) {
				measureToUse.addExtension(extension);
			}
		});
	}

	private void setDataRequirements(Measure measureToUse, org.hl7.fhir.r5.model.Library moduleDefinitionLibrary) {
		moduleDefinitionLibrary.getDataRequirement().forEach(dataRequirement -> {
			Extension dataReqExtension = new Extension();
			dataReqExtension.setUrl(EXTENSION_URL_DATA_REQUIREMENT);
			dataReqExtension.setValue(dataRequirement);
			measureToUse.addExtension(dataReqExtension);
		});
	}

	private void setParameters(Measure measureToUse, org.hl7.fhir.r5.model.Library moduleDefinitionLibrary) {
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

	private void setMeta(Measure measureToUse, org.hl7.fhir.r5.model.Library moduleDefinitionLibrary) {
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

}
