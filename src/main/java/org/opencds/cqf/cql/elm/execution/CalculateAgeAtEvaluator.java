package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.DateTime;

/*
CalculateAgeInYearsAt(birthDate DateTime, asOf DateTime) Integer
CalculateAgeInMonthsAt(birthDate DateTime, asOf DateTime) Integer
CalculateAgeInDaysAt(birthDate DateTime, asOf DateTime) Integer
CalculateAgeInHoursAt(birthDate DateTime, asOf DateTime) Integer
CalculateAgeInMinutesAt(birthDate DateTime, asOf DateTime) Integer
CalculateAgeInSecondsAt(birthDate DateTime, asOf DateTime) Integer

The CalculateAgeAt operators calculate the age of a person born on the given birthdate as of the given date in the precision named in the operator.
If the birthDate is null or the asOf argument is null, the result is null.
The CalculateAgeAt operators are defined in terms of a DateTime duration calculation.
  This means that if the given birthDate or asOf are not specified to the level of precision corresponding to the operator being invoked,
    the result will be an uncertainty over the range of possible values, potentially causing some comparisons to return null.
*/

/**
* Created by Chris Schuler on 7/14/2016
*/
public class CalculateAgeAtEvaluator extends org.cqframework.cql.elm.execution.CalculateAgeAt {

  public static Object calculateAgeAt(Object birthDate, Object asOf, String precision) {
    if (birthDate == null || asOf == null) { return null; }
    return DurationBetweenEvaluator.durationBetween((DateTime)birthDate, (DateTime)asOf, precision);
  }

  @Override
  public Object evaluate(Context context) {
    Object birthDate = getOperand().get(0).evaluate(context);
    Object asOf = getOperand().get(1).evaluate(context);
    String precision = getPrecision().value();
    return calculateAgeAt(birthDate, asOf, precision);
  }
}
