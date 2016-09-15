package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;

/*
as<T>(argument Any) T
cast as<T>(argument Any) T

The as operator allows the result of an expression to be cast as a given target type.
  This allows expressions to be written that are statically typed against the expected run-time type of the argument.
If the argument is not of the specified type at run-time the result is null.
The cast prefix indicates that if the argument is not of the specified type at run-time then an exception is thrown.
Example:
The following examples illustrate the use of the as operator.
define AllProcedures: [Procedure]
define ImagingProcedures:
  AllProcedures P
    where P is ImagingProcedure
    return P as ImagingProcedure
define RuntimeError:
  ImagingProcedures P
    return cast P as Observation
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class AsEvaluator extends org.cqframework.cql.elm.execution.As {

    private Class resolveType(Context context) {
        if (this.getAsTypeSpecifier() != null) {
            return context.resolveType(this.getAsTypeSpecifier());
        }

        return context.resolveType(this.getAsType());
    }

    @Override
    public Object evaluate(Context context) {
        Object operand = getOperand().evaluate(context);
        if (operand != null) {
            Class clazz = resolveType(context);
            if (clazz.isAssignableFrom(operand.getClass())) {
                return operand;
            }
            else if (this.isStrict()) {
                throw new IllegalArgumentException(String.format("Cannot cast a value of type %s as %s.", operand.getClass().getName(), clazz.getName()));
            }
            else {
                return null;
            }
        }

        return null;
    }
}
