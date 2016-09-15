package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.DateTime;

/*
date from(argument DateTime) DateTime

NOTE: this is within the purview of DateTimeComponentFrom
  Description available in that class
*/

/**
* Created by Chris Schuler on 6/22/2016
*/
public class DateFromEvaluator extends org.cqframework.cql.elm.execution.DateFrom {

  @Override
  public Object evaluate(Context context) {
    Object operand = getOperand().evaluate(context);

    if (operand instanceof DateTime) {
      return (DateTime)operand;
    }
    throw new IllegalArgumentException(String.format("Cannot DateFrom arguments of type '%s'.", operand.getClass().getName()));
  }
}
