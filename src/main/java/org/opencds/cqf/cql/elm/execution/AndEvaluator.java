package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;

/*
and (left Boolean, right Boolean) Boolean

The and operator returns true if both its arguments are true.
If either argument is false, the result is false. Otherwise, the result is null.

The following examples illustrate the behavior of the and operator:
define IsTrue = true and true
define IsFalse = true and false
define IsAlsoFalse = false and null
define IsNull = true and null
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class AndEvaluator extends org.cqframework.cql.elm.execution.And {
    @Override
    public Object evaluate(Context context) {
        Boolean left = (Boolean) getOperand().get(0).evaluate(context);
        Boolean right = (Boolean) getOperand().get(1).evaluate(context);

        if (left == null || right == null) {
            if ((left != null && left == false) || (right != null && right == false)) {
                return false;
            }

            return null;
        }

        return (left && right);
    }
}
