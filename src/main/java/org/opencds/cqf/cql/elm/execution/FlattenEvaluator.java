package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;

import java.util.ArrayList;

/*
flatten(argument List<List<T>>) List<T>

The flatten operator flattens a list of lists into a single list.
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class FlattenEvaluator extends org.cqframework.cql.elm.execution.Flatten {

    @Override
    public Object evaluate(Context context) {
        Object value = getOperand().evaluate(context);
        if (value == null) {
            return null;
        }

        ArrayList resultList = new ArrayList();
        for (Object element : (Iterable)value) {
            for (Object subElement : (Iterable)element) {
                resultList.add(subElement);
            }
        }

        return resultList;
    }
}
