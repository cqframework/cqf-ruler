package org.opencds.cqf.cql.elm.execution;

import org.apache.commons.lang3.NotImplementedException;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Time;
import java.math.BigDecimal;

/*
maximum<T>() T

The maximum operator returns the maximum representable value for the given type.
The maximum operator is defined for the Integer, Decimal, DateTime, and Time types.
For Integer, maximum returns the maximum signed 32-bit integer, 231 - 1.
For Decimal, maximum returns the maximum representable decimal value, (1037 – 1) / 108 (9999999999999999999999999999.99999999).
For DateTime, maximum returns the maximum representable date/time value, DateTime(9999, 12, 31, 23, 59, 59, 999).
For Time, maximum returns the maximum representable time value, Time(23, 59, 59, 999).
For any other type, attempting to invoke maximum results in an error.
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class MaxValueEvaluator extends org.cqframework.cql.elm.execution.MaxValue {

    @Override
    public Object evaluate(Context context) {
        switch (valueType.getLocalPart()) {
            case "Integer": return org.opencds.cqf.cql.runtime.Interval.maxValue(Integer.class);
            case "Decimal": return org.opencds.cqf.cql.runtime.Interval.maxValue(BigDecimal.class);
            case "Quantity": return org.opencds.cqf.cql.runtime.Interval.maxValue(org.opencds.cqf.cql.runtime.Quantity.class);
            case "DateTime": return org.opencds.cqf.cql.runtime.Interval.maxValue(DateTime.class);
            case "Time": return org.opencds.cqf.cql.runtime.Interval.maxValue(Time.class);
            default: throw new NotImplementedException(String.format("maxValue not implemented for type %s", valueType.getLocalPart()));
        }
    }
}
