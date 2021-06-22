package org.opencds.cqf.r4.builders;//package org.opencds.cqf.r4.builders;

import org.hl7.fhir.r4.model.StructureMap;
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
