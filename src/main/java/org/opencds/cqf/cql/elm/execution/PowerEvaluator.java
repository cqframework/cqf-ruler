package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import java.math.BigDecimal;

/*
^(argument Integer, exponent Integer) Integer
^(argument Decimal, exponent Decimal) Decimal

The power (^) operator raises the first argument to the power given by the second argument.
When invoked with mixed Integer and Decimal arguments, the Integer argument will be implicitly converted to Decimal.
If either argument is null, the result is null.
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class PowerEvaluator extends org.cqframework.cql.elm.execution.Power {

    @Override
    public Object evaluate(Context context) {
        Object left = getOperand().get(0).evaluate(context);
        Object right = getOperand().get(1).evaluate(context);

        if (left == null || right == null) {
            return null;
        }

        if (left instanceof Integer) {
            if ((Integer)right < 0) {
              return new BigDecimal(1).divide(new BigDecimal((Integer)left).pow(Math.abs((Integer)right)));
            }
            return new BigDecimal((Integer)left).pow((Integer)right).intValue();
        }

        if (left instanceof BigDecimal) {
            return new BigDecimal(Math.pow((((BigDecimal)left).doubleValue()), ((BigDecimal)right).doubleValue()));
        }

        throw new IllegalArgumentException(String.format("Cannot %s arguments of type '%s' and '%s'.", this.getClass().getSimpleName(), left.getClass().getName(), right.getClass().getName()));
    }
}
