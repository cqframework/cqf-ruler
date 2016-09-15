package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;

/*
is true(argument Boolean) Boolean

The is true operator determines whether or not its argument evaluates to true.
If the argument evaluates to true, the result is true; otherwise, the result is false.
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class IsTrueEvaluator extends org.cqframework.cql.elm.execution.IsTrue {

    @Override
    public Object evaluate(Context context) {
        return Boolean.TRUE == (Boolean) getOperand().evaluate(context);
    }
}
