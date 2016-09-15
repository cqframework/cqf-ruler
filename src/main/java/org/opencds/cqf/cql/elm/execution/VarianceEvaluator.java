package org.opencds.cqf.cql.elm.execution;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Quantity;
import org.opencds.cqf.cql.runtime.Value;
import java.util.Iterator;
import java.math.BigDecimal;
import java.math.RoundingMode;

/*
Variance(argument List<Decimal>) Decimal
Variance(argument List<Quantity>) Quantity

The Variance operator returns the statistical variance of the elements in source.
If the source contains no non-null elements, null is returned.
If the source is null, the result is null.
Return types: BigDecimal & Quantity
*/

/**
* Created by Chris Schuler on 6/14/2016
*/
public class VarianceEvaluator extends org.cqframework.cql.elm.execution.Variance {

  public static Object variance(Object source) {
    if (source instanceof Iterable) {
      Iterable<Object> element = (Iterable<Object>)source;
      Iterator<Object> itr = element.iterator();

      if (!itr.hasNext()) { return null; } // empty list

      DescriptiveStatistics stats = new DescriptiveStatistics();
      Object value = itr.next();
      while (value == null) { value = itr.next(); }
      if (!itr.hasNext()) { return null; } // no non-null elements

      if (value instanceof BigDecimal) {
        stats.addValue(((BigDecimal)value).doubleValue());
        while (itr.hasNext()) {
          BigDecimal next = (BigDecimal)itr.next();
          if (next != null) { stats.addValue(next.doubleValue()); }
        }
        BigDecimal retVal = new BigDecimal(stats.getVariance());
        return retVal.precision() > 8 ? retVal.setScale(8, RoundingMode.FLOOR) : retVal;
      }

      else if (value instanceof Quantity) {
        stats.addValue((((Quantity)value).getValue()).doubleValue());
        while (itr.hasNext()) {
          BigDecimal next = ((Quantity)itr.next()).getValue();
          if (next != null) { stats.addValue(next.doubleValue()); }
        }
        Quantity retVal = new Quantity().withValue(new BigDecimal(stats.getVariance()))
                                        .withUnit(((Quantity)value).getUnit());
        return retVal.getValue().precision() > 8 ? retVal.withValue(retVal.getValue().setScale(8, RoundingMode.FLOOR)) : retVal;
      }

      throw new IllegalArgumentException(String.format("Cannot PopulationVariance arguments of type '%s'.", value.getClass().getName()));
    }
    throw new IllegalArgumentException(String.format("Invalid instance '%s' for Variance operation.", source.getClass().getName()));
  }

  @Override
  public Object evaluate(Context context) {
    Object source = getSource().evaluate(context);
    if (source == null) { return null; }

    return variance(source);
  }
}
