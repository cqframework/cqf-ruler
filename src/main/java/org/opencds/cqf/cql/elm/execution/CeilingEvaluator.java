package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Quantity;
import java.math.BigDecimal;

/*
Ceiling(argument Decimal) Integer

The Ceiling operator returns the first integer greater than or equal to the argument.
When invoked with an Integer argument, the argument will be implicitly converted to Decimal.
If the argument is null, the result is null.
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class CeilingEvaluator extends org.cqframework.cql.elm.execution.Ceiling {
  @Override
  public Object evaluate(Context context) {
    Object value = getOperand().evaluate(context);

    if (value == null) {
        return null;
    }

    if (value instanceof BigDecimal) {
      return BigDecimal.valueOf(Math.ceil(((BigDecimal)value).doubleValue())).intValue();
    }

    else if (value instanceof Quantity) {
      return BigDecimal.valueOf(Math.ceil(((Quantity)value).getValue().doubleValue())).intValue();
    }

    throw new IllegalArgumentException(String.format("Cannot perform Ceiling operation with argument of type '%s'.", value.getClass().getName()));
  }
}
