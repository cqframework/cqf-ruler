// package org.opencds.cqf.cql.elm.execution;
//
// import org.opencds.cqf.cql.execution.Context;
//
// /*
// implies (argument Boolean) Boolean
//
// The implies operator returns the logical implication of its arguments. This means that if the left operand evaluates to true, this operator returns the boolean evaluation of the right operand. If the left operand evaluates to false, this operator returns true. Otherwise, this operator returns true if the right operand evaluates to true, and null otherwise.
// The following table defines the truth table for this operator:
// 	    | TRUE	FALSE	NULL
// ------------------------
// TRUE  |	TRUE	FALSE	NULL
// FALSE	| TRUE	TRUE	TRUE
// NULL	| TRUE	NULL	NULL
// */
//
// /**
//  * Created by Bryn on 5/25/2016.
//  */
// public class ImpliesEvaluator extends org.cqframework.cql.elm.execution.Implies {
//
//     @Override
//     public Object evaluate(Context context) {
//         Boolean left = (Boolean)getOperand().get(0).evaluate(context);
//         Boolean right = (Boolean)getOperand().get(1).evaluate(context);
//
//         if (left == null) {
//             return right == null || !right ? null : true;
//         }
//         else if (left) {
//             return right;
//         }
//         else {
//             return true;
//         }
//     }
// }
