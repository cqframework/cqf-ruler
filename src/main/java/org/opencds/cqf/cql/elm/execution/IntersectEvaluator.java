package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.runtime.Value;

import java.util.ArrayList;
import java.util.List;

/*
*** NOTES FOR INTERVAL ***
intersect(left Interval<T>, right Interval<T>) Interval<T>

The intersect operator for intervals returns the intersection of two intervals.
  More precisely, the operator returns the interval that defines the overlapping portion of both arguments.
If the arguments do not overlap, this operator returns null.
If either argument is null, the result is null.

*** NOTES FOR LIST ***
intersect(left List<T>, right List<T>) List<T>

The intersect operator for lists returns the intersection of two lists.
  More precisely, the operator returns a list containing only the elements that appear in both lists.
This operator uses the notion of equivalence to determine whether or not two elements are the same.
If either argument is null, the result is null.
*/

/**
 * Created by Bryn on 5/25/2016.
 * Edited by Chris Schuler on 6/8/2016 - added Interval Logic
 */
public class IntersectEvaluator extends org.cqframework.cql.elm.execution.Intersect {

  @Override
  public Object evaluate(Context context) {
    Object left = getOperand().get(0).evaluate(context);
    Object right = getOperand().get(1).evaluate(context);

    if (left == null || right == null) { return null; }

    if (left instanceof Interval) {
      Interval leftInterval = (Interval)left;
      Interval rightInterval = (Interval)right;

      if (leftInterval == null || rightInterval == null) { return null; }

      if (!OverlapsEvaluator.overlaps(leftInterval, rightInterval)) { return null; }

      Object leftStart = leftInterval.getStart();
      Object leftEnd = leftInterval.getEnd();
      Object rightStart = rightInterval.getStart();
      Object rightEnd = rightInterval.getEnd();

      if (leftStart == null || leftEnd == null || rightStart == null || rightEnd == null) { return null; }

      Object max = Value.compareTo(leftStart, rightStart, ">") ? leftStart : rightStart;
      Object min = Value.compareTo(leftEnd, rightEnd, "<") ? leftEnd : rightEnd;

      return new Interval(max, true, min, true);
    }

    else if (left instanceof Iterable) {
      Iterable<Object> leftArr = (Iterable<Object>)left;
      Iterable<Object> rightArr = (Iterable<Object>)right;

      if (leftArr == null || rightArr == null) {
          return null;
      }

      List<Object> result = new ArrayList<Object>();
      for (Object leftItem : leftArr) {
          if (InEvaluator.in(leftItem, rightArr)) {
              result.add(leftItem);
          }
      }
      return result;
    }
    throw new IllegalArgumentException(String.format("Cannot Intersect arguments of type '%s' and '%s'.", left.getClass().getName(), right.getClass().getName()));
  }
}
