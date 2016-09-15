package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Time;

/*
same precision as(left DateTime, right DateTime) Boolean
same precision as(left Time, right Time) Boolean

The same-precision-as operator compares two date/time values to the specified precision for equality.
  Individual component values are compared starting from the year component down to the specified precision.
    If all values are specified and have the same value for each component, then the result is true.
      If a compared component is specified in both dates, but the values are not the same, then the result is false.
        Otherwise the result is null, as there is not enough information to make a determination.
For DateTime values, precision must be one of: year, month, day, hour, minute, second, or millisecond.
For Time values, precision must be one of: hour, minute, second, or millisecond.
For comparisons involving date/time or time values with imprecision, note that the result of the comparison may be null,
  depending on whether the values involved are specified to the level of precision used for the comparison.
If either or both arguments are null, the result is null.
*/

/**
* Created by Chris Schuler on 6/23/2016
*/
public class SameAsEvaluator extends org.cqframework.cql.elm.execution.SameAs {

  @Override
  public Object evaluate(Context context) {
    Object left = getOperand().get(0).evaluate(context);
    Object right = getOperand().get(1).evaluate(context);

    if (left == null || right == null) { return null; }

    if (left instanceof DateTime && right instanceof DateTime) {
      DateTime leftDT = (DateTime)left;
      DateTime rightDT = (DateTime)right;
      String precision = getPrecision().value();

      if (precision == null) {
        throw new IllegalArgumentException("Precision must be specified.");
      }

      int idx = DateTime.getFieldIndex(precision);

      if (idx != -1) {
        // check level of precision
        if (idx + 1 > leftDT.getPartial().size() || idx + 1 > rightDT.getPartial().size()) {
          return null;
        }

        for (int i = 0; i < idx + 1; ++i) {
          if (leftDT.getPartial().getValue(i) != rightDT.getPartial().getValue(i)) {
            return false;
          }
        }

        return true;
      }

      else {
        throw new IllegalArgumentException(String.format("Invalid duration precision: %s", precision));
      }
    }

    if (left instanceof Time && right instanceof Time) {
      Time leftT = (Time)left;
      Time rightT = (Time)right;
      String precision = getPrecision().value();

      if (precision == null) {
        throw new IllegalArgumentException("Precision must be specified.");
      }

      int idx = Time.getFieldIndex(precision);

      if (idx != -1) {
        // check level of precision
        if (idx + 1 > leftT.getPartial().size() || idx + 1 > rightT.getPartial().size()) {
          return null;
        }

        for (int i = 0; i < idx + 1; ++i) {
          if (leftT.getPartial().getValue(i) != rightT.getPartial().getValue(i)) {
            return false;
          }
        }

        return true;
      }

      else {
        throw new IllegalArgumentException(String.format("Invalid duration precision: %s", precision));
      }
    }

    throw new IllegalArgumentException(String.format("Cannot SameAs arguments of type '%s' and '%s'.", left.getClass().getName(), right.getClass().getName()));
  }
}
