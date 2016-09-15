package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;

/*
is null(argument Any) Boolean

The is null operator determines whether or not its argument evaluates to null.
If the argument evaluates to null, the result is true; otherwise, the result is false.
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class IsNullEvaluator extends org.cqframework.cql.elm.execution.IsNull {

    @Override
    public Object evaluate(Context context) {
        return getOperand().evaluate(context) == null;
    }
}
