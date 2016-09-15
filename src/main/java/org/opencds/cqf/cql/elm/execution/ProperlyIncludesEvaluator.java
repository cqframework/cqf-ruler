package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.runtime.Value;
import java.util.ArrayList;

/*
*** NOTES FOR INTERVAL ***
properly includes(left Interval<T>, right Interval<T>) Boolean

The properly includes operator for intervals returns true if the first interval completely includes the second and the
  first interval is strictly larger than the second.
  More precisely, if the starting point of the first interval is less than or equal to the starting point of the second interval,
    and the ending point of the first interval is greater than or equal to the ending point of the second interval,
      and they are not the same interval.
This operator uses the semantics described in the Start and End operators to determine interval boundaries.
If either argument is null, the result is null.

*** NOTES FOR LIST ***
properly includes(left List<T>, right List<T>) Boolean

The properly includes operator for lists returns true if the first list contains every element of the second list,
  and the first list is strictly larger than the second list.
This operator uses the notion of equivalence to determine whether or not two elements are the same.
If either argument is null, the result is null.
Note that the order of elements does not matter for the purposes of determining inclusion.
*/

/**
* Created by Chris Schuler on 6/8/2016
*/
public class ProperlyIncludesEvaluator extends org.cqframework.cql.elm.execution.ProperIncludes {

  @Override
  public Object evaluate(Context context) {
    Object operand1 = getOperand().get(0).evaluate(context);
    Object operand2 = getOperand().get(1).evaluate(context);

    if (operand1 == null || operand2 == null) { return null; }

    if (operand1 instanceof Interval) {
      Interval left = (Interval)operand1;
      Interval right = (Interval)operand2;

      if (left != null && right != null) {
        Object leftStart = left.getStart();
        Object leftEnd = left.getEnd();
        Object rightStart = right.getStart();
        Object rightEnd = right.getEnd();

        return (Value.compareTo(Interval.getSize(leftStart, leftEnd), Interval.getSize(rightStart, rightEnd), ">")
                && Value.compareTo(leftStart, rightStart, "<=") && Value.compareTo(leftEnd, rightEnd, ">="));
      }
      return null;
    }

    else if (operand1 instanceof Iterable) {
      ArrayList<Object> left = (ArrayList<Object>)operand1;
      ArrayList<Object> right = (ArrayList<Object>)operand2;

      return (Boolean)IncludesEvaluator.includes(left, right) && left.size() > right.size();
    }

    throw new IllegalArgumentException(String.format("Cannot ProperlyIncludes arguments of type: %s", operand1.getClass().getName()));
  }
}
