package org.opencds.cqf.r4.builders;

import java.util.UUID;

import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.StructureMap;
import org.opencds.cqf.common.builders.BaseBuilder;

public class StructuredMapBuilder extends BaseBuilder<StructureMap> {

    public StructuredMapBuilder() {
        super(new StructureMap());
    }

    public StructuredMapBuilder(StructureMap activityDefinition) {
        super(activityDefinition);
    }

    public StructuredMapBuilder buildUrl(String url) {
        complexProperty.setUrl(UUID.randomUUID().toString());
        return this;
    }

    public StructuredMapBuilder buildRandomUrl() {
        return buildUrl("urn:uuid:" + UUID.randomUUID().toString());
    }

    public StructuredMapBuilder buildName(String name) {
        complexProperty.setName(name);
        return this;
    }

    public StructuredMapBuilder buildStatus(Enumerations.PublicationStatus publicationStatus) {
        complexProperty.setStatus(publicationStatus);
        return this;
    }

    public StructuredMapBuilder buildPublisher(String publisher) {
        complexProperty.setPublisher(publisher);
        return this;
    }

    public StructuredMapBuilder buildGroup(StructureMap.StructureMapGroupComponent group) {
        complexProperty.addGroup(group);
        return this;
    }

    public StructuredMapBuilder buildStructure(StructureMap.StructureMapStructureComponent structure) {
        complexProperty.addStructure(structure);
        return this;
    }

    public StructuredMapBuilder buildStructure(StructureMap.StructureMapModelMode mode, String fhirType) {
        StructureMap.StructureMapStructureComponent structureMapStructureComponent = new StructureMap.StructureMapStructureComponent();
        structureMapStructureComponent.setMode(mode);
        structureMapStructureComponent.setUrl("http://hl7.org/fhir/StructureDefinition/" + fhirType.toLowerCase());
        complexProperty.addStructure(structureMapStructureComponent);
        return this;
    }

    public StructuredMapBuilder buildStructureTarget(String fhirType) {
        return buildStructure(StructureMap.StructureMapModelMode.TARGET, fhirType);
    }

    public StructuredMapBuilder buildStructureSource(String fhirType) {
        return buildStructure(StructureMap.StructureMapModelMode.SOURCE, fhirType);
    }

    public StructuredMapBuilder buildDescription(String description) {
        complexProperty.setDescription(description);
        return this;
    }
}
