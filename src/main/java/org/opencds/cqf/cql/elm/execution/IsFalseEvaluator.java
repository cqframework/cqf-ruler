package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;

/*
is false(argument Boolean) Boolean

The is false operator determines whether or not its argument evaluates to false.
If the argument evaluates to false, the result is true; otherwise, the result is false.
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class IsFalseEvaluator extends org.cqframework.cql.elm.execution.IsFalse {

    @Override
    public Object evaluate(Context context) {
        return Boolean.FALSE == (Boolean) getOperand().evaluate(context);
    }
}
