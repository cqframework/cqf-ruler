package org.opencds.cqf.providers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.lang3.tuple.Triple;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.CodeDef;
import org.cqframework.cql.elm.execution.CodeSystemDef;
import org.cqframework.cql.elm.execution.ExpressionDef;
import org.cqframework.cql.elm.execution.FunctionDef;
import org.cqframework.cql.elm.execution.IncludeDef;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.ValueSetDef;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.cqframework.cql.tools.formatter.CqlFormatterVisitor;
import org.cqframework.cql.tools.formatter.CqlFormatterVisitor.FormatResult;
import org.hl7.elm.r1.ValueSetRef;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Base64BinaryType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.dstu3.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.dstu3.model.Measure.MeasureSupplementalDataComponent;
import org.hl7.fhir.dstu3.model.ParameterDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.Type;
import org.opencds.cqf.helpers.DataElementType;
import org.opencds.cqf.helpers.LibraryHelper;
import org.opencds.cqf.helpers.LibraryResourceHelper;
import org.opencds.cqf.providers.CqfMeasure.TerminologyRef;
import org.opencds.cqf.providers.CqfMeasure.TerminologyRef.TerminologyRefType;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;

public class DataRequirementsProvider {

    public CqfMeasure createCqfMeasure(Measure measure, Collection<Library> libraries, LibraryResourceProvider libraryResourceProvider)
    {

        String primaryLibraryId = measure.getLibraryFirstRep().getReferenceElement().getIdPart();
        String primaryLibraryName = LibraryResourceHelper.resolveLibraryById(libraryResourceProvider, primaryLibraryId).getName();
        Library primaryLibrary = null;
        for (Library library : libraries) {
            if (library.getIdentifier().getId().equalsIgnoreCase(primaryLibraryName)) {
                primaryLibrary = library;
            }
        }

        CqfMeasure cqfMeasure = new CqfMeasure(measure);

        //Ensure All Data Requirements for all referenced libraries
        org.hl7.fhir.dstu3.model.Library moduleDefinition = this.getDataRequirements(measure, libraries, libraryResourceProvider);

        moduleDefinition.getRelatedArtifact().forEach(x -> cqfMeasure.addRelatedArtifact(x));
        cqfMeasure.setDataRequirement(moduleDefinition.getDataRequirement());
        cqfMeasure.setParameter(moduleDefinition.getParameter());
        
        ArrayList<RelatedArtifact> citations = new ArrayList<>();
        for (RelatedArtifact citation : cqfMeasure.getRelatedArtifact()) {
            if (citation.hasType() && citation.getType().toCode() == "citation" && citation.hasCitation()) {
                citations.add(citation);
            }
        }

        ArrayList<MeasureGroupComponent> populationStatements = new ArrayList<>();
        for (MeasureGroupComponent group : measure.getGroup()) {
            populationStatements.add(group.copy());
        }
        ArrayList<MeasureGroupPopulationComponent> definitionStatements = new ArrayList<>();
        ArrayList<MeasureGroupPopulationComponent> functionStatements = new ArrayList<>();
        ArrayList<MeasureGroupPopulationComponent> supplementalDataElements = new ArrayList<>();
        ArrayList<CqfMeasure.TerminologyRef> terminology = new ArrayList<>();
        ArrayList<CqfMeasure.TerminologyRef > codes = new ArrayList<>();
        ArrayList<CqfMeasure.TerminologyRef > codeSystems = new ArrayList<>();
        ArrayList<CqfMeasure.TerminologyRef > valueSets = new ArrayList<>();
        ArrayList<StringType> dataCriteria = new ArrayList<>();
        ArrayList<org.hl7.fhir.dstu3.model.Library> libraryResources = new ArrayList<>();


        String primaryLibraryCql = "";

        for (Library library : libraries) {
            Boolean isPrimaryLibrary = library == primaryLibrary;
            String libraryNamespace = "";
            if (primaryLibrary.getIncludes() != null) {
                for (IncludeDef include : primaryLibrary.getIncludes().getDef()) {
                    if (library.getIdentifier().getId().equalsIgnoreCase(include.getPath())) {
                        libraryNamespace = include.getLocalIdentifier() + ".";
                    }
                }
            }
            VersionedIdentifier libraryIdentifier = library.getIdentifier();
            org.hl7.fhir.dstu3.model.Library libraryResource = LibraryResourceHelper.resolveLibraryByName(
                libraryResourceProvider,
                libraryIdentifier.getId(),
                libraryIdentifier.getVersion());

            libraryResources.add(libraryResource);

            String cql = "";
            for (Attachment attachment : libraryResource.getContent()) {
                cqfMeasure.addContent(attachment);
                if (attachment.getContentType().equalsIgnoreCase("text/cql")) {
                    cql = new String(attachment.getData());
                }
            }
            if (isPrimaryLibrary) {
                primaryLibraryCql = cql;
            }
            String[] cqlLines = cql.replaceAll("[\r]", "").split("[\n]");
    
            if (library.getStatements() != null) {
                for (ExpressionDef statement : library.getStatements().getDef()) {
                    String[] location = statement.getLocator().split("-");
                    String statementText = "";
                    String signature = "";
                    int start = Integer.parseInt(location[0].split(":")[0]);
                    int end = Integer.parseInt(location[1].split(":")[0]);
                    for (int i = start - 1; i < end; i++) {
                        if (cqlLines[i].contains("define function \"" + statement.getName() + "\"(")) {
                            signature = cqlLines[i].substring(cqlLines[i].indexOf("("), cqlLines[i].indexOf(")") + 1);
                        }
                        if (!cqlLines[i].contains("define \"" + statement.getName() + "\":") && !cqlLines[i].contains("define function \"" + statement.getName() + "\"(")) {
                            statementText = statementText.concat((statementText.length() > 0 ? "\r\n" : "") + cqlLines[i]);
                        }
                    }
                    if (statementText.startsWith("context")) {
                        continue;
                    }
                    MeasureGroupPopulationComponent def = new MeasureGroupPopulationComponent();
                    def.setName(libraryNamespace + statement.getName() + signature);
                    def.setCriteria(statementText);
                    //TODO: Only statements that are directly referenced in the primary library cql will be included.
                    if (statement.getClass() == FunctionDef.class) {
                        if (isPrimaryLibrary || primaryLibraryCql.contains(libraryNamespace + "\"" + statement.getName() + "\"")) {
                            functionStatements.add(def);
                        }
                    }
                    else {
                        if (isPrimaryLibrary || primaryLibraryCql.contains(libraryNamespace + "\"" + statement.getName() + "\"")) {
                            definitionStatements.add(def);
                        }
                    }

                    for (MeasureGroupComponent group : populationStatements) {
                        for (MeasureGroupPopulationComponent population : group.getPopulation()) {
                            if (population.getCriteria() != null && population.getCriteria().equalsIgnoreCase(statement.getName())) {
                                population.setName(statement.getName());
                                population.setCriteria(statementText);
                            }
                        }
                    }

                    for (MeasureSupplementalDataComponent dataComponent : cqfMeasure.getSupplementalData()) {
                        if (dataComponent.getCriteria()!= null && dataComponent.getCriteria().equalsIgnoreCase(def.getName())) {
                            supplementalDataElements.add(def);
                        }
                    }
                }
            }

            if (library.getCodeSystems() != null && library.getCodeSystems().getDef() != null) {
                for (CodeSystemDef codeSystem : library.getCodeSystems().getDef()) {
                    String id = codeSystem.getId().replace("urn:oid:", "");
                    String name = codeSystem.getName();
                    String version = codeSystem.getVersion();

                    CqfMeasure.TerminologyRef term = new CqfMeasure.VersionedTerminologyRef(TerminologyRefType.CODESYSTEM, name, id, version);
                    Boolean exists = false;
                    for (CqfMeasure.TerminologyRef  t : codeSystems) {
                        if (t.getDefinition().equalsIgnoreCase(term.getDefinition())) {
                            exists = true;
                        }
                    }
                    if (!exists) {
                        codeSystems.add(term);
                    }
                }
            }

            if (library.getCodes() != null && library.getCodes().getDef() != null) {
                for (CodeDef code : library.getCodes().getDef()) {
                    String id = code.getId();
                    String name = code.getName();
                    String codeSystemName = code.getCodeSystem().getName();
                    String displayName = code.getDisplay();
                    String codeSystemId = null;

                    for (TerminologyRef rf : codeSystems)
                    {
                        if (rf.getName().equals(codeSystemName)) {
                            codeSystemId = rf.getId();
                            break;
                        }
                    }

                    CqfMeasure.TerminologyRef term = new CqfMeasure.CodeTerminologyRef(name, id, codeSystemName, codeSystemId, displayName);
                    Boolean exists = false;
                    for (CqfMeasure.TerminologyRef  t : codes) {
                        if (t.getDefinition().equalsIgnoreCase(term.getDefinition())) {
                            exists = true;
                        }
                    }
                    if (!exists) {
                        codes.add(term);
                    }
                }
            }

            if (library.getValueSets() != null && library.getValueSets().getDef() != null) {
                for (ValueSetDef valueSet : library.getValueSets().getDef()) {
                    String id = valueSet.getId().replace("urn:oid:", "");
                    String name = valueSet.getName();

                    CqfMeasure.TerminologyRef term = new CqfMeasure.VersionedTerminologyRef(TerminologyRefType.VALUESET, name, id);
                    Boolean exists = false;
                    for (CqfMeasure.TerminologyRef  t : valueSets) {
                        if (t.getDefinition().equalsIgnoreCase(term.getDefinition())) {
                            exists = true;
                        }
                    }
                    if (!exists) {
                        valueSets.add(term);
                    }
                  
                    for (DataRequirement data : cqfMeasure.getDataRequirement()) {
                        String type = data.getType();
                        try {
                            DataElementType dataType = DataElementType.valueOf(type.toUpperCase());
                            type = dataType.toString();
                        } catch (Exception e) {
                            //Do Nothing.  Leave type as is.
                        }

                        for (DataRequirementCodeFilterComponent filter : data.getCodeFilter()) {
                            if (filter.hasValueSetStringType() && filter.getValueSetStringType().getValueAsString().equalsIgnoreCase(valueSet.getId())) {
                                StringType dataElement = new StringType();
                                dataElement.setValueAsString("\"" + type + ": " + name + "\" using \"" + name + " (" + id + ")");
                                exists = false;
                                for (StringType string : dataCriteria) {
                                    if (string.getValueAsString().equalsIgnoreCase(dataElement.getValueAsString())) {
                                        exists = true;
                                    }
                                }
                                if (!exists) {
                                    dataCriteria.add(dataElement);
                                }
                            }
                        }
                    }
                }
            }
        }

        Comparator stringTypeComparator = new Comparator<StringType>() {
            @Override
            public int compare(StringType item, StringType t1) {
                String s1 = item.asStringValue();
                String s2 = t1.asStringValue();
                return s1.compareToIgnoreCase(s2);
            }
        };

        Comparator terminologyRefComparator = new Comparator<TerminologyRef>() {
            @Override
            public int compare(TerminologyRef item, TerminologyRef t1) {
                String s1 = item.getDefinition();
                String s2 = t1.getDefinition();
                return s1.compareToIgnoreCase(s2);
            }
        };
        Comparator populationComparator = new Comparator<MeasureGroupPopulationComponent>() {
            @Override
            public int compare(MeasureGroupPopulationComponent item, MeasureGroupPopulationComponent t1) {
                String s1 = item.getName();
                String s2 = t1.getName();
                return s1.compareToIgnoreCase(s2);
            }
        };

        Collections.sort(definitionStatements, populationComparator);
        Collections.sort(functionStatements, populationComparator);
        Collections.sort(supplementalDataElements, populationComparator);
        Collections.sort(codeSystems, terminologyRefComparator);
        Collections.sort(codes, terminologyRefComparator);
        Collections.sort(valueSets, terminologyRefComparator);
        Collections.sort(dataCriteria, stringTypeComparator);

        terminology.addAll(codeSystems);
        terminology.addAll(codes);
        terminology.addAll(valueSets);
        
        cqfMeasure.setPopulationStatements(populationStatements);
        cqfMeasure.setDefinitionStatements(definitionStatements);
        cqfMeasure.setFunctionStatements(functionStatements);
        cqfMeasure.setSupplementalDataElements(supplementalDataElements);
        cqfMeasure.setTerminology(terminology);
        cqfMeasure.setDataCriteria(dataCriteria);
        cqfMeasure.setLibraries(libraryResources);
        cqfMeasure.setCitations(citations);


        Map<String, List<Triple<Integer, String, String>>> criteriaMap = new HashMap<>();
        // Index all usages of criteria
        for (int i = 0; i < cqfMeasure.getGroup().size(); i++) {
            MeasureGroupComponent mgc = cqfMeasure.getGroup().get(i);
            for (int j = 0; j < mgc.getPopulation().size(); j++) {
                MeasureGroupPopulationComponent mgpc = mgc.getPopulation().get(j);
                String criteria = mgpc.getCriteria();
                if (criteria != null && !criteria.isEmpty()) {
                    if (!criteriaMap.containsKey(criteria)) {
                        criteriaMap.put(criteria, new ArrayList<Triple<Integer, String, String>>());
                    }

                    criteriaMap.get(criteria).add(Triple.of(i, mgpc.getCode().getCodingFirstRep().getCode(), mgpc.getDescription()));
                }
            }
        }

        // Find shared usages
        for (Entry<String, List<Triple<Integer, String, String>>> entry : criteriaMap.entrySet()) {
            String criteria = entry.getKey();
            
            if (cqfMeasure.getGroup().size() == 1 || entry.getValue().size() > 1) {
                cqfMeasure.addSharedPopulationCritiera(criteria, entry.getValue().get(0).getRight());
            }
        }

        // If there's only one group every critieria was shared. Kill the group.
        if (cqfMeasure.getGroup().size() == 1) {
            cqfMeasure.getGroup().clear();
        }
        // Otherwise, remove the shared components.
        else {
            for (int i = 0; i < cqfMeasure.getGroup().size(); i++) {
                MeasureGroupComponent mgc = cqfMeasure.getGroup().get(i);
                List<MeasureGroupPopulationComponent> newMgpc = new ArrayList<MeasureGroupPopulationComponent>();
                for (int j = 0; j < mgc.getPopulation().size(); j++) {
                    MeasureGroupPopulationComponent mgpc = mgc.getPopulation().get(j);
                    if (mgpc.hasCriteria() && !mgpc.getCriteria().isEmpty() && !cqfMeasure.getSharedPopulationCritieria().containsKey(mgpc.getCriteria())) {
                        newMgpc.add(mgpc);
                    }
                }

                mgc.setPopulation(newMgpc);
            }
        }

        return cqfMeasure;
    }

    public org.hl7.fhir.dstu3.model.Library getDataRequirements(Measure measure, Collection<org.cqframework.cql.elm.execution.Library> libraries, LibraryResourceProvider libraryResourceProvider){
        List<DataRequirement> reqs = new ArrayList<>();
        List<RelatedArtifact> dependencies = new ArrayList<>();
        List<ParameterDefinition> parameters = new ArrayList<>();

        for (Library library : libraries) {
            VersionedIdentifier primaryLibraryIdentifier = library.getIdentifier();
            org.hl7.fhir.dstu3.model.Library libraryResource = LibraryResourceHelper.resolveLibraryByName(
                libraryResourceProvider,
                primaryLibraryIdentifier.getId(),
                primaryLibraryIdentifier.getVersion());
    
            for (RelatedArtifact dependency : libraryResource.getRelatedArtifact()) {
                if (dependency.getType().toCode().equals("depends-on")) {
                    dependencies.add(dependency);
                }
            }

            reqs.addAll(libraryResource.getDataRequirement());
            parameters.addAll(libraryResource.getParameter());
        }

        List<Coding> typeCoding = new ArrayList<>();
        typeCoding.add(new Coding().setCode("module-definition"));
        org.hl7.fhir.dstu3.model.Library library =
                new org.hl7.fhir.dstu3.model.Library().setType(new CodeableConcept().setCoding(typeCoding));

        if (!dependencies.isEmpty()) {
            library.setRelatedArtifact(dependencies);
        }

        if (!reqs.isEmpty()) {
            library.setDataRequirement(reqs);
        }

        if (!parameters.isEmpty()) {
            library.setParameter(parameters);
        }

        return library;
    }


    public CqlTranslator getTranslator(org.hl7.fhir.dstu3.model.Library library, LibraryManager libraryManager, ModelManager modelManager) {
        Attachment cql = null;
        for (Attachment a : library.getContent()) {
            if (a.getContentType().equals("text/cql")) {
                cql = a;
                break;
            }
        }

        if (cql == null) {
            return null;
        }

        CqlTranslator translator = LibraryHelper.getTranslator(
                new ByteArrayInputStream(Base64.getDecoder().decode(cql.getDataElement().getValueAsString())),
                libraryManager, modelManager);

        return translator;
    }

    public void formatCql(org.hl7.fhir.dstu3.model.Library library) {
        for (Attachment att : library.getContent()) {
            if (att.getContentType().equals("text/cql")) {
                try {
                    FormatResult fr = CqlFormatterVisitor.getFormattedOutput(new ByteArrayInputStream(
                            Base64.getDecoder().decode(att.getDataElement().getValueAsString())));

                    // Only update the content if it's valid CQL.
                    if (fr.getErrors().size() == 0) {
                        Base64BinaryType bt = new Base64BinaryType(
                                new String(Base64.getEncoder().encode(fr.getOutput().getBytes())));
                        att.setDataElement(bt);
                    }
                } catch (IOException e) {
                    // Intentionally empty for now
                }
            }
        }
    }

    public void ensureElm(org.hl7.fhir.dstu3.model.Library library, CqlTranslator translator) {

        library.getContent().removeIf(a -> a.getContentType().equals("application/elm+xml"));
        String xml = translator.toXml();
        Attachment elm = new Attachment();
        elm.setContentType("application/elm+xml");
        elm.setData(xml.getBytes());
        library.getContent().add(elm);
    }

    public void ensureRelatedArtifacts(org.hl7.fhir.dstu3.model.Library library, CqlTranslator translator, LibraryResourceProvider libraryResourceProvider)
    {
        library.getRelatedArtifact().clear();
        org.hl7.elm.r1.Library elm = translator.toELM();
        if (elm.getIncludes() != null && !elm.getIncludes().getDef().isEmpty()) {
            for (org.hl7.elm.r1.IncludeDef def : elm.getIncludes().getDef()) {
                library.addRelatedArtifact(new RelatedArtifact().setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                        .setResource(new Reference().setReference(
                                LibraryResourceHelper.resolveLibraryByName(libraryResourceProvider, def.getPath(), def.getVersion()).getId())));
            }
        }

        if (elm.getUsings() != null && !elm.getUsings().getDef().isEmpty()) {
            for (org.hl7.elm.r1.UsingDef def : elm.getUsings().getDef()) {
                String uri = def.getUri();
                String version = def.getVersion();
                if (version != null && !version.isEmpty()) {
                    uri = uri + "|" + version;
                }
                library.addRelatedArtifact(
                        new RelatedArtifact().setType(RelatedArtifact.RelatedArtifactType.DEPENDSON).setUrl(uri));
            }
        }
    }

    public void ensureDataRequirements(org.hl7.fhir.dstu3.model.Library library, CqlTranslator translator) {
        library.getDataRequirement().clear();

        List<DataRequirement> reqs = new ArrayList<DataRequirement>();

        for (org.hl7.elm.r1.Retrieve retrieve : translator.toRetrieves()) {
            DataRequirement dataReq = new DataRequirement();
            dataReq.setType(retrieve.getDataType().getLocalPart());
            if (retrieve.getCodeProperty() != null) {
                DataRequirement.DataRequirementCodeFilterComponent codeFilter = new DataRequirement.DataRequirementCodeFilterComponent();
                codeFilter.setPath(retrieve.getCodeProperty());
                if (retrieve.getCodes() instanceof ValueSetRef) {
                    Type valueSetName = new StringType(
                            getValueSetId(((ValueSetRef) retrieve.getCodes()).getName(), translator));
                    codeFilter.setValueSet(valueSetName);
                }
                dataReq.setCodeFilter(Collections.singletonList(codeFilter));
            }
            // TODO - Date filters - we want to populate this with a $data-requirements
            // request as there isn't a good way through elm analysis
            reqs.add(dataReq);
        }


        // 
        // org.hl7.elm.r1.Library elm = translator.toELM();
        // Codes codes = elm.getCodes();
        // for (CodeDef cd : codes.getDef()) {
        //     cd.
        // }

        library.setDataRequirement(reqs);     
    }

    public String getValueSetId(String valueSetName, CqlTranslator translator) {
        org.hl7.elm.r1.Library.ValueSets valueSets = translator.toELM().getValueSets();
        if (valueSets != null) {
            for (org.hl7.elm.r1.ValueSetDef def : valueSets.getDef()) {
                if (def.getName().equals(valueSetName)) {
                    return def.getId();
                }
            }
        }

        return valueSetName;
    }

}