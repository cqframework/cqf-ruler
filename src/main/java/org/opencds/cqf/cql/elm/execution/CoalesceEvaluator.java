package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import java.util.List;
import java.util.Iterator;

/*
Coalesce<T>(argument1 T, argument2 T) T
Coalesce<T>(argument1 T, argument2 T, argument3 T) T
Coalesce<T>(argument1 T, argument2 T, argument3 T, argument4 T) T
Coalesce<T>(argument1 T, argument2 T, argument3 T, argument4 T, argument5 T) T
Coalesce<T>(arguments List<T>) T

The Coalesce operator returns the first non-null result in a list of arguments.
If all arguments evaluate to null, the result is null.
The static type of the first argument determines the type of the result, and all subsequent arguments must be of that same type.
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class CoalesceEvaluator extends org.cqframework.cql.elm.execution.Coalesce {
    @Override
    public Object evaluate(Context context) {
        List<org.cqframework.cql.elm.execution.Expression> operands = getOperand();

        Iterator<org.cqframework.cql.elm.execution.Expression> expressions = operands.iterator();
        while (expressions.hasNext()) {
            org.cqframework.cql.elm.execution.Expression expression = expressions.next();
            Object tmpVal = expression.evaluate(context);
            if (tmpVal != null) {
                if (tmpVal instanceof Iterable && operands.size() == 1) {
                    Iterator<Object> elemsItr = ((Iterable) tmpVal).iterator();
                    while (elemsItr.hasNext()) {
                        Object obj = elemsItr.next();
                        if (obj != null) {
                            return obj;
                        }
                    }
                    return null;
                }
                return tmpVal;
            }
        }
        return null;
    }
}
