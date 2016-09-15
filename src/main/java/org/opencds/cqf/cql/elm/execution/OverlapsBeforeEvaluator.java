package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.runtime.Value;

/*
overlaps before(left Interval<T>, right Interval<T>) Boolean

The operator overlaps before returns true if the first interval overlaps the second and starts before it
If either argument is null, the result is null.
*/

/**
* Created by Chris Schuler on 6/8/2016
*/
public class OverlapsBeforeEvaluator extends org.cqframework.cql.elm.execution.OverlapsBefore {

  @Override
  public Object evaluate(Context context) {
    Interval left = (Interval)getOperand().get(0).evaluate(context);
    Interval right = (Interval)getOperand().get(1).evaluate(context);

    if (left != null && right != null) {
      Object leftStart = left.getStart();
      Object leftEnd = left.getEnd();
      Object rightStart = right.getStart();
      Object rightEnd = right.getEnd();

      return (Value.compareTo(leftStart, rightStart, "<") && OverlapsEvaluator.overlaps(left, right));
    }
    return null;
  }
}
