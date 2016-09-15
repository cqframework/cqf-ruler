package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Quantity;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.runtime.Uncertainty;
import java.math.BigDecimal;
import java.math.RoundingMode;

/*
*(left Integer, right Integer) Integer
*(left Decimal, right Decimal) Decimal
*(left Decimal, right Quantity) Quantity
*(left Quantity, right Decimal) Quantity
*(left Quantity, right Quantity) Quantity

The multiply (*) operator performs numeric multiplication of its arguments.
When invoked with mixed Integer and Decimal arguments, the Integer argument will be implicitly converted to Decimal.
TODO: For multiplication operations involving quantities, the resulting quantity will have the appropriate unit. For example:
12 'cm' * 3 'cm'
3 'cm' * 12 'cm2'
In this example, the first result will have a unit of 'cm2', and the second result will have a unit of 'cm3'.
If either argument is null, the result is null.
*/

/**
 * Created by Bryn on 5/25/2016.
 * Edited by Chris Schuler on 6/14/2016
 */
public class MultiplyEvaluator extends org.cqframework.cql.elm.execution.Multiply {

  public static Object multiply(Object left, Object right) {
    if (left == null || right == null) {
        return null;
    }

    // *(Integer, Integer)
    if (left instanceof Integer) {
        return (Integer)left * (Integer)right;
    }

    // *(Decimal, Decimal)
    else if (left instanceof BigDecimal && right instanceof BigDecimal) {
        return ((BigDecimal)left).multiply((BigDecimal)right).setScale(8, RoundingMode.FLOOR);
    }

    // *(Quantity, Quantity)
    else if (left instanceof Quantity && right instanceof Quantity) {
      // TODO: unit multiplication i.e. cm*cm = cm^2
      String unit = ((Quantity)left).getUnit();
      return new Quantity().withValue((((Quantity)left).getValue()).multiply(((Quantity)right).getValue()).setScale(8, RoundingMode.FLOOR)).withUnit(unit);
    }

    // *(Decimal, Quantity)
    else if (left instanceof BigDecimal && right instanceof Quantity) {
      return ((BigDecimal)left).multiply(((Quantity)right).getValue()).setScale(8, RoundingMode.FLOOR);
    }

    // *(Quantity, Decimal)
    else if (left instanceof Quantity && right instanceof BigDecimal) {
      return (((Quantity)left).getValue()).multiply((BigDecimal)right).setScale(8, RoundingMode.FLOOR);
    }

    // *(Uncertainty, Uncertainty)
    else if (left instanceof Uncertainty && right instanceof Uncertainty) {
      Interval leftInterval = ((Uncertainty)left).getUncertaintyInterval();
      Interval rightInterval = ((Uncertainty)right).getUncertaintyInterval();
      return new Uncertainty().withUncertaintyInterval(new Interval(multiply(leftInterval.getStart(), rightInterval.getStart()), true, multiply(leftInterval.getEnd(), rightInterval.getEnd()), true));
    }

    throw new IllegalArgumentException(String.format("Cannot Multiply arguments of type '%s' and '%s'.", left.getClass().getName(), right.getClass().getName()));
  }

    @Override
    public Object evaluate(Context context) {
        Object left = getOperand().get(0).evaluate(context);
        Object right = getOperand().get(1).evaluate(context);

        return multiply(left, right);
    }
}
