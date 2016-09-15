package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import java.util.List;

/*
*** LIST NOTES ***
Length(argument List<T>) Integer

The Length operator returns the number of elements in a list.
If the argument is null, the result is null.

*** STRING NOTES ***
Length(argument String) Integer

The Length operator returns the number of characters in a string.
If the argument is null, the result is null.
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class LengthEvaluator extends org.cqframework.cql.elm.execution.Length {

    @Override
    public Object evaluate(Context context) {
        Object value = getOperand().evaluate(context);

        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            return ((String) value).length();
        }

        if (value instanceof Iterable) {
            if (value instanceof List) {
                return ((List) value).size();
            } else {
                int size = 0;
                for(Object curr : (Iterable) value)
                {
                    size++;
                }
                return size;
            }
        }
        throw new IllegalArgumentException(String.format("Cannot %s of type '%s'.", this.getClass().getSimpleName(), value.getClass().getName()));
    }
}
