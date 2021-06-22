package org.opencds.cqf.dstu3.builders;//package org.opencds.cqf.dstu3.builders;

import org.hl7.fhir.dstu3.model.StructureMap;
import org.opencds.cqf.common.builders.BaseBuilder;

public class StructuredMapStructureBuilder extends BaseBuilder<StructureMap.StructureMapStructureComponent> {

    // TODO

    public StructuredMapStructureBuilder() {
        this(new StructureMap.StructureMapStructureComponent());
    }

    public StructuredMapStructureBuilder(StructureMap.StructureMapStructureComponent complexProperty) {
        super(complexProperty);
    }

    // public StructuredMapStructureBuilder buildSource(String s) {
    // complexProperty.setUrl("someUrl");
    // complexProperty.setMode( StructureMap.StructureMapModelMode.SOURCE);
    // return this;
    // }
    //
    // public StructuredMapStructureBuilder buildTarget(String s) {
    // complexProperty.setUrl("someUrl");
    // complexProperty.setMode( StructureMap.StructureMapModelMode.SOURCE);
    // return this;
    // }
}
