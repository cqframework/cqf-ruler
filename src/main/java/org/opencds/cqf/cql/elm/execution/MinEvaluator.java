package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import java.util.Iterator;

/*
Min(argument List<Integer>) Integer
Min(argument List<Decimal>) Decimal
Min(argument List<Quantity>) Quantity
Min(argument List<DateTime>) DateTime
Min(argument List<Time>) Time
Min(argument List<String>) String

The Min operator returns the minimum element in the source.
If the source contains no non-null elements, null is returned.
If the source is null, the result is null.
*/

/**
* Created by Chris Schuler on 6/13/2016
*/
public class MinEvaluator extends org.cqframework.cql.elm.execution.Min {

  public static Object min(Object source) {
    if (source instanceof Iterable) {
      Iterable<Object> element = (Iterable<Object>)source;
      Iterator<Object> itr = element.iterator();

      if (!itr.hasNext()) { return null; } // empty list
      Object min = itr.next();
      while (min == null && itr.hasNext()) { min = itr.next(); }
      while (itr.hasNext()) {
        Object value = itr.next();

        if (value == null) { continue; } // skip null

        if ((Boolean)LessEvaluator.less(value, min)) { min = value; }

      }
      return min;
    }
    throw new IllegalArgumentException(String.format("Cannot Min arguments of type '%s'.", source.getClass().getName()));
  }

  @Override
  public Object evaluate(Context context) {

    Object source = getSource().evaluate(context);
    if (source == null) { return null; }

    return min(source);
  }
}
