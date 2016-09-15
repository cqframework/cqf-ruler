package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.runtime.Value;

import java.util.ArrayList;

/*
*** NOTES FOR INTERVAL ***
union(left Interval<T>, right Interval<T>) Interval<T>

The union operator for intervals returns the union of the intervals.
  More precisely, the operator returns the interval that starts at the earliest starting point in either argument,
    and ends at the latest starting point in either argument.
If the arguments do not overlap or meet, this operator returns null.
If either argument is null, the result is null.

*** NOTES FOR LIST ***
union(left List<T>, right List<T>) List<T>

The union operator for lists returns a list with all elements from both arguments.
Note that duplicates are not eliminated during this process, if an element appears once in both sources,
  that element will be present twice in the resulting list.
If either argument is null, the result is null.
Note that the union operator can also be invoked with the symbolic operator (|).
*/

/**
 * Created by Bryn on 5/25/2016.
 * Edited by Chris Schuler on 6/8/2016 - Interval Logic
 */
public class UnionEvaluator extends org.cqframework.cql.elm.execution.Union {

  public static Interval union(Object left, Object right) {
    if (left == null || right == null) { return null; }

    Object leftStart = ((Interval)left).getStart();
    Object leftEnd = ((Interval)left).getEnd();
    Object rightStart = ((Interval)right).getStart();
    Object rightEnd = ((Interval)right).getEnd();

    if (leftStart == null || leftEnd == null || rightStart == null || rightEnd == null) { return null; }

    if (!OverlapsEvaluator.overlaps((Interval)left, (Interval)right) && !MeetsEvaluator.meets((Interval)left, (Interval)right)) {
      return null;
    }

    Object min = Value.compareTo(leftStart, rightStart, "<") ? leftStart : rightStart;
    Object max = Value.compareTo(leftEnd, rightEnd, ">") ? leftEnd : rightEnd;

    return new Interval(min, true, max, true);
  }

  @Override
  public Object evaluate(Context context) {
    Object left = getOperand().get(0).evaluate(context);
    Object right = getOperand().get(1).evaluate(context);

    if (left == null || right == null) {
        return null;
    }

    if (left instanceof Interval) {
      return union(left, right);
    }

    else if (left instanceof Iterable) {
      // List Logic
      ArrayList result = new ArrayList();
      for (Object leftElement : (Iterable)left) {
          result.add(leftElement);
      }

      for (Object rightElement : (Iterable)right) {
          result.add(rightElement);
      }
      return result;
    }
    throw new IllegalArgumentException(String.format("Cannot Union arguments of type: %s and %s", left.getClass().getName(), right.getClass().getName()));
  }
}
