package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;

/*
structured type Quantity
{
  value Decimal
  unit String
}

The Quantity type represents quantities with a specified unit within CQL.
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class QuantityEvaluator extends org.cqframework.cql.elm.execution.Quantity {

    @Override
    public Object evaluate(Context context) {
        return new org.opencds.cqf.cql.runtime.Quantity().withValue(this.getValue()).withUnit(this.getUnit());
    }
}
