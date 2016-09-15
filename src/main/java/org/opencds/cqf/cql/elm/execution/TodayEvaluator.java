package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.DateTime;

/*
Today() DateTime

The Today operator returns the date (with no time component) of the start timestamp associated with the evaluation request.
See the Now operator for more information on the rationale for defining the Today operator in this way.
*/

/**
* Created by Chris Schuler on 6/21/2016
*/
public class TodayEvaluator extends org.cqframework.cql.elm.execution.Today {

  @Override
  public Object evaluate(Context context) {
    return DateTime.getToday();
  }
}
