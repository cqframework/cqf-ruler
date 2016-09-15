package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Quantity;
import org.opencds.cqf.cql.runtime.Value;
import java.util.Iterator;
import java.util.ArrayList;
import java.math.BigDecimal;

/*
Mode(argument List<T>) T

The Mode operator returns the statistical mode of the elements in source.
If the source contains no non-null elements, null is returned.
If the source is null, the result is null.
*/

/**
* Created by Chris Schuler on 6/13/2016
*/
public class ModeEvaluator extends org.cqframework.cql.elm.execution.Mode {

  public static Object mode(Object source) {

    if (source instanceof Iterable) {
      Iterable<Object> element = (Iterable<Object>)source;
      Iterator<Object> itr = element.iterator();

      if (!itr.hasNext()) { return null; } // empty list
      Object mode = new Object();
      ArrayList<Object> values = new ArrayList<>();
      while (itr.hasNext()) {
        Object value = itr.next();
        if (value != null) { values.add(value); }
      }

      if (values.isEmpty()) { return null; } // all null
      values = MedianEvaluator.sortList(values);

      int max = 0;
      for (int i = 0; i < values.size(); ++i) {
        int count = (values.lastIndexOf(values.get(i)) - i) + 1;
        if (count > max) {
          mode = values.get(i);
          max = count;
        }
      }
      return mode;
    }
    throw new IllegalArgumentException(String.format("Cannot Mode arguments of type '%s'.", source.getClass().getName()));
  }

  @Override
  public Object evaluate(Context context) {
    Object source = getSource().evaluate(context);
    if (source == null) { return null; }

    return mode(source);
  }
}
