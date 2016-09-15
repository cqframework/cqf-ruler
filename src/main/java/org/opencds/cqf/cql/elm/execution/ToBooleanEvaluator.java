package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;

/*
ToBoolean(argument String) Boolean

The ToBoolean operator converts the value of its argument to a Boolean value.
The operator accepts the following string representations:
true: true t yes y 1
false: false f no n 0
Note that the operator will ignore case when interpreting the string as a Boolean value.
If the input cannot be interpreted as a valid Boolean value, a run-time error is thrown.
If the argument is null, the result is null.
*/

/**
* Created by Chris Schuler on 6/14/2016
*/
public class ToBooleanEvaluator extends org.cqframework.cql.elm.execution.ToBoolean {

  @Override
  public Object evaluate(Context context) {
    Object value = getOperand().evaluate(context);

    if (value == null) { return null; }

    if (value instanceof String) {
      String compare = ((String)value).toLowerCase();
      if (compare.equals("true") || compare.equals("t") || compare.equals("yes") || compare.equals("y") || compare.equals("1")) {
        return true;
      }
      else if (compare.equals("false") || compare.equals("f") || compare.equals("no") || compare.equals("n") || compare.equals("0")) {
        return false;
      }
      throw new IllegalArgumentException(String.format("%s is not a valid String representation of a Boolean.", (String)value));
    }

    throw new IllegalArgumentException(String.format("Cannot cast a value of type %s as Boolean - use String values.", value.getClass().getName()));
  }
}
