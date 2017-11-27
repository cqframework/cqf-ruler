package org.opencds.cqf.builders;

import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.RequestGroup;

import java.util.List;

public class RequestGroupBuilder extends BaseBuilder<RequestGroup> {

    public RequestGroupBuilder(RequestGroup complexProperty) {
        super(complexProperty);
    }

    // TODO - incomplete

    public RequestGroupBuilder buildAction(List<RequestGroup.RequestGroupActionComponent> actions) {
        complexProperty.setAction(actions);
        return this;
    }

    public RequestGroupBuilder buildExtension(List<Extension> extensions) {
        complexProperty.setExtension(extensions);
        return this;
    }
}
