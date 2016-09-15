package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.DateTime;
import org.joda.time.Partial;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

/*
ToDateTime(argument String) DateTime

The ToDateTime operator converts the value of its argument to a DateTime value.
The operator expects the string to be formatted using the ISO-8601 date/time representation:
  YYYY-MM-DDThh:mm:ss.fff(+|-)hh:mm
In addition, the string must be interpretable as a valid date/time value.
For example, the following are valid string representations for date/time values:
'2014-01-01'                  // January 1st, 2014
'2014-01-01T14:30:00.0Z'      // January 1st, 2014, 2:30PM UTC
'2014-01-01T14:30:00.0-07:00' // January 1st, 2014, 2:30PM Mountain Standard (GMT-7:00)
If the input string is not formatted correctly, or does not represent a valid date/time value, a run-time error is thrown.
As with date/time literals, date/time values may be specified to any precision. If no timezone is supplied,
  the timezone of the evaluation request timestamp is assumed.
If the argument is null, the result is null.
*/

/**
* Created by Chris Schuler on 7/11/2016
*/
public class ToDateTimeEvaluator extends org.cqframework.cql.elm.execution.ToDateTime {

  public static String removeTimezone(String isoDateString) {
    if (isoDateString.indexOf('+') != -1) {
      return isoDateString.split("\\+")[0];
    }
    else if (isoDateString.lastIndexOf('-') > 7) {
      String[] temp = isoDateString.split("T");
      String[] temp2 = temp[1].split("-");
      return temp[0] + "T" + temp2[0];
    }
    else if(isoDateString.indexOf('Z') != -1) {
      return isoDateString.replace('Z', ' ').trim();
    }
    return isoDateString;
  }

  public static BigDecimal getTimezone(String isoDateString) {
    BigDecimal tz = new BigDecimal(new org.joda.time.DateTime(isoDateString).getZone().getOffset(0) / 3600000.0);
    if (isoDateString.indexOf('+') != -1) {
      String[] temp = isoDateString.split("\\+");
      String[] temp2 = temp[1].split(":");
      Double hour = Double.parseDouble(temp2[0]);
      Double minute = Integer.parseInt(temp2[1]) / 60.0;
      tz = new BigDecimal(hour + minute);
    }
    else if (isoDateString.lastIndexOf('-') > 7) {
      String[] temp = isoDateString.split("T");
      String[] temp2 = temp[1].split("-");
      String[] temp3 = temp2[1].split(":");
      Double hour = Double.parseDouble(temp3[0]);
      Double minute = Integer.parseInt(temp3[1]) / 60.0;
      tz = new BigDecimal(hour + minute).negate();
    }
    return tz;
  }

  public static int[] getValues(org.joda.time.DateTime dt, int numElements) {
    List<Integer> values = new ArrayList<Integer>();
    for (int i = 0; i < numElements; ++i) {
      values.add(dt.property(DateTime.getField(i)).get());
    }
    return values.stream().mapToInt(i -> i).toArray();
  }

  /**
  * This method is necessary because the cstor for DateTime(isoDateString) will autofill missing elements.
  * For example:
  * DateTime("2014-01-01") returns "2014-01-01T00:00:00.0-7:00(GMT-7:00)"
  * This unwanted functionality results in a Partial that has more elements than what was specified.
  * As a way around this, I have developed a simple divide and conquer algorithm to determine the number of elements
  * from the original string retrieved from the evaluate call for the operand.
  * NOTE: the timezone logic will not be handled here.
  * Steps:
  * 1. Divide string into DATE and TIME parts by splitting on "T"
  * 2. Check size of resulting String array for TIME component (>1)
  * 2a. If TIME component exists, split into TIME and TIMEZONE parts - TIMEZONE will be ignored
  * 2b. This is achieved by first splitting on (+|-) for the (+|-)hh:mm timezone format
  * 2c. Then splitting on (\W) for the Z timezone format
  * 3. Get the number of DATE elements by splitting the DATE part on (-) and recording the length of the resulting array
  * 4. Return the sum of the DATE and TIME parts for the number of elements
  */
  public static Integer numElements(String isoDateString) {
    Integer dateSize = 0;
    Integer timeSize = 0;
    // Step 1
    String[] dateAndTime = isoDateString.split("T");
    // Step 2
    if (dateAndTime.length > 1) {
      // Step 2a & 2b
      String[] timeAndTimezone = dateAndTime[1].split("[+-]");
      // Step 2c
      timeSize = timeAndTimezone[0].split("\\W").length;
    }
    // Step 3
    dateSize = dateAndTime[0].split("-").length;
    // Step 4
    return dateSize + timeSize;
  }

  @Override
  public Object evaluate(Context context) {
    Object operand = getOperand().evaluate(context);

    if (operand == null) { return null; }

    int[] values = getValues(new org.joda.time.DateTime(removeTimezone(operand.toString())), numElements(operand.toString()));

    return new DateTime().withPartial(new Partial(DateTime.getFields(values.length), values)).withTimezoneOffset(getTimezone(operand.toString()));
  }
}
