package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Quantity;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Time;
import java.math.BigDecimal;

/*
ToString(argument Boolean) String
ToString(argument Integer) String
ToString(argument Decimal) String
ToString(argument Quantity) String
ToString(argument DateTime) String
ToString(argument Time) String

The ToString operator converts the value of its argument to a String value.
The operator uses the following string representations for each type:
Boolean	true|false
Integer	   (-)?#0
Decimal	   (-)?#0.0#
Quantity	 (-)?#0.0# '<unit>'
DateTime	 YYYY-MM-DDThh:mm:ss.fff(+|-)hh:mm
Time	     Thh:mm:ss.fff(+|-)hh:mm
If the argument is null, the result is null.
*/

/**
* Created by Chris Schuler on 6/14/2016
*/
public class ToStringEvaluator extends org.cqframework.cql.elm.execution.ToString {

  @Override
  public Object evaluate(Context context) {
    Object operand = getOperand().evaluate(context);

    if (operand == null) { return null; }

    if (operand instanceof Integer) {
      return Integer.toString((Integer)operand);
    }
    else if (operand instanceof BigDecimal) {
      return ((BigDecimal)operand).toString();
    }
    else if (operand instanceof Quantity) {
      return (((Quantity)operand).getValue()).toString() + ((Quantity)operand).getUnit();
    }
    else if (operand instanceof Boolean) {
      return Boolean.toString((Boolean)operand);
    }
    else if (operand instanceof DateTime) {
      return ((DateTime)operand).getPartial().toString();
    }
    else if (operand instanceof Time) {
      return ((Time)operand).getPartial().toString();
    }
    throw new IllegalArgumentException(String.format("Cannot ToString a value of type %s.", operand.getClass().getName()));
  }
}
