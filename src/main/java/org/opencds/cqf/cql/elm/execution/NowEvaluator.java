package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.DateTime;

/*
Now() DateTime

The Now operator returns the date and time of the start timestamp associated with the evaluation request.
Now is defined in this way for two reasons:
1.	The operation will always return the same value within any given evaluation, ensuring that the result of
      an expression containing Now will always return the same result.
2.	The operation will return the timestamp associated with the evaluation request, allowing the evaluation to
      be performed with the same timezone information as the data delivered with the evaluation request.
*/

/**
* Created by Chris Schuler on 6/21/2016 (v1)
*/
public class NowEvaluator extends org.cqframework.cql.elm.execution.Now {

  @Override
  public Object evaluate(Context context) {
    return DateTime.getNow();
  }
}
