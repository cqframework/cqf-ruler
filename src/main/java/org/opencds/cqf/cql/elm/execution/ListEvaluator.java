package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;

import java.util.ArrayList;

/**
 * Created by Bryn on 5/25/2016.
 */
public class ListEvaluator extends org.cqframework.cql.elm.execution.List {

    @Override
    public Object evaluate(Context context) {
        ArrayList<Object> result = new ArrayList<Object>();
        for (org.cqframework.cql.elm.execution.Expression element : this.getElement()) {
            result.add(element.evaluate(context));
        }
        return result;
    }
}
