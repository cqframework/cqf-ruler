package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;

/**
 * Created by Bryn on 5/25/2016.
 */
public class ParameterRefEvaluator extends org.cqframework.cql.elm.execution.ParameterRef {

    @Override
    public Object evaluate(Context context) {
        return context.resolveParameterRef(this.getLibraryName(), this.getName());
    }
}
