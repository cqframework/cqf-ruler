package org.opencds.cqf.common.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.dao.ISearchBuilder;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.model.TranslatedLibrary;
import org.cqframework.cql.elm.requirements.fhir.DataRequirementsProcessor;
import org.hl7.fhir.convertors.VersionConvertor_40_50;
import org.hl7.fhir.r5.model.*;
import org.opencds.cqf.common.config.HapiProperties;
import org.opencds.cqf.cql.engine.fhir.retrieve.R4FhirQueryGenerator;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.engine.fhir.terminology.R4FhirTerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class CommonDataRequirementsProvider {

    private static final Logger logger = LoggerFactory.getLogger(CommonDataRequirementsProvider.class);

    private static String EXTENSION_URL_PARAMETER = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-parameter";
    private static String EXTENSION_URL_DATA_REQUIREMENT = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-dataRequirement";
    private static String EXTENSION_URL_REFERENCE_CODE = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-directReferenceCode";
    private static String EXTENSION_URL_LOGIC_DEFINITION = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-logicDefinition";
    private static String EXTENSION_URL_COMPUTABLE_MEASURE = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/computable-measure-cqfm";
    private static String EXTENSION_URL_FHIR_QUERY_PATTERN = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-fhirQueryPattern";


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
        Library library = this.internalGetModuleDefinitionLibrary(libraryManager, translatedLibrary, options, expressionList);

        return library;
    }

    public Library getModuleDefinitionLibrary(LibraryManager libraryManager, TranslatedLibrary translatedLibrary, CqlTranslatorOptions options) {
        Library library = this.internalGetModuleDefinitionLibrary(libraryManager, translatedLibrary, options, null);

        return library;
    }

    public Library internalGetModuleDefinitionLibrary(LibraryManager libraryManager, TranslatedLibrary translatedLibrary, CqlTranslatorOptions options, Set<String> expressionList) {
        DataRequirementsProcessor dqReqTrans = new DataRequirementsProcessor();

        boolean shouldGetDataRequirementsRecursively = false;
        Library library = dqReqTrans.gatherDataRequirements(libraryManager, translatedLibrary, options, expressionList,
                true, shouldGetDataRequirementsRecursively);


        org.hl7.fhir.r4.model.Library libraryR4 = (org.hl7.fhir.r4.model.Library) VersionConvertor_40_50.convertResource(library);

        List<org.hl7.fhir.r4.model.DataRequirement> dataReqs = libraryR4.getDataRequirement();

        SearchParameterResolver searchParameterResolver = new SearchParameterResolver(FhirContext.forR4());
        TerminologyProvider terminologyProvider = new R4FhirTerminologyProvider(FhirContext.forCached(FhirVersionEnum.R4)
                .newRestfulGenericClient(HapiProperties.getServerAddress()));
        R4FhirQueryGenerator queryGenerator = new R4FhirQueryGenerator(searchParameterResolver, terminologyProvider);
        for (org.hl7.fhir.r4.model.DataRequirement drq : dataReqs) {
            List<String> queries = queryGenerator.generateFhirQueries(drq, null);
            for (String query : queries) {
                org.hl7.fhir.r4.model.Extension ext = new org.hl7.fhir.r4.model.Extension();
                ext.setUrl(EXTENSION_URL_FHIR_QUERY_PATTERN);
                ext.setValue(new org.hl7.fhir.r4.model.StringType(query));
                drq.getExtension().add(ext);
            }
        }

        org.hl7.fhir.r5.model.Library libraryR5 = (org.hl7.fhir.r5.model.Library) VersionConvertor_40_50.convertResource(libraryR4);

        return libraryR5;
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
