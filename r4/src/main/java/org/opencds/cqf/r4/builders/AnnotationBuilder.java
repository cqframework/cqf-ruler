package org.opencds.cqf.r4.builders;

import java.util.Date;

import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.common.builders.BaseBuilder;

public class AnnotationBuilder extends BaseBuilder<Annotation> {

    public AnnotationBuilder() {
        super(new Annotation());
    }

    // Type is one of the following: Reference or String
    public AnnotationBuilder buildAuthor(Type choice) {
        complexProperty.setAuthor(choice);
        return this;
    }

    public AnnotationBuilder buildTime(Date date) {
        complexProperty.setTime(date);
        return this;
    }

    // required
    public AnnotationBuilder buildText(String text) {
        complexProperty.setText(text);
        return this;
    }
}
