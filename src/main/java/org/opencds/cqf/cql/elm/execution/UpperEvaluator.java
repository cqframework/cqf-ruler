package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;

/*
Upper(argument String) String

The Upper operator returns the upper case of its argument.
If the argument is null, the result is null.
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class UpperEvaluator extends org.cqframework.cql.elm.execution.Upper {

    @Override
    public Object evaluate(Context context) {
        Object value = getOperand().evaluate(context);

        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            return ((String)value).toUpperCase();
        }
        throw new IllegalArgumentException(String.format("Cannot %s with argument of type '%s'.", this.getClass().getSimpleName(), value.getClass().getName()));
    }
}
