package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import java.util.Iterator;

/*
Count(argument List<T>) Integer

* The Count operator returns the number of non-null elements in the source.
* If the list contains no non-null elements, the result is 0.
* If the list is null, the result is null.
* Always returns Integer
*/

/**
* Created by Chris Schuler on 6/13/2016
*/
public class CountEvaluator extends org.cqframework.cql.elm.execution.Count {

  @Override
  public Object evaluate(Context context) {

    Object source = getSource().evaluate(context);
    if (source == null) { return null; }

    Integer size = new Integer(0);

    if (source instanceof Iterable) {
      Iterable<Object> element = (Iterable<Object>)source;
      Iterator<Object> itr = element.iterator();

      if (!itr.hasNext()) { return size; } // empty list
      while (itr.hasNext()) {
        Object value = itr.next();
        if (value == null) { continue; } // skip null
        ++size;
      }
    }
    else { return null; }
    return size;
  }
}
