package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.runtime.Value;

/*
meets(left Interval<T>, right Interval<T>) Boolean

The meets operator returns true if the first interval ends immediately before the second interval starts,
  or if the first interval starts immediately after the second interval ends.
In other words, if the ending point of the first interval is equal to the predecessor of the starting point of the second,
  or if the starting point of the first interval is equal to the successor of the ending point of the second.
If either argument is null, the result is null.
*/

/**
* Created by Chris Schuler on 6/8/2016
*/
public class MeetsEvaluator extends org.cqframework.cql.elm.execution.Meets {

  public static Boolean meets(Interval left, Interval right) {
    if (left != null && right != null) {
      Object leftStart = left.getStart();
      Object leftEnd = left.getEnd();
      Object rightStart = right.getStart();
      Object rightEnd = right.getEnd();

      if (leftStart == null || leftEnd == null || rightStart == null || rightEnd == null) { return null; }

      return (Value.compareTo(rightStart, leftEnd, ">")) ? Value.compareTo(rightStart, Interval.successor(leftEnd), "==") : Value.compareTo(leftStart, Interval.successor(rightEnd), "==");
    }
    return null;
  }

  @Override
  public Object evaluate(Context context) {
    Interval left = (Interval)getOperand().get(0).evaluate(context);
    Interval right = (Interval)getOperand().get(1).evaluate(context);

    return meets(left, right);
  }
}
