package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;

/*
Lower(argument String) String

The Lower operator returns the lower case of its argument.
If the argument is null, the result is null.
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class LowerEvaluator extends org.cqframework.cql.elm.execution.Lower {

    @Override
    public Object evaluate(Context context) {
        Object value = getOperand().evaluate(context);

        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            return ((String) value).toLowerCase();
        }
        throw new IllegalArgumentException(String.format("Cannot %s with argument of type '%s'.", this.getClass().getSimpleName(), value.getClass().getName()));
    }
}
