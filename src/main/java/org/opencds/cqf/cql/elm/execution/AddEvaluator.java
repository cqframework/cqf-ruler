package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Quantity;
import org.opencds.cqf.cql.runtime.Time;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Uncertainty;
import org.opencds.cqf.cql.runtime.Interval;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.ArrayList;

/*
*** NOTES FOR ARITHMETIC OPERATORS ***
+(left Integer, right Integer) Integer
+(left Decimal, right Decimal) Decimal
+(left Quantity, right Quantity) Quantity

The add (+) operator performs numeric addition of its arguments.
When invoked with mixed Integer and Decimal arguments, the Integer argument will be implicitly converted to Decimal.
TODO: When adding quantities, the dimensions of each quantity must be the same, but not necessarily the unit.
  For example, units of 'cm' and 'm' can be added, but units of 'cm2' and  'cm' cannot.
    The unit of the result will be the most granular unit of either input.
If either argument is null, the result is null.


*** NOTES FOR DATETIME ***
+(left DateTime, right Quantity) DateTime
+(left Time, right Quantity) Time

The add (+) operator returns the value of the given date/time, incremented by the time-valued quantity,
  respecting variable length periods for calendar years and months.
For DateTime values, the quantity unit must be one of: years, months, days, hours, minutes, seconds, or milliseconds.
For Time values, the quantity unit must be one of: hours, minutes, seconds, or milliseconds.
The operation is performed by attempting to derive the highest granularity precision first, working down successive
  granularities to the granularity of the time-valued quantity. For example, the following addition:
    DateTime(2014) + 24 months
    This example results in the value DateTime(2016) even though the date/time value is not specified to the level
      of precision of the time-valued quantity.
    NOTE: this implementation (v3) returns a DateTime which truncates minimum element values until the original DateTime's precision is reached.
    For Example:
      DateTime(2014) + 735 days
        returns DateTime(2016)
If either argument is null, the result is null.
*/

/**
 * Created by Bryn on 5/25/2016 (v1)
 * Edited by Chris Schuler on 6/13/2016 (v2), 6/25/2016 (v3)
 */
public class AddEvaluator extends org.cqframework.cql.elm.execution.Add {

  private static final int YEAR_RANGE_MAX = 9999;

  public static Object add(Object left, Object right) {

    if (left == null || right == null) {
        return null;
    }

    if (left instanceof Integer && right instanceof Integer) {
      return (Integer)left + (Integer)right;
    }

    else if (left instanceof BigDecimal && right instanceof BigDecimal) {
      return ((BigDecimal)left).add((BigDecimal)right);
    }

    else if (left instanceof Quantity && right instanceof Quantity) {
      return new Quantity().withValue((((Quantity)left).getValue()).add(((Quantity)right).getValue())).withUnit(((Quantity)left).getUnit());
    }

    // +(DateTime, Quantity)
    else if (left instanceof DateTime && right instanceof Quantity) {
      DateTime dt = (DateTime)left;
      String unit = ((Quantity)right).getUnit();
      int value = ((Quantity)right).getValue().intValue();

      int idx = DateTime.getFieldIndex2(unit);

      if (idx != -1) {
        int startSize = dt.getPartial().size();
        // check that the Partial has the precision specified
        if (startSize < idx + 1) {
          // expand the Partial to the proper precision
          for (int i = startSize; i < idx + 1; ++i) {
            dt.setPartial(dt.getPartial().with(DateTime.getField(i), DateTime.getField(i).getField(null).getMinimumValue()));
          }
        }
        // do the addition
        dt.setPartial(dt.getPartial().property(DateTime.getField(idx)).addToCopy(value));
        // truncate to original precision
        for (int i = idx; i >= startSize; --i) {
          dt.setPartial(dt.getPartial().without(DateTime.getField(i)));
        }
      }

      else {
        throw new IllegalArgumentException(String.format("Invalid duration unit: %s", unit));
      }

      if (dt.getPartial().getValue(0) > YEAR_RANGE_MAX) {
        throw new ArithmeticException("The date time addition results in a year greater than the accepted range.");
      }

      return dt;
    }

    // +(Uncertainty, Uncertainty)
    else if (left instanceof Uncertainty && right instanceof Uncertainty) {
      Interval leftInterval = ((Uncertainty)left).getUncertaintyInterval();
      Interval rightInterval = ((Uncertainty)right).getUncertaintyInterval();
      return new Uncertainty().withUncertaintyInterval(new Interval(add(leftInterval.getStart(), rightInterval.getStart()), true, add(leftInterval.getEnd(), rightInterval.getEnd()), true));
    }

    // +(Time, Quantity)
    else if (left instanceof Time && right instanceof Quantity) {
      Time time = (Time)left;
      String unit = ((Quantity)right).getUnit();
      int value = ((Quantity)right).getValue().intValue();

      int idx = Time.getFieldIndex2(unit);

      if (idx != -1) {
        int startSize = time.getPartial().size();
        // check that the Partial has the precision specified
        if (startSize < idx + 1) {
          // expand the Partial to the proper precision
          for (int i = startSize; i < idx + 1; ++i) {
            time.setPartial(time.getPartial().with(Time.getField(i), Time.getField(i).getField(null).getMinimumValue()));
          }
        }
        // do the addition
        time.setPartial(time.getPartial().property(Time.getField(idx)).addToCopy(value));
        // truncate to original precision
        for (int i = idx; i >= startSize; --i) {
          time.setPartial(time.getPartial().without(Time.getField(i)));
        }
      }

      else {
        throw new IllegalArgumentException(String.format("Invalid duration unit: %s", unit));
      }
      return time;
    }

    throw new IllegalArgumentException(String.format("Cannot AddEvaluator arguments of type '%s' and '%s'.", left.getClass().getName(), right.getClass().getName()));
  }

    @Override
    public Object evaluate(Context context) {
      Object left = getOperand().get(0).evaluate(context);
      Object right = getOperand().get(1).evaluate(context);

      return add(left, right);
    }
}
