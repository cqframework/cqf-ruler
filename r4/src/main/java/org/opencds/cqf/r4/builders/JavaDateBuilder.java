package org.opencds.cqf.r4.builders;

import org.opencds.cqf.builders.BaseBuilder;
import org.opencds.cqf.cql.runtime.DateTime;
import java.util.Date;

public class JavaDateBuilder extends BaseBuilder<Date> {

    public JavaDateBuilder() {
        super(new Date());
    }

    public JavaDateBuilder buildFromDateTime(DateTime dateTime) {
        complexProperty = Date.from(dateTime.getDateTime().toInstant());
        return this;
    }
}
