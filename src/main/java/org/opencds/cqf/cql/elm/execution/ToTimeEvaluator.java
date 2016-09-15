package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Time;
import org.joda.time.DateTime;
import org.joda.time.Partial;
import java.math.BigDecimal;

/*
ToTime(argument String) Time

The ToTime operator converts the value of its argument to a Time value.
The operator expects the string to be formatted using ISO-8601 time representation:
  Thh:mm:ss.fff(+|-)hh:mm
In addition, the string must be interpretable as a valid time-of-day value.
For example, the following are valid string representations for time-of-day values:
'T14:30:00.0Z'                // 2:30PM UTC
'T14:30:00.0-07:00'           // 2:30PM Mountain Standard (GMT-7:00)
If the input string is not formatted correctly, or does not represent a valid time-of-day value, a run-time error is thrown.
As with time-of-day literals, time-of-day values may be specified to any precision.
If no timezone is supplied, the timezone of the evaluation request timestamp is assumed.
If the argument is null, the result is null.
*/

/**
* Created by Chris Schuler on 7/12/2016
*/
public class ToTimeEvaluator extends org.cqframework.cql.elm.execution.ToTime {

  public static BigDecimal getTimezone(String isoTimeString) {
    BigDecimal tz = new BigDecimal(new DateTime(isoTimeString).getZone().getOffset(0) / 3600000.0);
    if (isoTimeString.indexOf('+') != -1) {
      String[] temp = isoTimeString.split("\\+");
      String[] temp2 = temp[1].split(":");
      Double hour = Double.parseDouble(temp2[0]);
      Double minute = Integer.parseInt(temp2[1]) / 60.0;
      tz = new BigDecimal(hour + minute);
    }
    else if (isoTimeString.indexOf('-') != -1) {
      String[] temp = isoTimeString.split("-");
      String[] temp2 = temp[1].split(":");
      Double hour = Double.parseDouble(temp2[0]);
      Double minute = Integer.parseInt(temp2[1]) / 60.0;
      tz = new BigDecimal(hour + minute).negate();
    }
    return tz;
  }

  @Override
  public Object evaluate(Context context) {
    Object operand = getOperand().evaluate(context);

    if (operand == null) { return null; }

    String[] timeAndTimezone = operand.toString().replace('T', ' ').replace('Z', ' ').trim().split("[\\+-]");
    String[] time = timeAndTimezone[0].split("\\W");
    int[] values = new int[time.length];
    for (int i = 0; i < values.length; ++i) {
      values[i] = Integer.parseInt(time[i]);
    }

    return new Time().withPartial(new Partial(Time.getFields(values.length), values)).withTimezoneOffset(getTimezone(operand.toString()));
  }
}
