package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Value;
import org.opencds.cqf.cql.runtime.Quantity;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Time;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.Iterator;

/*
collapse(argument List<Interval<T>>) List<Interval<T>>

The collapse operator returns the unique set of intervals that completely covers the ranges present in the given list of intervals.
If the list of intervals is empty, the result is empty.
If the list of intervals contains a single interval, the result is a list with that interval.
If the list of intervals contains nulls, they will be excluded from the resulting list.
If the argument is null, the result is null.
*/

/**
* Created by Chris Schuler on 6/8/2016
*/
public class CollapseEvaluator extends org.cqframework.cql.elm.execution.Collapse {

  public static Object collapse(ArrayList<Interval> intervals) {
    Collections.sort(intervals, new Comparator<Interval>() {
      @Override
      public int compare(Interval o1, Interval o2) {
        if (o1.getStart() instanceof Integer || o1.getStart() instanceof DateTime || o1.getStart() instanceof Time) {
          if (Value.compareTo(o1.getStart(), o2.getStart(), "<")) { return -1; }
          else if (Value.compareTo(o1.getStart(), o2.getStart(), "==")) { return 0; }
          else if (Value.compareTo(o1.getStart(), o2.getStart(), ">")) { return 1; }
        }
        else if (o1.getStart() instanceof BigDecimal) {
          return ((BigDecimal)o1.getStart()).compareTo((BigDecimal)o2.getStart());
        }
        else if (o1.getStart() instanceof Quantity) {
          return (((Quantity)o1.getStart()).getValue().compareTo(((Quantity)o2.getStart()).getValue()));
        }
        throw new IllegalArgumentException(String.format("Cannot Collapse arguments of type '%s' and '%s'.", o1.getClass().getName(), o1.getClass().getName()));
      }
    });

    for (int i = 0; i < intervals.size(); ++i) {
      if ((i+1) < intervals.size()) {
        if (OverlapsEvaluator.overlaps((Interval)intervals.get(i), (Interval)intervals.get(i+1))) {
          intervals.set(i, new Interval(((Interval)intervals.get(i)).getStart(), true, ((Interval)intervals.get(i+1)).getEnd(), true));
          intervals.remove(i+1);
          i -= 1;
        }
      }
    }
    return intervals;
  }

  @Override
  public Object evaluate(Context context) {
    Iterable<Object> list = (Iterable<Object>)getOperand().evaluate(context);
    if (list == null) { return null; }
    ArrayList intervals = new ArrayList();
    for (Object interval : list) {
      if (interval != null) { intervals.add(interval); }
    }
    if (intervals.size() == 1) { return intervals; }
    else if (intervals.size() == 0) { return null; }

    return collapse(intervals);
  }
}
