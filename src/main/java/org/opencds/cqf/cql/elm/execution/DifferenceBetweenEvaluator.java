package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Time;

// for Uncertainty
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.runtime.Uncertainty;

import java.util.Arrays;
import java.util.ArrayList;

import org.joda.time.*;

/*
difference in precision between(low DateTime, high DateTime) Integer
difference in precision between(low Time, high Time) Integer

The difference-between operator returns the number of boundaries crossed for the specified precision between the
first and second arguments.
If the first argument is after the second argument, the result is negative.
The result of this operation is always an integer; any fractional boundaries are dropped.
For DateTime values, precision must be one of: years, months, days, hours, minutes, seconds, or milliseconds.
For Time values, precision must be one of: hours, minutes, seconds, or milliseconds.
If either argument is null, the result is null.

Additional Complexity: precison elements above the specified precision must also be accounted for (handled by Joda Time).
For example:
days between DateTime(2012, 5, 5) and DateTime(2011, 5, 0) = 365 + 5 = 370 days

NOTE: This is the same operation as DurationBetween, but the precision after the specified precision is truncated
to get the number of boundaries crossed instead of whole calendar periods.
For Example:
difference in days between DateTime(2014, 5, 12, 12, 10) and DateTime(2014, 5, 25, 15, 55)
will truncate the DateTimes to:
DateTime(2014, 5, 12) and DateTime(2014, 5, 25) respectively
*/

/**
* Created by Chris Schuler on 6/22/2016
*/
public class DifferenceBetweenEvaluator extends org.cqframework.cql.elm.execution.DifferenceBetween {

  public static Integer between(Partial leftTrunc, Partial rightTrunc, int idx, boolean dt) {
    Integer ret = 0;
    switch(idx) {
      case 0: ret = Years.yearsBetween(leftTrunc, rightTrunc).getYears();
              break;
      case 1: ret = Months.monthsBetween(leftTrunc, rightTrunc).getMonths();
              break;
      case 2: ret = Days.daysBetween(leftTrunc, rightTrunc).getDays();
              break;
      case 3: ret = Hours.hoursBetween(leftTrunc, rightTrunc).getHours();
              break;
      case 4: ret = Minutes.minutesBetween(leftTrunc, rightTrunc).getMinutes();
              break;
      case 5: ret = Seconds.secondsBetween(leftTrunc, rightTrunc).getSeconds();
              break;
      case 6: ret = Seconds.secondsBetween(leftTrunc, rightTrunc).getSeconds() * 1000;
              // now do the actual millisecond DifferenceBetween - add to ret
              if (dt) { ret += rightTrunc.getValue(idx) - leftTrunc.getValue(idx); }
              else { ret += rightTrunc.getValue(idx - 3) - leftTrunc.getValue(idx - 3); }
              break;
    }
    return ret;
  }

  @Override
  public Object evaluate(Context context) {
    Object left = getOperand().get(0).evaluate(context);
    Object right = getOperand().get(1).evaluate(context);
    String precision = getPrecision().value();

    if (precision == null) {
      throw new IllegalArgumentException("Precision must be specified.");
    }

    if (left == null || right == null) { return null; }

    if (left instanceof DateTime && right instanceof DateTime) {
      DateTime leftDT = (DateTime)left;
      DateTime rightDT = (DateTime)right;

      int idx = DateTime.getFieldIndex(precision);

      if (idx != -1) {

        // Uncertainty
        if (Uncertainty.isUncertain(leftDT, precision)) {
          ArrayList<DateTime> highLow = Uncertainty.getHighLowList(leftDT, precision);
          return new Uncertainty().withUncertaintyInterval(new Interval(between(highLow.get(1).getPartial(), rightDT.getPartial(), idx, true), true, between(highLow.get(0).getPartial(), rightDT.getPartial(), idx , true), true));
        }

        else if (Uncertainty.isUncertain(rightDT, precision)) {
          ArrayList<DateTime> highLow = Uncertainty.getHighLowList(rightDT, precision);
          return new Uncertainty().withUncertaintyInterval(new Interval(between(leftDT.getPartial(), highLow.get(0).getPartial(), idx, true), true, between(leftDT.getPartial(), highLow.get(1).getPartial(), idx, true), true));
        }

        // truncate Partial
        int [] a = new int[idx + 1];
        int [] b = new int[idx + 1];

        for (int i = 0; i < idx + 1; ++i) {
          a[i] = leftDT.getPartial().getValue(i);
          b[i] = rightDT.getPartial().getValue(i);
        }

        Partial leftTrunc = new Partial(DateTime.getFields(idx + 1), a);
        Partial rightTrunc = new Partial(DateTime.getFields(idx + 1), b);

        return between(leftTrunc, rightTrunc, idx, true);
      }

      else {
        throw new IllegalArgumentException(String.format("Invalid duration precision: %s", precision));
      }
    }

    if (left instanceof Time && right instanceof Time) {
      Time leftT = (Time)left;
      Time rightT = (Time)right;

      int idx = Time.getFieldIndex(precision);

      if (idx != -1) {

        // Uncertainty
        if (Uncertainty.isUncertain(leftT, precision)) {
          ArrayList<Time> highLow = Uncertainty.getHighLowList(leftT, precision);
          return new Uncertainty().withUncertaintyInterval(new Interval(between(highLow.get(1).getPartial(), rightT.getPartial(), idx, false), true, between(highLow.get(0).getPartial(), rightT.getPartial(), idx, false), true));
        }

        else if (Uncertainty.isUncertain(rightT, precision)) {
          ArrayList<Time> highLow = Uncertainty.getHighLowList(rightT, precision);
          return new Uncertainty().withUncertaintyInterval(new Interval(between(leftT.getPartial(), highLow.get(0).getPartial(), idx, false), true, between(leftT.getPartial(), highLow.get(1).getPartial(), idx, false), true));
        }

        // truncate Partial
        int [] a = new int[idx + 1];
        int [] b = new int[idx + 1];

        for (int i = 0; i < idx + 1; ++i) {
          a[i] = leftT.getPartial().getValue(i);
          b[i] = rightT.getPartial().getValue(i);
        }

        Partial leftTrunc = new Partial(Time.getFields(idx + 1), a);
        Partial rightTrunc = new Partial(Time.getFields(idx + 1), b);

        return between(leftTrunc, rightTrunc, idx + 3, false);
      }

      else {
        throw new IllegalArgumentException(String.format("Invalid duration precision: %s", precision));
      }
    }

    throw new IllegalArgumentException(String.format("Cannot DifferenceBetween arguments of type '%s' and '%s'.", left.getClass().getName(), right.getClass().getName()));
  }
}
