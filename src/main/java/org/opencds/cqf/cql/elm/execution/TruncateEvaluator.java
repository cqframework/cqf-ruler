package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;

import java.math.BigDecimal;

/*
Truncate(argument Decimal) Integer

The Truncate operator returns the integer component of its argument.
When invoked with an Integer argument, the argument will be implicitly converted to Decimal.
If the argument is null, the result is null.
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class TruncateEvaluator extends org.cqframework.cql.elm.execution.Truncate {

    @Override
    public Object evaluate(Context context) {
        Object value = getOperand().evaluate(context);

        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal) {
            Double val = ((BigDecimal)value).doubleValue();
            if (val < 0){
                return ((BigDecimal)value).setScale(0, BigDecimal.ROUND_CEILING).intValue();
            }
            else {
                return ((BigDecimal)value).setScale(0, BigDecimal.ROUND_FLOOR).intValue();
            }
        }
        throw new IllegalArgumentException(String.format("Cannot Truncate with argument of type '%s'.", value.getClass().getName()));
    }
}
