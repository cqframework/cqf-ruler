package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Quantity;
import java.math.BigDecimal;

/*
ToQuantity(argument String) Quantity

The ToQuantity operator converts the value of its argument to a Quantity value.
The operator accepts strings using the following format:
(+|-)?#0(.0#)?('<unit>')?
Note that the decimal value of the quantity returned by this operator must be a valid value in the range representable for Decimal values in CQL.
If the input string is not formatted correctly, or cannot be interpreted as a valid Quantity value, a run-time error is thrown.
If the argument is null, the result is null.
ToQuantity('5.5 cm2'), ToQuantity('5.5cm2'), or ToQuantity('5.5') - optional +/- at beginning
*/

/**
* Created by Chris Schuler on 6/14/2016
*/
public class ToQuantityEvaluator extends org.cqframework.cql.elm.execution.ToQuantity {

  @Override
  public Object evaluate(Context context) {
    Object operand = getOperand().evaluate(context);

    if (operand == null) { return null; }

    if (operand instanceof String) {
      String str = (String)operand;
      String number = "";
      String unit = "";
      for (char c : str.toCharArray()) {
        if ((Character.isDigit(c) || c == '.' || c == '+' || c == '-') && unit.isEmpty()) { number += c; }
        else if (Character.isLetter(c) || c == '/') { unit += c; }
        else if (c == ' ') { continue; }
        else {
          throw new IllegalArgumentException(String.format("%c is not allowed in ToQuantity format", c));
        }
      }
      return new Quantity().withValue(new BigDecimal(number)).withUnit(unit.toLowerCase());
    }
    throw new IllegalArgumentException(String.format("Cannot cast a value of type %s as Quantity - use String values.", operand.getClass().getName()));
  }
}
