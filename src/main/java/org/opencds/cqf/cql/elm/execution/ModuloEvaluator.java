package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import java.math.BigDecimal;
import java.math.RoundingMode;

/*
mod(left Integer, right Integer) Integer
mod(left Decimal, right Decimal) Decimal

The mod operator computes the remainder of the division of its arguments.
When invoked with mixed Integer and Decimal arguments, the Integer argument will be implicitly converted to Decimal.
If either argument is null, the result is null.
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class ModuloEvaluator extends org.cqframework.cql.elm.execution.Modulo {

    @Override
    public Object evaluate(Context context) {
        Object left = getOperand().get(0).evaluate(context);
        Object right = getOperand().get(1).evaluate(context);

        if (left == null || right == null) {
            return null;
        }

        if (left instanceof Integer) {
            if ((Integer)right == 0) { return null; }
            return (Integer)left % (Integer)right;
        }

        if (left instanceof BigDecimal) {
            if ((BigDecimal)right == new BigDecimal("0.0")) { return null; }
            return ((BigDecimal)left).remainder((BigDecimal)right).setScale(8, RoundingMode.FLOOR);
        }

        throw new IllegalArgumentException(String.format("Cannot %s arguments of type '%s' and '%s'.", this.getClass().getSimpleName(), left.getClass().getName(), right.getClass().getName()));
    }
}
