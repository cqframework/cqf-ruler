package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;

/*
xor (left Boolean, right Boolean) Boolean

The xor (exclusive or) operator returns true if one argument is true and the other is false.
If both arguments are true or both arguments are false, the result is false. Otherwise, the result is null.
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class XorEvaluator extends org.cqframework.cql.elm.execution.Xor {

    @Override
    public Object evaluate(Context context) {
        Boolean left = (Boolean)getOperand().get(0).evaluate(context);
        Boolean right = (Boolean)getOperand().get(1).evaluate(context);

        if (left == null || right == null) {
            return null;
        }

        return (left ^ right);
    }
}
