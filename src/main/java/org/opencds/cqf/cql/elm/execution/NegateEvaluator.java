package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Quantity;
import java.math.BigDecimal;

/*
-(argument Integer) Integer
-(argument Decimal) Decimal
-(argument Quantity) Quantity

The negate (-) operator returns the negative of its argument.
When negating quantities, the unit is unchanged.
If the argument is null, the result is null.
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class NegateEvaluator extends org.cqframework.cql.elm.execution.Negate {

  @Override
  public Object evaluate(Context context) {
    Object value = getOperand().evaluate(context);

    if (value instanceof Integer) {
        return -(int) value;
    }

    if (value instanceof BigDecimal) {
        return ((BigDecimal)value).negate();
    }

    if (value instanceof Quantity) {
        Quantity quantity = (Quantity) value;
        return new Quantity().withValue(quantity.getValue().negate()).withUnit(quantity.getUnit());
    }

    return value;
  }
}
