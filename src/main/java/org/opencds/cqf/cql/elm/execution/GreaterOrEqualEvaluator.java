package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Value;

/*
>=(left Integer, right Integer) Boolean
>=(left Decimal, right Decimal) Boolean
>=(left Quantity, right Quantity) Boolean
>=(left DateTime, right DateTime) Boolean
>=(left Time, right Time) Boolean
>=(left String, right String) Boolean

The greater or equal (>=) operator returns true if the first argument is greater than or equal to the second argument.
For comparisons involving quantities, the dimensions of each quantity must be the same, but not necessarily the unit.
  For example, units of 'cm' and 'm' are comparable, but units of 'cm2' and  'cm' are not.
For comparisons involving date/time or time values with imprecision, note that the result of the comparison may be null,
  depending on whether the values involved are specified to the level of precision used for the comparison.
If either argument is null, the result is null.
*/

/**
 * Created by Bryn on 5/25/2016 (v1), edited by Chris Schuler on 6/28/2016 (v2)
 */
public class GreaterOrEqualEvaluator extends org.cqframework.cql.elm.execution.GreaterOrEqual {

  public static Object greaterOrEqual(Object left, Object right) {

    if (left == null || right == null) {
        return null;
    }

    return Value.compareTo(left, right, ">=");
  }

    @Override
    public Object evaluate(Context context) {
        Object left = getOperand().get(0).evaluate(context);
        Object right = getOperand().get(1).evaluate(context);

        return greaterOrEqual(left, right);
    }
}
