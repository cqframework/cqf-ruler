package org.opencds.cqf.cql.runtime;

import org.joda.time.Partial;
import org.joda.time.DateTimeFieldType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.text.*;
import java.util.Date;

/**
* Created by Chris Schuler on 6/20/2016
*/
public class DateTime {

  protected Partial dateTime;
  protected BigDecimal timezoneOffset;

  protected static final DateTimeFieldType[] fields = new DateTimeFieldType[] {
    DateTimeFieldType.year(),
    DateTimeFieldType.monthOfYear(),
    DateTimeFieldType.dayOfMonth(),
    DateTimeFieldType.hourOfDay(),
    DateTimeFieldType.minuteOfHour(),
    DateTimeFieldType.secondOfMinute(),
    DateTimeFieldType.millisOfSecond(),
  };

  public static int[] getValues(Integer... values) {
    int count = 0;
    int[] temp = new int[7];
    for (Integer value : values) {
      if (value != null) {
        temp[count] = value;
        ++count;
      }
    }
    return Arrays.copyOf(temp, count);
  }

  public static DateTimeFieldType[] getFields(int numFields) {
    DateTimeFieldType[] ret = new DateTimeFieldType[numFields];
    for (int i = 0; i < numFields; ++i) {
      ret[i] = fields[i];
    }
    return ret;
  }

  public static DateTimeFieldType getField(int idx) {
    return fields[idx];
  }

  public static int getFieldIndex(String dateTimeElement) {
    // DateTimePrecision Enum represents precision as Titlecase Strings
    ArrayList<String> indexes = new ArrayList<>(Arrays.asList("year", "month", "day", "hour", "minute", "second", "millisecond"));
    return indexes.indexOf(dateTimeElement.toLowerCase());
  }

  public static int getFieldIndex2(String dateTimeElement) {
    ArrayList<String> indexes = new ArrayList<>(Arrays.asList("years", "months", "days", "hours", "minutes", "seconds", "milliseconds"));
    return indexes.indexOf(dateTimeElement.toLowerCase());
  }

  public static String getUnit(int idx) {
    switch (idx) {
      case 0: return "years";
      case 1: return "months";
      case 2: return "days";
      case 3: return "hours";
      case 4: return "minutes";
      case 5: return "seconds";
      case 6: return "milliseconds";
    }
    throw new IllegalArgumentException("Invalid index for DateTime unit request.");
  }

  public Partial getPartial() {
    return dateTime;
  }

  public void setPartial(Partial newDateTime) {
    dateTime = newDateTime;
  }

  public DateTime withPartial(Partial newDateTime) {
    setPartial(newDateTime);
    return this;
  }

  public BigDecimal getTimezoneOffset() {
    return timezoneOffset;
  }

  public void setTimezoneOffset(BigDecimal newTimezoneOffset) {
    timezoneOffset = newTimezoneOffset;
  }

  public DateTime withTimezoneOffset(BigDecimal newTimezoneOffset) {
    setTimezoneOffset(newTimezoneOffset);
    return this;
  }

  public static DateTime fromJavaDate(Date date) {
    if (date == null) {
      return null;
    }
    return fromJodaDateTime(new org.joda.time.DateTime(date));
  }

  public static DateTime fromJodaDateTime(org.joda.time.DateTime dt) {
    int [] values = { dt.year().get(), dt.monthOfYear().get(), dt.dayOfMonth().get(), dt.hourOfDay().get(),
            dt.minuteOfHour().get(), dt.secondOfMinute().get(), dt.millisOfSecond().get() };
    return new DateTime().withPartial(new Partial(fields, values)).withTimezoneOffset(new BigDecimal(0));
  }

  public static DateTime getToday() {
    org.joda.time.DateTime dt = org.joda.time.DateTime.now();
    int [] values = { dt.year().get(), dt.monthOfYear().get(), dt.dayOfMonth().get(), 0, 0, 0, 0 };
    return new DateTime().withPartial(new Partial(fields, values)).withTimezoneOffset(new BigDecimal(0));
  }

  public static DateTime getNow() {
    org.joda.time.DateTime dt = org.joda.time.DateTime.now();
    int [] values = { dt.year().get(), dt.monthOfYear().get(), dt.dayOfMonth().get(), dt.hourOfDay().get(),
                      dt.minuteOfHour().get(), dt.secondOfMinute().get(), dt.millisOfSecond().get() };
    return new DateTime().withPartial(new Partial(fields, values)).withTimezoneOffset(new BigDecimal(0));
  }

  public static DateTime expandPartialMin(DateTime dt, int size) {
    for (int i = dt.getPartial().size(); i < size; ++i) {
      dt.setPartial(dt.getPartial().with(DateTime.getField(i), DateTime.getField(i).getField(null).getMinimumValue()));
    }
    return dt;
  }

  public static DateTime expandPartialMax(DateTime dt, int size, int maxPrecision) {
    for (int i = dt.getPartial().size(); i < size; ++i) {
      // only want to max values up to the missing precision
      if (i > maxPrecision) {
        dt.setPartial(dt.getPartial().with(DateTime.getField(i), DateTime.getField(i).getField(null).getMinimumValue()));
      }
      else if (i == 2) {
        dt.setPartial(dt.getPartial().with(getField(i), dt.getPartial().getChronology().dayOfMonth().getMaximumValue(dt.getPartial())));
      }
      else {
        dt.setPartial(dt.getPartial().with(getField(i), DateTime.getField(i).getField(null).getMaximumValue()));
      }
    }
    return dt;
  }

  public static Boolean formatCheck(ArrayList<Object> timeElements) {
    boolean prevNull = false;
    for (Object element : timeElements) {
      if (element == null) { prevNull = true; }
      else if (element != null && prevNull) {
        return false;
      }
    }
    return true;
  }
}
