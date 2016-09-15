package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.runtime.Value;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Time;
import org.opencds.cqf.cql.runtime.Uncertainty;

import java.util.ArrayList;

/*
*** NOTES FOR INTERVAL ***
before(left Interval<T>, right Interval<T>) Boolean
before(left T, right Interval<T>) Boolean
before(left interval<T>, right T) Boolean

The before operator for intervals returns true if the first interval ends before the second one starts.
  In other words, if the ending point of the first interval is less than the starting point of the second interval.
For the point-interval overload, the operator returns true if the given point is less than the start of the interval.
For the interval-point overload, the operator returns true if the given interval ends before the given point.
This operator uses the semantics described in the Start and End operators to determine interval boundaries.
If either argument is null, the result is null.


*** NOTES FOR DATETIME ***
before precision of(left DateTime, right DateTime) Boolean
before precision of(left Time, right Time) Boolean

The before-precision-of operator compares two date/time values to the specified precision to determine whether the
  first argument is the before the second argument. Precision must be one of: year, month, day, hour, minute, second, or millisecond.
For comparisons involving date/time or time values with imprecision, note that the result of the comparison may be null,
  depending on whether the values involved are specified to the level of precision used for the comparison.
If either or both arguments are null, the result is null.
*/

/**
* Created by Chris Schuler on 6/7/2016 (v1), 6/26/2016 (v2)
*/
public class BeforeEvaluator extends org.cqframework.cql.elm.execution.Before {

  @Override
  public Object evaluate(Context context) {
    Object testLeft = getOperand().get(0).evaluate(context);
    Object testRight = getOperand().get(1).evaluate(context);

    if (testLeft == null || testRight == null) { return null; }

    if (testLeft instanceof Interval && testRight instanceof Interval) {
      return Value.compareTo(((Interval)testLeft).getStart(), ((Interval)testRight).getEnd(), "<");
    }

    else if (testLeft instanceof Interval && !(testRight instanceof Interval)) {
      return Value.compareTo(((Interval)testLeft).getEnd(), testRight, "<");
    }

    else if (!(testLeft instanceof Interval) && testRight instanceof Interval) {
      return Value.compareTo(testLeft, ((Interval)testRight).getStart(), "<");
    }

    // (DateTime, DateTime)
    else if (testLeft instanceof DateTime && testRight instanceof DateTime) {
      DateTime leftDT = (DateTime)testLeft;
      DateTime rightDT = (DateTime)testRight;
      String precision = getPrecision() == null ? null : getPrecision().value();

      if (precision == null) {
        throw new IllegalArgumentException("Precision must be specified.");
      }

      int idx = DateTime.getFieldIndex(precision);

      if (idx != -1) {
        // check level of precision
        if (idx + 1 > leftDT.getPartial().size() || idx + 1 > rightDT.getPartial().size()) {

          if (Uncertainty.isUncertain(leftDT, precision)) {
            return LessEvaluator.less(Uncertainty.getHighLowList(leftDT, precision).get(0), rightDT);
          }

          else if (Uncertainty.isUncertain(rightDT, precision)) {
            return LessEvaluator.less(leftDT, Uncertainty.getHighLowList(rightDT, precision).get(1));
          }

          return null;
        }

        return leftDT.getPartial().getValue(idx) < rightDT.getPartial().getValue(idx);
      }

      else {
        throw new IllegalArgumentException(String.format("Invalid duration precision: %s", precision));
      }
    }

    else if (testLeft instanceof Time && testRight instanceof Time) {
      Time leftT = (Time)testLeft;
      Time rightT = (Time)testRight;
      String precision = getPrecision() == null ? null : getPrecision().value();

      if (precision == null) {
        throw new IllegalArgumentException("Precision must be specified.");
      }

      int idx = Time.getFieldIndex(precision);

      if (idx != -1) {
        // check level of precision
        if (idx + 1 > leftT.getPartial().size() || idx + 1 > rightT.getPartial().size()) {

          if (Uncertainty.isUncertain(leftT, precision)) {
            return LessEvaluator.less(Uncertainty.getHighLowList(leftT, precision).get(0), rightT);
          }

          else if (Uncertainty.isUncertain(rightT, precision)) {
            return LessEvaluator.less(leftT, Uncertainty.getHighLowList(rightT, precision).get(1));
          }

          return null;
        }

        return leftT.getPartial().getValue(idx) < rightT.getPartial().getValue(idx);
      }

      else {
        throw new IllegalArgumentException(String.format("Invalid duration precision: %s", precision));
      }
    }

    throw new IllegalArgumentException(String.format("Cannot Before arguments of type '%s' and '%s'.", testLeft.getClass().getName(), testRight.getClass().getName()));
  }
}
