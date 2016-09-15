package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Value;

/*
*** NOTES FOR CLINICAL OPERATORS ***
~(left Code, right Code) Boolean

The ~ operator for Code values returns true if the code, system, and version elements are equivalent.
  The display element is ignored for the purposes of determining Code equivalence.
For Concept values, equivalence is defined as a non-empty intersection of the codes in each Concept.
  The display element is ignored for the purposes of determining Concept equivalence.
Note that this operator will always return true or false, even if either or both of its arguments are null,
  or contain null components.
Note carefully that this notion of equivalence is not the same as the notion of equivalence used in terminology:
  "these codes represent the same concept." CQL specifically avoids defining terminological equivalence.
    The notion of equivalence defined here is used to provide consistent and intuitive semantics when dealing with
      missing information in membership contexts.

*** NOTES FOR INTERVAL ***
~(left Interval<T>, right Interval<T>) Boolean

The ~ operator for intervals returns true if and only if the intervals are over the same point type,
  and the starting and ending points of the intervals as determined by the Start and End operators are equivalent.

*** NOTES FOR LIST ***
~(left List<T>, right List<T>) Boolean

The ~ operator for lists returns true if and only if the lists contain elements of the same type, have the same number of elements,
  and for each element in the lists, in order, the elements are equivalent.
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class EquivalentEvaluator extends org.cqframework.cql.elm.execution.Equivalent {

    @Override
    public Object evaluate(Context context) {
        Object left = getOperand().get(0).evaluate(context);
        Object right = getOperand().get(1).evaluate(context);

        return Value.equivalent(left, right);
    }
}
