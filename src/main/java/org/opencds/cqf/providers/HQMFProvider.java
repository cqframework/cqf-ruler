package org.opencds.cqf.providers;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import com.helger.collection.pair.Pair;
import com.jamesmurty.utils.XMLBuilder2;

import org.apache.derby.iapi.types.XML;
import org.hibernate.query.criteria.internal.compile.CriteriaInterpretation;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Contributor;
import org.hl7.fhir.dstu3.model.DataElement;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.MarkdownType;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Contributor.ContributorType;
import org.hl7.fhir.dstu3.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.dstu3.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.dstu3.model.Measure.MeasureSupplementalDataComponent;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.springframework.http.MediaTypeEditor;
import org.stringtemplate.v4.compiler.STParser.compoundElement_return;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class HQMFProvider {

    private static Map<String, String> measureTypeValueSetMap = new HashMap<String, String>() {
        {
            put("PROCESS", "Process");
            put("OUTCOME", "Outcome");
            put("STRUCTURE", "Structure");
            put("PATIENT-REPORTED-OUTCOME", "Patient Reported Outcome");
            put("COMPOSITE", "Composite");
        }
    };

    private static Map<String, String> measureScoringValueSetMap = new HashMap<String, String>() {
        {
            put("PROPORTION", "Proportion");
            put("RATIO", "Ratio");
            put("CONTINUOUS-VARIABLE", "Continuous Variable");
            put("COHORT", "Cohort");
        }
    };

    private static class CodeMapping {
        public CodeMapping(String code, String displayName, String criteriaName, String criteriaExtension) {
            this.code = code;
            this.displayName = displayName;
            this.criteriaName = criteriaName;
            this.criteriaExtension = criteriaExtension;
        }

        public String code;
        public String displayName;
        public String criteriaName;
        public String criteriaExtension;

    }

    private static Map<String, CodeMapping> measurePopulationValueSetMap = new HashMap<String, CodeMapping>() {
        {
            put("initial-population-identifier", new CodeMapping("IPOP", "Initial Population", "initialPopulationCriteria", "initialPopulation"));
            put("numerator-identifier", new CodeMapping("NUMER", "Numerator", "numeratorCriteria", "numerator"));
            put("numerator-exclusion-identifier", new CodeMapping("NUMEX", "Numerator Exclusion", "numeratorExclusionCriteria", "numeratorExclusions"));
            put("denominator-identifier", new CodeMapping("DENOM", "Denominator", "denominatorCriteria", "denominator"));
            put("denominator-exclusions-identifier", new CodeMapping("DENEX", "Denominator Exclusion", "denominatorExclusionCritieria", "denominatorExclusions"));
            put("denominator-exception-identifier", new CodeMapping("DENEXCEP", "Denominator Exception", "denominatorExceptionCriteria", "denominatorExceptions"));
            // TODO: Figure out what the codes for these are (MPOP, MPOPEX, MPOPEXCEP are
            // guesses)
            put("measure-population-identifier", new CodeMapping("MPOP", "Measure Population", "measurePopulationCriteria", "measurePopulation"));
            put("measure-population-exclusion-identifier", new CodeMapping("MPOPEX", "Measure Population Exclusion", "measurePopulationExclusionCriteria", "measurePopulationExclusions"));
            put("measure-population-observation-identifier", new CodeMapping("MPOPEXCEP", "Measure Population Observation", "measurePopulationObservationCriteria", "measurePopulationObservations"));
        }
    };

    public String generateHQMF(CqfMeasure m) {
        XMLBuilder2 xml = createQualityMeasureDocumentElement(m);
        this.addResponsibleParties(xml, m);
        this.addDefinitions(xml, m);

        String primaryLibraryGuid = UUID.randomUUID().toString();
        String primaryLibraryName = this.addRelatedDocuments(xml, m, primaryLibraryGuid);
        this.addControlVariables(xml, m);
        this.addSubjectOfs(xml, m);
        this.addComponentOfs(xml, m);
        this.addComponents(xml, m, primaryLibraryGuid, primaryLibraryName);
        return writeDocument(xml.getDocument());
    }

    private XMLBuilder2 createQualityMeasureDocumentElement(CqfMeasure m) {
        // TODO: Get unique Id from NCQA identifier...
        String uniqueId = UUID.randomUUID().toString();

        XMLBuilder2 builder = XMLBuilder2.create("QualityMeasureDocument").ns("urn:hl7-org:v3")
                .ns("cql-ext", "urn:hhs-cql:hqmf-n1-extensions:v1")
                .ns("xsi", "http://www.w3.org/2001/XMLSchema-instance")
                .elem("typeId").a("extension", "POQM_HD000001UV02").a("root", "2.16.840.1.113883.1.3").up()
                .elem("templateId").elem("item").a("extension", "2017-08-01").a("root", "2.16.840.1.113883.10.20.28.1.2").up().up()
                .elem("id").a("root","40280382-6258-7581-0162-92660f2414b9").up().up().elem("code")
                .a("code", "57024-2").a("codeSystem", "2.16.840.1.113883.6.1").elem("displayName")
                .a("value", "Health Quality Measure Document").up().up().elem("title").text(m.getDescription()).up()
                .elem("statusCode").a("code", "COMPLETED").up().elem("setId")
                .a("root", uniqueId).up().elem("versionNumber").text(m.getVersion()).up()
                .root();

        return builder;
    }

    private void addDefinitions(XMLBuilder2 xml, CqfMeasure m) {
        HashSet<String> valueSets = new HashSet<String>();
        for (DataRequirement d : m.getDataRequirement()) {

            // ValueSets
            if (d.hasCodeFilter()) {
                String valueSet = d.getCodeFilterFirstRep().getValueSetStringType().asStringValue();
                if (!valueSets.contains(valueSet)) {
                    valueSets.add(valueSet);
                }
            }

            // TODO: Direct reference codes - I think we have to parse these from the CQL
        }

        for (String v : valueSets) {
            this.addValueSet(xml, v);
        }
    }

    private void addValueSet(XMLBuilder2 xml, String oid) {
        xml.root().elem("definition").elem("valueSet").a("classCode", "OBS").a("modeCode", "DEF").elem("id")
                .a("root", oid).up();
        // TODO: We don't have value set names
        // .elem("title").a("value", "getName");
    }

    private void addDirectReferenceCode(XMLBuilder2 xml, DataRequirement d) {
        String code = d.getCodeFilterFirstRep().getPath();
        String codeSystem = d.getCodeFilterFirstRep().getValueSetStringType().asStringValue();
        xml.root().elem("definition").elem("cql-ext:code").a("code", code).a("codeSystem", codeSystem)
                .a("codeSystemName", "getName").a("codeSystemVersion", "getCodeSystemVersion").up().elem("displayName")
                .a("value", "getDisplayName");
    }

    // Returns the 
    private String addRelatedDocuments(XMLBuilder2 xml, CqfMeasure m, String primaryLibraryGuid) {
        // This is all the libraries a measure depends on EXCEPT the primary library
        for (RelatedArtifact r : m.getRelatedArtifact()) {
            if (r.getType() == RelatedArtifactType.DEPENDSON) {
                if (r.hasResource() && r.getResource().hasReference()
                        && r.getResource().getReference().startsWith("Library")) {
                    this.addRelatedDocument(xml, r.getResource().getReference().replace("Library/", ""), UUID.randomUUID().toString());
                }
            }
        }

        if (m.hasLibrary()) {
            String libraryName = m.getLibraryFirstRep().getReference().replace("Library/", "");
            this.addRelatedDocument(xml, libraryName, primaryLibraryGuid);
            return libraryName;
        }

        return null;
    }

    // Returns the random guid assigned to a document
    private void addRelatedDocument(XMLBuilder2 xml, String name, String guid) {
        xml.root().elem("relatedDocument").a("typeCode", "COMP").elem("expressionDocument").elem("id").a("root", guid)
                .up().elem("text").a("mediaType", "text/cql").elem("reference").a("value", name + ".cql").up()
                .elem("translation").a("mediaType", "application/elm+xml").elem("reference").a("value", name + ".xml")
                .up().up().elem("translation").a("mediaType", "application/elm+json").elem("reference")
                .a("value", name + ".json");
    }

    private void addControlVariables(XMLBuilder2 xml, CqfMeasure m) {
        // These are parameters?

        // Measure Period
        // TODO: Same as effective period?
        this.addMeasurePeriod(xml, m.getEffectivePeriod());
    }

    private void addMeasurePeriod(XMLBuilder2 xml, Period p) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        xml.root().elem("controlVariable").elem("measurePeriod").elem("id").a("extension", "measurePeriod")
                .a("root", "cfe828b5-ce93-4354-b137-77b0aeb41dd6").up().elem("code").a("code", "MSRTP")
                .a("codeSystem", "2.16.840.1.113883.5.4").elem("originalText").a("value", "Measurement Period").up()
                .up().elem("value").a("xsi:type", "PIVL_TS").elem("phase").a("highClosed", "true")
                .a("lowClosed", "true").elem("low").a("value", sdf.format(p.getStart())).up().elem("width")
                .a("unit", "a").a("value", "1").a("xsi:type", "PQ").up().up().elem("period").a("unit", "a")
                .a("value", "1");
    }

    private void addSubjectOfs(XMLBuilder2 xml, CqfMeasure m) {
        String codeSystem = "2.16.840.1.113883.5.4";
        XMLBuilder2 temp = null;

        for (Identifier i : m.getIdentifier())
        {
   
            if (i.hasType())
            {
                switch (i.getType().getCodingFirstRep().getCode()) {
                    // eCQMIdentifier (MAT / CMS)
                    case "CMS":
                    this.addMeasureAttributeWithNullAndText(xml, "OTH","eCQM Identifier", "text/plain", i.getValue());
                        break;

                    //NQF Number
                    case "NQF":
                        this.addMeasureAttributeWithNullAndText(xml, "OTH","NQF Number", "text/plain", i.getValue());
                        break;

                    default:
                        break;
                }
            }
        }

        // Copyright
        this.addMeasureAttributeWithCodeAndTextValue(xml, "COPY", codeSystem, "Copyright", "text/plain",
                m.hasCopyright() ? m.getCopyright() : "None");

        // Disclaimer
        this.addMeasureAttributeWithCodeAndTextValue(xml, "DISC", codeSystem, "Disclaimer", "text/plain",
                m.hasDisclaimer() ? m.getDisclaimer() : "None");

        // Measure Scoring
        if (m.hasScoring()) {
            Coding scoring = m.getScoring().getCodingFirstRep();
            String measureScoringCode = scoring.getCode().toUpperCase();
            this.addMeasureAttributeWithCodeAndCodeValue(xml, "MSRSCORE", codeSystem, "Measure Scoring",
                    measureScoringCode, "2.16.840.1.113883.1.11.20367",
                    measureScoringValueSetMap.get(measureScoringCode));
        }

        // Measure Type
        if (m.hasType()) {
            Coding measureType = m.getTypeFirstRep().getCoding().get(0);
            String measureTypeCode = measureType.getCode().toUpperCase();
            this.addMeasureAttributeWithCodeAndCodeValue(xml, "MSRTYPE", codeSystem, "Measure Type", measureTypeCode,
                    "2.16.840.1.113883.1.11.20368", measureTypeValueSetMap.get(measureTypeCode));
        }

        // Risk Adjustment
        this.addMeasureAttributeWithCodeAndTextValue(xml, "MSRADJ", codeSystem, "Risk Adjustment", "text/plain",
                m.hasRiskAdjustment() ? m.getRiskAdjustment() : "None");

        // Rate Aggregation
        this.addMeasureAttributeWithCodeAndTextValue(xml, "MSRAGG", codeSystem, "Rate Aggregation", "text/plain",
                m.hasRateAggregation() ? m.getRateAggregation() : "None");

        // Rationale
        this.addMeasureAttributeWithCodeAndTextValue(xml, "RAT", codeSystem, "Rationale", "text/plain",
                m.hasRationale() ? m.getRationale() : "None");

        // Clinical Recomendation Statement
        this.addMeasureAttributeWithCodeAndTextValue(xml, "CRS", codeSystem, "Clinical Recommendation Statement",
                "text/plain", m.hasClinicalRecommendationStatement() ? m.getClinicalRecommendationStatement() : "None");

        // Improvement Notation
        this.addMeasureAttributeWithCodeAndTextValue(xml, "IDUR", codeSystem, "Improvement Notation", "text/plain",
                m.hasImprovementNotation() ? m.getImprovementNotation() : "None");

        // Reference (citations)
        // TODO: Should we add a "None" entry if there are no references?
        if (m.hasRelatedArtifact()) {
            for (RelatedArtifact r : m.getRelatedArtifact()) {
                if (r.getType() == RelatedArtifactType.CITATION) {
                    this.addMeasureAttributeWithCodeAndTextValue(xml, "REF", codeSystem, "Reference", "text/plain",
                            r.getCitation());
                }
            }
        }

        // Definition
        if (m.hasDefinition()) {
            for (MarkdownType mt : m.getDefinition()) {
                this.addMeasureAttributeWithCodeAndTextValue(xml, "DEF", codeSystem, "Definition", "text/markdown",
                        mt.asStringValue());
            }
        } else {
            this.addMeasureAttributeWithCodeAndTextValue(xml, "DEF", codeSystem, "Definition", "text/plain", "None");
        }

        // Guidance
        this.addMeasureAttributeWithCodeAndTextValue(xml, "GUIDE", codeSystem, "Guidance", "text/plain",
                m.hasGuidance() ? m.getGuidance() : "None");

        // Transmission Format
        this.addMeasureAttributeWithCodeAndTextValue(xml, "TRANF", codeSystem, "Transmission Format", "text/plain",
                "TBD");

        // TODO: Groups - Seems like HQMF only supports descriptions for one?
        // TODO: Stratification
        if (m.hasGroup()) {
            MeasureGroupComponent mgc = m.getGroupFirstRep();
            this.addGroupMeasureAttributes(xml, codeSystem, mgc);
        }

        // TODO: Supplemental Data Elements - This HQMF measure has a description of the elements
    }

    private void addGroupMeasureAttributes(XMLBuilder2 xml, String codeSystem, MeasureGroupComponent mgc) {
        for  (Map.Entry<String, CodeMapping> entry : measurePopulationValueSetMap.entrySet()) {
            String key = entry.getKey();
            MeasureGroupPopulationComponent mgpc = GetPopulationForKey(key, mgc);
            if (mgpc != null) {
                this.addMeasureAttributeWithCodeAndTextValue(xml, 
                    entry.getValue().code, codeSystem, entry.getValue().displayName, 
                        "text/plain", mgpc.hasDescription() ? mgpc.getDescription() : "None");
            }
        }
    }

    private MeasureGroupPopulationComponent GetPopulationForKey(String key, MeasureGroupComponent mgc) {
        for (MeasureGroupPopulationComponent mgpc : mgc.getPopulation()) {
            if (mgpc.getIdentifier().getValue().equals(key)) {
                return mgpc;
            }
        }

        return null;
    }

    private void addMeasureAttributeWithCodeAndTextValue(XMLBuilder2 xml, String code, String codeSystem, String displayName, String mediaType, String value) {
        XMLBuilder2 temp = this.addMeasureAttribute(xml);
        this.addMeasureAttributeCode(temp, code, codeSystem, displayName);
        this.addMeasureAttributeValue(temp, mediaType, value, "ED"); 
    }

    private void addMeasureAttributeWithCodeAndCodeValue(XMLBuilder2 xml, String code, String codeSystem, String displayName, String valueCode, String valueCodeSystem, String valueDisplayName) {
        XMLBuilder2 temp = this.addMeasureAttribute(xml);
        this.addMeasureAttributeCode(temp, code, codeSystem, displayName);
        this.addMeasureAttributeValue(temp, valueCode, valueCodeSystem, "CD", valueDisplayName);
    }

    private void addMeasureAttributeWithNullAndText(XMLBuilder2 xml, String nullFlavor, String originalText, String mediaType, String value) {
        XMLBuilder2 temp = this.addMeasureAttribute(xml);
        this.addMeasureAttributeCode(temp, nullFlavor, originalText);
        this.addMeasureAttributeValue(temp, mediaType, value, "ED"); 
    }

    private XMLBuilder2 addMeasureAttribute(XMLBuilder2 xml) {
        return xml.root().elem("subjectOf")
            .elem("measureAttribute");
    }

    private void addMeasureAttributeCode(XMLBuilder2 xml, String code, String codeSystem, String displayName) {
        xml.elem("code").a("code", code).a("codeSystem", codeSystem).elem("displayName").a("value", displayName).up().up();
    }

    private void addMeasureAttributeCode(XMLBuilder2 xml, String nullFlavor, String originalText) {
        xml.elem("code").a("nullFlavor", nullFlavor).elem("originalText").a("value", originalText).up().up();
    }

    private void addMeasureAttributeValue(XMLBuilder2 xml, String code, String codeSystem, String xsiType, String displayName) {
        xml.elem("value").a("code", code).a("codeSystem", codeSystem).a("xsi:type", xsiType).elem("displayName").a("value", displayName).up().up();
    }

    private void addMeasureAttributeValue(XMLBuilder2 xml, String mediaType, String value, String xsiType) {
        xml.elem("value").a("mediaType", mediaType).a("value", value).a("xsi:type", xsiType).up();
    }

    private void addComponentOfs(XMLBuilder2 xml, CqfMeasure m) {
        // TODO: Where's the quality measure set? Hedis?
        String qualityMeasureSetId = "a0f96a17-36f0-46d4-bbd5-ad265d81bc95";

        xml.root().elem("componentOf").elem("qualityMeasureSet").a("classCode", "ACT")
            .elem("id").a("root", qualityMeasureSetId).up()
            .elem("title").a("value", "None");
    }

    private void addComponents(XMLBuilder2 xml, CqfMeasure m, String documentGuid, String documentName) {
        this.addDataCriteriaSection(xml, m);
        this.addPopulationCriteriaSection(xml, m, documentGuid, documentName);
    }


    private void addDataCriteriaSection(XMLBuilder2 xml, CqfMeasure m) {
        XMLBuilder2 readyForEntries = this.addDataCriteriaHeader(xml);
    }

    private XMLBuilder2 addDataCriteriaHeader(XMLBuilder2 xml) {
        return xml.root().elem("component")
            .elem("dataCriteriaSection")
                .elem("templateId")
                    .elem("item").a("extension","2017-08-01").a("root", "2.16.840.1.113883.10.20.28.2.6").up().up()
                .elem("code").a("code","57025-9").a("codeSystem","2.16.840.1.113883.6.1").up()
                .elem("title").a("value", "Data Criteria Section").up()
                .elem("text").up();
    }

    // Unlike other functions, this function expects the xml builder to be located
    // at the correct spot. It's also expected to reset the xmlBuilder to the correct spot.
    private void addDataCriteriaEntry(XMLBuilder2 xml, String localVariableName, String criteriaName, String classCode, String itemExtension, String itemRoot,
        String idExtension, String idRoot, String code, String codeSystem, String codeSystemName, String codeDisplayName, String title, String statusCode, String valueSet) {
        xml.elem("entry").a("typeCode", "DRIV")
            .elem("localVariableName").a("value", localVariableName).up()
            .elem(criteriaName).a("classCode", classCode).a("moodCode", "EVN").up()
            .elem("templateId").elem("item").a("extension", itemExtension).a("root", itemRoot).up().up()
            .elem("id").a("extension", idExtension).a("root", idRoot).up()
            .elem("code").a("code", code).a("codeSystem", codeSystem).a("codeSystemName", codeSystemName)
                .elem("displayName").a("value", codeDisplayName).up().up()
            .elem("title").a("value", title).up()
            .elem("statusCode").a("code", statusCode).up()
            .elem("value").a("valueSet", valueSet).a("xsi:type", "CD").up().up();
    }

    private void addPopulationCriteriaSection(XMLBuilder2 xml, CqfMeasure m, String documentGuid, String documentName) {
        XMLBuilder2 readyForComponents = this.addPopulationCriteriaHeader(xml);

        // TODO: How do you do multiple population groups in HQMF
        if (m.hasGroup()) {
            MeasureGroupComponent mgc = m.getGroupFirstRep();
            for (MeasureGroupPopulationComponent mgpc : mgc.getPopulation()) {
                String key = mgpc.getIdentifier().getValue();
                CodeMapping mapping = measurePopulationValueSetMap.get(key);
                this.addPopulationCriteriaComponentCriteria(readyForComponents, mapping.criteriaName, mapping.criteriaExtension, mapping.code, documentName + ".\"" + mgpc.getCriteria() + "\"", documentGuid);
            }
        }

        if (m.hasSupplementalData()) {
            for (MeasureSupplementalDataComponent sde : m.getSupplementalData()) {
                this.addPopulationCriteriaComponentSDE(readyForComponents, UUID.randomUUID().toString(), documentName + ".\"" + sde.getCriteria() + "\"", documentGuid);
            }
        }
    }

    // Unlike other functions, this function expects the xml builder to be located
    // at the correct spot.
    private void addPopulationCriteriaComponentCriteria(XMLBuilder2 xml, String criteriaName, String criteriaIdExtension,
        String code, String criteriaReferenceIdExtension, String criteriaReferenceIdRoot) {
        xml.elem("component").a("typeCode", "COMP")
        .elem(criteriaName).a("classCode", "OBS").a("moodCode", "EVN")
            .elem("id").a("extension", criteriaIdExtension).a("root", UUID.randomUUID().toString()).up()
            .elem("code").a("code", code).a("codeSystem", "2.16.840.1.113883.5.4").a("codeSystemName", "Act Code").up()
            .elem("precondition").a("typeCode", "PRCN")
                .elem("criteriaReference").a("classCode", "OBS").a("moodCode", "EVN")
                    .elem("id").a("extension", criteriaReferenceIdExtension).a("root", criteriaReferenceIdRoot).up().up().up().up().up();
    }

    // Unlike other functions, this function expects the xml builder to be located
    // at the correct spot.
    private void addPopulationCriteriaComponentSDE(XMLBuilder2 xml, String sdeIdRoot, String criteriaReferenceIdExtension, String criteriaReferenceIdRoot) {
        xml.elem("component").a("typeCode", "COMP")
        .elem("cql-ext:supplementalDataElement")
            .elem("id").a("extension", "Supplemental Data Elements").a("root", sdeIdRoot).up()
            .elem("code").a("code", "SDE").a("codeSystem", "2.16.840.1.113883.5.4").a("codeSystemName", "Act Code").up()
            .elem("precondition").a("typeCode", "PRCN")
                .elem("criteriaReference").a("classCode", "OBS").a("moodCode", "EVN")
                    .elem("id").a("extension", criteriaReferenceIdExtension).a("root", criteriaReferenceIdRoot).up().up().up().up().up();
    }

    private XMLBuilder2 addPopulationCriteriaHeader(XMLBuilder2 xml) {
        return xml.root().elem("component")
            .elem("populationCriteriaSection")
                .elem("templateId")
                    .elem("item").a("extension","2017-08-01").a("root", "2.16.840.1.113883.10.20.28.2.7").up().up()
                .elem("code").a("code","57026-7").a("codeSystem","2.16.840.1.113883.6.1").up()
                .elem("title").a("value", "Population Criteria Section").up()
                .elem("text").up();
    }

    private void addResponsibleParties(XMLBuilder2 xml, CqfMeasure m) {

        List<Contributor> contributors = m.getContributor();
        if (contributors != null) {
            for (Contributor c : contributors)
            {
                // TODO: Hard-coded NCQA's OID
                if (c.getType() == ContributorType.AUTHOR) {
                    this.addResponsibleParty(xml, "author", "2.16.840.1.113883.3.464", c.getName());
                }
            }
        }

        // TODO: Hard-coded NCQA's OID
        if (m.getPublisher() != null) {
            this.addResponsibleParty(xml, "publisher", "2.16.840.1.113883.3.464", m.getPublisher());
        }


        // Add verifier 
        // TODO: Not present on the FHIR resource - need and extension?
        // TODO: Hard-coded to National Quality Forum
        this.addResponsibleParty(xml, "verifier", "2.16.840.1.113883.3.560", "National Quality Forum");

    }

    private void addResponsibleParty(XMLBuilder2 xml, String type, String oid, String name) {
        xml.root().elem(type)
            .elem("responsibleParty").a("classCode", "ASSIGNED")
            .elem("representedResponsibleOrganization").a("classCode","ORG").a("determinerCode","INSTANCE")
                .elem("id")
                    .elem("item").a("root", oid).up().up()
                .elem("name")
                    .elem("item")
                       .elem("part").a("value", name);

    }

    private String writeDocument(Document d) {
        try {
            DOMSource source = new DOMSource(d);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);

            transformer.transform(source, result);

            return writer.toString();
        }
        catch (Exception e) {
            return null;
        }
    }


    private boolean validateHQMF(String xml) {
        try {
            return this.validateXML(this.loadHQMFSchema(), xml);
        }
        catch (SAXException e) {
            return false;
        }
    }

    private boolean validateXML(Schema schema, String xml){    
        try {
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xml)));
        } catch (IOException | SAXException e) {
            System.out.println("Exception: " + e.getMessage());
            return false;
        }
        return true;
    }

    private Schema loadHQMFSchema() throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        URL hqmfSchema = ClassLoader.getSystemClassLoader().getResource("hqmf/schemas/EMeasure_N1.xsd");
        return factory.newSchema(hqmfSchema);
    }

    // args[0] == relative path to json measure -> i.e. measure/mesure-demo.json (optional)
    // args[1] == path to resource output -> i.e. library/library-demo.json(optional)
    // args[2] == path to resource output -> i.e. hqmf.xml(optional)
    public static void main(String[] args) {

        try {
            Path pathToMeasure = Paths.get(HQMFProvider.class.getClassLoader().getResource("hqmf/examples/input/measure-cms124-QDM.json").toURI());
            Path pathToLibrary = Paths.get(HQMFProvider.class.getClassLoader().getResource("hqmf/examples/input/library-ccs-logic.json").toURI());
            Path pathToOutput = Paths.get("src/main/resources/hqmf/hqmf.xml").toAbsolutePath();

            if (args.length >= 3) {
                pathToOutput = Paths.get(new URI(args[1]));
            }

            if (args.length >= 2) {
                pathToLibrary = Paths.get(new URI(args[1]));
            }

            if (args.length >= 1) {
                pathToMeasure = Paths.get(new URI(args[0]));
            }

            HQMFProvider provider = new HQMFProvider();

            FhirContext context = FhirContext.forDstu3();

            IParser parser = pathToMeasure.toString().endsWith("json") ? context.newJsonParser() : context.newXmlParser();
            Measure measure = (Measure) parser.parseResource(new FileReader(pathToMeasure.toFile()));

            Library library = (Library) parser.parseResource(new FileReader(pathToLibrary.toFile()));

            CqfMeasure cqfMeasure = new CqfMeasure(measure);
            cqfMeasure.setContent(library.getContent());
            cqfMeasure.setParameter(library.getParameter());
            cqfMeasure.setDataRequirement(library.getDataRequirement());
            cqfMeasure.getRelatedArtifact().addAll(library.getRelatedArtifact());
            
            String result = provider.generateHQMF(cqfMeasure);

            PrintWriter writer = new PrintWriter(new File(pathToOutput.toString()), "UTF-8");
            writer.println(result);
            writer.println();
            writer.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return;
        }
    }
}
