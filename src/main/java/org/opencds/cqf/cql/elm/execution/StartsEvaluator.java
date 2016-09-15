package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.runtime.Value;

/*
starts(left Interval<T>, right Interval<T>) Boolean

The starts operator returns true if the first interval starts the second.
  More precisely, if the starting point of the first is equal to the starting point of the second interval and the
    ending point of the first interval is less than or equal to the ending point of the second interval.
This operator uses the semantics described in the Start and End operators to determine interval boundaries.
If either argument is null, the result is null.
*/

/**
* Created by Chris Schuler on 6/8/2016
*/
public class StartsEvaluator extends org.cqframework.cql.elm.execution.Starts {

  @Override
  public Object evaluate(Context context) {
    Interval left = (Interval)getOperand().get(0).evaluate(context);
    Interval right = (Interval)getOperand().get(1).evaluate(context);

    if (left != null && right != null) {
      Object leftStart = left.getStart();
      Object leftEnd = left.getEnd();
      Object rightStart = right.getStart();
      Object rightEnd = right.getEnd();

      if (leftStart == null || leftEnd == null || rightStart == null || rightEnd == null) { return null; }

      return (Value.compareTo(leftStart, rightStart, "==") && Value.compareTo(leftEnd, rightEnd, "<="));
    }
    return null;
  }
}
