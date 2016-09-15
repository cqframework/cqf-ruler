package org.opencds.cqf.cql.elm.execution;

import org.apache.commons.lang3.NotImplementedException;
import org.opencds.cqf.cql.execution.Context;

/*
convert to<T>(argument Any) T

The convert operator converts a value to a specific type.
The result of the operator is the value of the argument converted to the target type, if possible.
  Note that use of this operator may result in a run-time exception being thrown if there is no valid
    conversion from the actual value to the target type.
The following table lists the conversions supported in CQL:
From\To	  Boolean	  Integer	  Decimal	  Quantity	String	Datetime	Time	Code	Concept
Boolean	    N/A	      -	        -	          -	   Explicit	   -	      -	    -	     -
Integer	     -	     N/A	   Implicit	      -	   Explicit	   -	      -	    -	     -
Decimal	     -	      -	       N/A	        -	   Explicit	   - 	      -	    -	     -
Quantity	   -	      -	        -	         N/A	 Explicit	   -	      -	    -	     -
String	  Explicit Explicit	 Explicit	  Explicit	 N/A	  Explicit Explicit	-	     -
Datetime	   -	      -	        -	          -	   Explicit	  N/A	      -	    -	     -
Time	       -	      -	        -	          -	   Explicit	   -	     N/A	  -	     -
Code	       -	      -	        -	          -	      -	       -	      -	   N/A	Implicit
Concept	     -	      -	        -	          -	      -	       -	      -	    -	    N/A

For conversions between date/time and string values, ISO-8601 standard format is used:
yyyy-MM-ddThh:mm:ss.fff(Z | +/- hh:mm)
For example, the following are valid string representations for date/time values:
'2014-01-01T14:30:00.0Z'      // January 1st, 2014, 2:30PM UTC
'2014-01-01T14:30:00.0-07:00' // January 1st, 2014, 2:30PM Mountain Standard (GMT-7:00)
'T14:30:00.0Z'                // 2:30PM UTC
'T14:30:00.0-07:00'           // 2:30PM Mountain Standard (GMT-7:00)
For specific semantics for each conversion, refer to the explicit conversion operator documentation.

*/

/**
 * Created by Bryn on 5/25/2016.
 * Edited by Chris Schuler in 6/15/2016
 */
public class ConvertEvaluator extends org.cqframework.cql.elm.execution.Convert {

  private Class resolveType(Context context) {
    if (this.getToTypeSpecifier() != null) {
      return context.resolveType(this.getToTypeSpecifier());
    }
    return context.resolveType(this.getToType());
  }

  @Override
  public Object evaluate(Context context) {

    Object operand = getOperand().evaluate(context);
    if (operand == null) { return null; }

    Class type = resolveType(context);

    try {
      if (type.isInstance(operand)) {
        Class cls = operand.getClass();
        return cls.newInstance();
      }
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    throw new IllegalArgumentException(String.format("Cannot Convert a value of type %s as %s.", operand.getClass().getName(), type.getName()));
  }
}
