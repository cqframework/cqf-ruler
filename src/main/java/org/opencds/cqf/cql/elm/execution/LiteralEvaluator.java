package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;

import java.math.BigDecimal;

/**
 * Created by Bryn on 5/25/2016.
 */
public class LiteralEvaluator extends org.cqframework.cql.elm.execution.Literal {

    @Override
    public Object evaluate(Context context) {
        switch (this.getValueType().getLocalPart()) {
            case "Boolean": return Boolean.parseBoolean(this.getValue());
            case "Integer": return Integer.parseInt(this.getValue());
            case "Decimal": return new BigDecimal(this.getValue());
            case "String": return this.getValue();
            default: throw new IllegalArgumentException(String.format("Cannot construct literal value for type '%s'.", this.getValueType().toString()));
        }
    }
}
