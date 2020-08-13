package org.opencds.cqf.r4.builders;

import java.util.Date;

import org.opencds.cqf.common.builders.BaseBuilder;
import org.opencds.cqf.cql.engine.runtime.DateTime;

public class JavaDateBuilder extends BaseBuilder<Date> {

    public JavaDateBuilder() {
        super(new Date());
    }

    public JavaDateBuilder buildFromDateTime(DateTime dateTime) {
        complexProperty = Date.from(dateTime.getDateTime().toInstant());
        return this;
    }
}
