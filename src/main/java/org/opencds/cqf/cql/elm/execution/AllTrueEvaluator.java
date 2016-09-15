package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;

import java.util.Iterator;

/*
AllTrue(argument List<Boolean>) Boolean

The AllTrue operator returns true if all the non-null elements in the source are true.
If the source contains no non-null elements, true is returned.
If the source is null, the result is null.
*/

/**
 * Created by Bryn on 5/25/2016.
 * Edited by Chris Schuler on 6/13/2016
 */
public class AllTrueEvaluator extends org.cqframework.cql.elm.execution.AllTrue {
    @Override
    public Object evaluate(Context context) {

        Object src = getSource().evaluate(context);
        if (src == null) { return null; }

        if(src instanceof Iterable) {
          Iterable<Object> element = (Iterable<Object>)src;
          Iterator<Object> elemsItr = element.iterator();
          if (!elemsItr.hasNext()) { return null; } // empty list
          while (elemsItr.hasNext()) {
              Object exp = elemsItr.next();
              if (exp == null) { continue; } // skip null
              Boolean boolVal = (Boolean) exp;

              if (boolVal == null || Boolean.FALSE == boolVal) return false;
          }
        }else{
            return null;
        }

        return true;
    }
}
