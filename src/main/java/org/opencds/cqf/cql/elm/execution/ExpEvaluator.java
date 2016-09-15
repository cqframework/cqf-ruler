package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;

import java.math.BigDecimal;

/*
Exp(argument Decimal) Decimal

The Exp operator raises e to the power of its argument.
When invoked with an Integer argument, the argument will be implicitly converted to Decimal.
If the argument is null, the result is null.
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class ExpEvaluator extends org.cqframework.cql.elm.execution.Exp {

    @Override
    public Object evaluate(Context context) {
        Object value = getOperand().evaluate(context);

        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal){
          BigDecimal retVal = new BigDecimal(0);
          try {
            retVal = new BigDecimal(Math.exp(((BigDecimal)value).doubleValue()));
          } catch (NumberFormatException nfe) {
            if (((BigDecimal)value).compareTo(new BigDecimal(0)) > 0) {
              throw new ArithmeticException("Results in positive infinity");
            }
            else if (((BigDecimal)value).compareTo(new BigDecimal(0)) < 0) {
              throw new ArithmeticException("Results in negative infinity");
            }
            else { throw new NumberFormatException(); }
          }
          return retVal;
        }

        throw new IllegalArgumentException(String.format("Cannot %s with argument of type '%s'.",this.getClass().getSimpleName(), value.getClass().getName()));
    }
}
