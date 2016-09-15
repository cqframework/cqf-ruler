package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import java.math.BigDecimal;
import java.math.RoundingMode;

/*
Round(argument Decimal) Decimal
Round(argument Decimal, precision Integer) Decimal

The Round operator returns the nearest whole number to its argument. The semantics of round are defined as a traditional
  round, meaning that a decimal value of 0.5 or higher will round to 1.
When invoked with an Integer argument, the argument will be implicitly converted to Decimal.
If the argument is null, the result is null.
Precision determines the decimal place at which the rounding will occur.
If precision is not specified or null, 0 is assumed.
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class RoundEvaluator extends org.cqframework.cql.elm.execution.Round {

    @Override
    public Object evaluate(Context context) {
        Object value = getOperand().evaluate(context);
        Object precisionValue = getPrecision() == null ? null : getPrecision().evaluate(context);
        //BigDecimal precision = new BigDecimal((precisionValue == null ? 0 : (Integer)precisionValue));
        RoundingMode rm = RoundingMode.HALF_UP;

        if (value == null) { return null; }

        if (((BigDecimal)value).compareTo(new BigDecimal(0)) < 0) { rm = RoundingMode.HALF_DOWN; }

        if (value instanceof BigDecimal){
            if (precisionValue == null || ((Integer)precisionValue == 0)) {
                return ((BigDecimal)value).setScale(0, rm);
            }
            else {
                return ((BigDecimal)value).setScale((Integer)precisionValue, rm);
            }
        }

        throw new IllegalArgumentException(String.format("Cannot Round with argument of type '%s'.", value.getClass().getName()));
    }
}
