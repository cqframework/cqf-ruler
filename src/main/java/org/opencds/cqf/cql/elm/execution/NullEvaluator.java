package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;

/**
 * Created by Bryn on 5/25/2016.
 */
public class NullEvaluator extends org.cqframework.cql.elm.execution.Null {

    @Override
    public Object evaluate(Context context) {
        return null;
    }
}
