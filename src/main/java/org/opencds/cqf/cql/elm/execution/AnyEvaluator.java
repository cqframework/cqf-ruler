package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Quantity;
import org.opencds.cqf.cql.runtime.Value;
import java.math.BigDecimal;
import java.util.*;

/*
simple type Any

The Any type is the maximal supertype in the CQL type system, meaning that all types derive from Any, including list, interval, and structured types.
In addition, the type of a null result is Any.
Missing from elm.execution
*/
/**
* Created by Chris Schuler on 6/13/2016
*/
public class AnyEvaluator {
  // @Override
  // public Object evaluate(Context context) {
  //   return null;
  // }
}
// public class AnyEvaluator extends Any {
//
//   @Override
//   public Object evaluate(Context context) {
//     Object source = getOperand().evaluate(context);
//
//     if (source == null) { return Any; }
//
//     if (source instanceof Integer) {
//       return (Integer)source;
//     }
//     else if (source instanceof BigDecimal) {
//       return (BigDecimal)source;
//     }
//     else if (source instanceof Quantity) {
//       return ((Quantity)source).getValue();
//     }
//     else if (source instanceof Boolean) {
//       return (Boolean)source;
//     }
//     else if (source instanceof String) {
//       return (String)source;
//     }
//
//     // TODO: Finish Implementation for types:
//     // DateTime
//     // Time
//     // Code
//     // Concept
//     // Tuple
//     // Interval
//     // list
//     // Named
//
//     throw new IllegalArgumentException(String.format("Cannot Any arguments of type '%s'.", source.getClass().getName()));
//   }
// }
