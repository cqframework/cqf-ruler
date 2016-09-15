package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.runtime.Value;

/*
contains(argument List<T>, element T) Boolean

The contains operator for intervals returns true if the given point is greater than or equal to the starting point
  of the interval, and less than or equal to the ending point of the interval.
For open interval boundaries, exclusive comparison operators are used.
For closed interval boundaries, if the interval boundary is null, the result of the boundary comparison is considered true.
If either argument is null, the result is null.
*/

/**
 * Created by Bryn on 5/25/2016 (v1)
 * Edited by Chris Schuler on 6/8/2016 (v2)
 */
public class ContainsEvaluator extends org.cqframework.cql.elm.execution.Contains {

  @Override
  public Object evaluate(Context context) {
    Object test = getOperand().get(0).evaluate(context);

    if (test instanceof Interval) {
      Interval left = (Interval)test;
      Object right = getOperand().get(1).evaluate(context);

      if (left != null && right != null) {
        Object leftStart = left.getStart();
        Object leftEnd = left.getEnd();

        return (Value.compareTo(right, leftStart, ">=") && Value.compareTo(right, leftEnd, "<="));
      }
      return null;
    }

    else if (test instanceof Iterable) {
      Iterable<Object> list = (Iterable<Object>)test;
      Object testElement = getOperand().get(1).evaluate(context);

      return InEvaluator.in(testElement, list);
    }

    throw new IllegalArgumentException(String.format("Cannot Contains arguments of type '%s'.", test.getClass().getName()));
  }
}
