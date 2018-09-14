package org.opencds.cqf.builders;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.ISODateTimeFormat;
import org.opencds.cqf.cql.runtime.DateTime;

import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public class JavaDateBuilder extends BaseBuilder<Date> {

    public JavaDateBuilder() {
        super(new Date());
    }

    public JavaDateBuilder buildFromDateTime(DateTime dateTime) {
        org.joda.time.DateTime dt = org.joda.time.DateTime.parse(dateTime.toString(), ISODateTimeFormat.basicDateTime());
        complexProperty = dt.toDate();
        return this;
    }
}
