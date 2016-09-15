package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Interval;

/*
end of(argument Interval<T>) T

The End operator returns the ending point of an interval.
If the high boundary of the interval is open, this operator returns the predecessor of the high value of the interval.
  Note that if the high value of the interval is null, the result is null.
If the high boundary of the interval is closed and the high value of the interval is not null,
  this operator returns the high value of the interval.
    Otherwise, the result is the maximum value of the point type of the interval.
If the argument is null, the result is null.
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class EndEvaluator extends org.cqframework.cql.elm.execution.End {

    @Override
    public Object evaluate(Context context) {
        Interval argument = (Interval)this.getOperand().evaluate(context);
        if (argument != null) {
            return argument.getEnd();
        }

        return null;
    }
}
