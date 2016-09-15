package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Quantity;
import java.util.Iterator;
import java.math.BigDecimal;

/*
Sum(argument List<Integer>) Integer
Sum(argument List<Decimal>) Decimal
Sum(argument List<Quantity>) Quantity

The Sum operator returns the sum of non-null elements in the source.
If the source contains no non-null elements, null is returned.
If the list is null, the result is null.
Return types: Integer, BigDecimal & Quantity
*/

/**
* Created by Chris Schuler on 6/14/2016
*/
public class SumEvaluator extends org.cqframework.cql.elm.execution.Sum {

  public static Object sum(Object source) {
    if (source instanceof Iterable) {
      Iterable<Object> element = (Iterable<Object>)source;
      Iterator<Object> itr = element.iterator();

      if (!itr.hasNext()) { return null; } // empty list
      Object sum = itr.next();
      while (sum == null) { sum = itr.next(); }

      if (sum instanceof Integer) {
        while (itr.hasNext()) {
          Integer next = (Integer)itr.next();
          if (next != null) { sum = (Integer)sum + next; }
        }
        return (Integer)sum;
      }
      else if (sum instanceof BigDecimal) {
        while (itr.hasNext()) {
          BigDecimal next = (BigDecimal)itr.next();
          if (next != null) { sum = ((BigDecimal)sum).add(next); }
        }
        return (BigDecimal)sum;
      }
      else if (sum instanceof Quantity) {
        while (itr.hasNext()) {
          BigDecimal next = ((Quantity)itr.next()).getValue();
          if (next != null) { sum = (((Quantity)sum).getValue()).add(next); }
        }
        return new Quantity().withValue(((Quantity)sum).getValue()).withUnit(((Quantity)sum).getUnit());
      }
      throw new IllegalArgumentException(String.format("Cannot Sum arguments of type '%s'.", sum.getClass().getName()));
    }
    throw new IllegalArgumentException(String.format("Invalid instance '%s' for Sum operation.", source.getClass().getName()));
  }

  @Override
  public Object evaluate(Context context) {
    Object source = getSource().evaluate(context);
    if (source == null) { return null; }

    return sum(source);
  }
}
