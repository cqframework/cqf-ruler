package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Quantity;
import org.opencds.cqf.cql.runtime.Value;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Time;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/*
Median(argument List<Decimal>) Decimal
Median(argument List<Quantity>) Quantity

The Median operator returns the median of the elements in source.
If the source contains no non-null elements, null is returned.
If the source is null, the result is null.
*/

/**
* Created by Chris Schuler on 6/13/2016
*/
public class MedianEvaluator extends org.cqframework.cql.elm.execution.Median {

  public static ArrayList<Object> sortList(ArrayList<Object> values) {
    Collections.sort(values, new Comparator<Object>() {
      @Override
      public int compare(Object o1, Object o2) {
        if (o1 instanceof Integer) {
          if (Value.compareTo(o1, o2, "<")) { return -1; }
          else if (Value.compareTo(o1, o2, ">")) { return 1; }
          else { return 0; }
        }
        else if (o1 instanceof BigDecimal) {
          return ((BigDecimal)o1).compareTo((BigDecimal)o2);
        }
        else if (o1 instanceof Quantity) {
          return (((Quantity)o1).getValue().compareTo(((Quantity)o2).getValue()));
        }
        else if (o1 instanceof DateTime || o1 instanceof Time) {
          if ((Boolean)LessEvaluator.less(o1, o2)) { return -1; }
          else if ((Boolean)GreaterEvaluator.greater(o1, o2)) { return 1; }
          else { return 0; }
        }

        throw new IllegalArgumentException(String.format("Cannot Median arguments of type '%s' and '%s'.", o1.getClass().getName(), o1.getClass().getName()));
      }
    });
    return values;
  }

  public static Object median(Object source) {
    Iterable<Object> element = (Iterable<Object>)source;
    Iterator<Object> itr = element.iterator();

    if (!itr.hasNext()) { return null; } // empty list
    Object median = new Object();
    ArrayList<Object> values = new ArrayList<>();
    while (itr.hasNext()) {
      Object value = itr.next();
      if (value != null) { values.add(value); }
    }

    if (values.isEmpty()) { return null; } // all null

    values = sortList(values);

    if (values.size() % 2 != 0) {
      return values.get(values.size() / 2);
    }
    else {
      if (values.get(0) instanceof Integer) { // size of list is even
        return TruncatedDivideEvaluator.div(AddEvaluator.add(values.get(values.size()/2), values.get((values.size()/2)-1)), new Integer(2));
      }
      else if (values.get(0) instanceof BigDecimal || values.get(0) instanceof Quantity) {
        return DivideEvaluator.divide(AddEvaluator.add(values.get(values.size()/2), values.get((values.size()/2)-1)), new BigDecimal("2.0"));
      }
    }
    return null;
  }

  @Override
  public Object evaluate(Context context) {

    Object source = getSource().evaluate(context);
    if (source == null) { return null; }

    if (source instanceof Iterable) {
      return median(source);
    }
    else { return null; }
  }
}
