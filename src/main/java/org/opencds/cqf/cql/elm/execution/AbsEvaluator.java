package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Quantity;
import java.math.BigDecimal;

/*
Abs(argument Integer) Integer
Abs(argument Decimal) Decimal
Abs(argument Quantity) Quantity

The Abs operator returns the absolute value of its argument.
When taking the absolute value of a quantity, the unit is unchanged.
If the argument is null, the result is null.
*/

/**
 * Created by Bryn on 5/24/2016.
 * Edited by Chris Schuler on 6/14/2016
 */
public class AbsEvaluator extends org.cqframework.cql.elm.execution.Abs {
    @Override
    public Object evaluate(Context context) {
        Object value = getOperand().evaluate(context);

        if (value == null) {
            return null;
        }

        if (value instanceof Integer) {
            return Math.abs((Integer)value);
        }

        else if (value instanceof BigDecimal) {
            return ((BigDecimal)value).abs();
        }

        else if (value instanceof Quantity) {
          return (((Quantity)value).getValue()).abs();
        }

        throw new IllegalArgumentException(String.format("Cannot %s with argument of type '%s'.",this.getClass().getSimpleName(), value.getClass().getName()));
    }
}
