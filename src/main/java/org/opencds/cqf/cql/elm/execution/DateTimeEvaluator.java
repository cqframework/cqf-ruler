package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.ArrayList;

import org.joda.time.Partial;

/*
simple type DateTime

The DateTime type represents date and time values with potential uncertainty within CQL.
CQL supports date and time values in the range @0001-01-01T00:00:00.0 to @9999-12-31T23:59:59.999 with a 1 millisecond step size.
*/

/**
 * Created by Chris Schuler on 6/20/2016
 */
public class DateTimeEvaluator extends org.cqframework.cql.elm.execution.DateTime {

  @Override
  public Object evaluate(Context context) {
    Integer year = (this.getYear() == null) ? null : (Integer)this.getYear().evaluate(context);
    if (year == null) { return null; }

    if (year < 1) {
      throw new IllegalArgumentException(String.format("The year: %d falls below the accepted bounds of 0001-9999.", year));
    }

    else if (year > 9999) {
      throw new IllegalArgumentException(String.format("The year: %d falls above the accepted bounds of 0001-9999.", year));
    }

    Integer month = (this.getMonth() == null) ? null : (Integer)this.getMonth().evaluate(context);
    Integer day = (this.getDay() == null) ? null : (Integer)this.getDay().evaluate(context);
    Integer hour = (this.getHour() == null) ? null : (Integer)this.getHour().evaluate(context);
    Integer minute = (this.getMinute() == null) ? null : (Integer)this.getMinute().evaluate(context);
    Integer second = (this.getSecond() == null) ? null : (Integer)this.getSecond().evaluate(context);
    Integer millis = (this.getMillisecond() == null) ? null : (Integer)this.getMillisecond().evaluate(context);
    // if no timezone default to system timezone
    BigDecimal offset = (this.getTimezoneOffset() == null) ? new BigDecimal(new org.joda.time.DateTime().getZone().getOffset(0) / 3600000.0) : (BigDecimal)this.getTimezoneOffset().evaluate(context);

    org.opencds.cqf.cql.runtime.DateTime dt = new org.opencds.cqf.cql.runtime.DateTime();

    if (dt.formatCheck(new ArrayList<Object>(Arrays.asList(year, month, day, hour, minute, second, millis)))) {
      int [] values = dt.getValues(year, month, day, hour, minute, second, millis);
      return dt.withPartial(new Partial(dt.getFields(values.length), values)).withTimezoneOffset(offset);
    }
    else {
      throw new IllegalArgumentException("DateTime format is invalid");
    }
  }
}
