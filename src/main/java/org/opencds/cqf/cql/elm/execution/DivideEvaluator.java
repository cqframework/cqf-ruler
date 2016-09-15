package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Quantity;
import org.opencds.cqf.cql.runtime.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;

/*
/(left Decimal, right Decimal) Decimal
/(left Quantity, right Decimal) Quantity
/(left Quantity, right Quantity) Quantity

The divide (/) operator performs numeric division of its arguments.
Note that this operator is Decimal division; for Integer division, use the truncated divide (div) operator.
When invoked with Integer arguments, the arguments will be implicitly converted to Decimal.
TODO: For division operations involving quantities, the resulting quantity will have the appropriate unit. For example:
12 'cm2' / 3 'cm'
In this example, the result will have a unit of 'cm'.
If either argument is null, the result is null.
*/

/**
 * Created by Bryn on 5/25/2016.
 * Edited by Chris Schuler on 6/13/2016
 */
public class DivideEvaluator extends org.cqframework.cql.elm.execution.Divide {

  public static Object divide(Object left, Object right) {

    if (left == null || right == null) {
        return null;
    }

    if (left instanceof BigDecimal && right instanceof BigDecimal) {
      if (Value.compareTo(right, new BigDecimal("0.0"), "==")) { return null; }
      return ((BigDecimal)left).divide((BigDecimal)right, 8, RoundingMode.FLOOR);
    }

    else if (left instanceof Quantity && right instanceof Quantity) {
      if (Value.compareTo(((Quantity)right).getValue(), new BigDecimal(0), "==")) { return null; }
      return new Quantity().withValue((((Quantity)left).getValue()).divide(((Quantity)right).getValue(), 8, RoundingMode.FLOOR)).withUnit(((Quantity)left).getUnit());
    }

    else if (left instanceof Quantity && right instanceof BigDecimal) {
      if (Value.compareTo(right, new BigDecimal("0.0"), "==")) { return null; }
      return new Quantity().withValue((((Quantity)left).getValue()).divide((BigDecimal)right, 8, RoundingMode.FLOOR)).withUnit(((Quantity)left).getUnit());
    }

    throw new IllegalArgumentException(String.format("Cannot Divide arguments of type '%s' and '%s'.", left.getClass().getName(), right.getClass().getName()));

  }

    @Override
    public Object evaluate(Context context) {
        Object left = getOperand().get(0).evaluate(context);
        Object right = getOperand().get(1).evaluate(context);

        return divide(left, right);
    }
}
