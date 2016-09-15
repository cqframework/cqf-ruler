package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;

import java.math.BigDecimal;

/*
Ln(argument Decimal) Decimal

The Ln operator computes the natural logarithm of its argument.
When invoked with an Integer argument, the argument will be implicitly converted to Decimal.
If the argument is null, the result is null.
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class LnEvaluator extends org.cqframework.cql.elm.execution.Ln {

    @Override
    public Object evaluate(Context context) {
        Object value = getOperand().evaluate(context);

        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal){
          BigDecimal retVal = new BigDecimal(0);
          try {
            retVal = new BigDecimal(Math.log(((BigDecimal)value).doubleValue()));
          } catch (NumberFormatException nfe){
            if (((BigDecimal)value).compareTo(new BigDecimal(0)) < 0) {
              return null;
            }
            else if (((BigDecimal)value).compareTo(new BigDecimal(0)) == 0) {
              throw new ArithmeticException("Results in negative infinity");
            }
            else { throw new NumberFormatException(); }
          }
            return retVal;
        }

        throw new IllegalArgumentException(String.format("Cannot Natural Log with argument of type '%s'.", value.getClass().getName()));
    }
}
