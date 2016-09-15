package org.opencds.cqf.cql.runtime;

import java.math.BigDecimal;
import org.opencds.cqf.cql.runtime.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.HashMap;

import org.joda.time.DateTimeFieldType;
/**
 * Created by Bryn on 5/2/2016.
 * Edited by Chris Schuler
 */
public class Value {

    public static Boolean compare(double left, double right, String op) {
      if (op.equals("==")) { return left == right; }
      else if (op.equals("!=")) { return left != right; }
      else if (op.equals("<")) { return left < right; }
      else if (op.equals(">")) { return left > right; }
      else if (op.equals("<=")) {return left <= right; }
      else if (op.equals(">=")) { return left >= right; }
      else { return null; }
    }

    public static Boolean compareTo(Object left, Object right, String op) {
      if (left == null || right == null) { return null; }

      if (left instanceof Integer) {
        BigDecimal leftOp = new BigDecimal((Integer)left);
        BigDecimal rightOp = new BigDecimal((Integer)right);
        if (leftOp == null || rightOp == null) { return null; }
        return compare(leftOp.doubleValue(), rightOp.doubleValue(), op);
      }

      else if (left instanceof BigDecimal) {
        return compare(((BigDecimal)left).doubleValue(), ((BigDecimal)right).doubleValue(), op);
      }

      else if (left instanceof Quantity) {
        BigDecimal leftOp = ((Quantity)left).getValue();
        BigDecimal rightOp = ((Quantity)right).getValue();
        if (leftOp == null || rightOp == null) { return null; }
        return compare(leftOp.doubleValue(), rightOp.doubleValue(), op);
      }

      else if (left instanceof String) {
        String leftStr = (String)left;
        String rightStr = (String)right;
        if (op.equals(">")) { return leftStr.compareTo(rightStr) > 0; }
        else if (op.equals(">=")) { return leftStr.compareTo(rightStr) >= 0; }
        else if (op.equals("<")) { return leftStr.compareTo(rightStr) < 0; }
        else if (op.equals("<=")) { return leftStr.compareTo(rightStr) <= 0; }
        else if (op.equals("==")) { return leftStr.compareTo(rightStr) == 0; }
      }

      else if (left instanceof Uncertainty || right instanceof Uncertainty) {
        ArrayList<Interval> intervals = Uncertainty.getLeftRightIntervals(left, right);
        Interval leftU = intervals.get(0);
        Interval rightU = intervals.get(1);
        String op1 = "";
        String op2 = "";

        if (op.equals(">")) { op1 = ">"; op2 = "<="; }
        else if (op.equals(">=")) { op1 = ">="; op2 = "<"; }
        else if (op.equals("<")) { op1 = "<"; op2 = ">="; }
        else if (op.equals("<=")) { op1 = "<="; op2 = ">"; }

        if (compareTo(leftU.getStart(), rightU.getEnd(), op1)) { return true; }
        if (compareTo(leftU.getEnd(), rightU.getStart(), op2)) { return false; }
        return null;
      }

      else if (left instanceof DateTime) {
        DateTime leftDT = (DateTime)left;
        DateTime rightDT = (DateTime)right;

        int size = 0;

        // Uncertainty detection
        if (leftDT.getPartial().size() != rightDT.getPartial().size()) {
          size = leftDT.getPartial().size() > rightDT.getPartial().size() ? rightDT.getPartial().size() : leftDT.getPartial().size();
        }
        else { size = leftDT.getPartial().size(); }

        if (op.equals(">") || op.equals(">=")) {
          for (int i = 0; i < size; ++i) {
            if (leftDT.getPartial().getValue(i) > rightDT.getPartial().getValue(i)) {
              return true;
            }
            else if (leftDT.getPartial().getValue(i) < rightDT.getPartial().getValue(i)) {
              return false;
            }
          }
        }

        else if (op.equals("<") || op.equals("<=")) {
          for (int i = 0; i < size; ++i) {
            if (leftDT.getPartial().getValue(i) > rightDT.getPartial().getValue(i)) {
              return false;
            }
            else if (leftDT.getPartial().getValue(i) < rightDT.getPartial().getValue(i)) {
              return true;
            }
          }
        }

        else if (op.equals("==")) { return equivalent(leftDT, rightDT); }
        // Uncertainty wrinkle
        if (leftDT.getPartial().size() != rightDT.getPartial().size()) { return null; }
        return compare(new BigDecimal(leftDT.getPartial().getValue(size-1)).doubleValue(),
                       new BigDecimal(rightDT.getPartial().getValue(size-1)).doubleValue(), op);
      }

      else if (left instanceof Time) {
        Time leftT = (Time)left;
        Time rightT = (Time)right;

        int size = 0;

        // Uncertainty detection
        if (leftT.getPartial().size() != rightT.getPartial().size()) {
          size = leftT.getPartial().size() > rightT.getPartial().size() ? rightT.getPartial().size() : leftT.getPartial().size();
        }
        else { size = leftT.getPartial().size(); }

        if (op.equals(">") || op.equals(">=")) {
          for (int i = 0; i < size; ++i) {
            if (leftT.getPartial().getValue(i) > rightT.getPartial().getValue(i)) {
              return true;
            }
            else if (leftT.getPartial().getValue(i) < rightT.getPartial().getValue(i)) {
              return false;
            }
          }
        }

        else if (op.equals("<") || op.equals("<=")) {
          for (int i = 0; i < size; ++i) {
            if (leftT.getPartial().getValue(i) > rightT.getPartial().getValue(i)) {
              return false;
            }
            else if (leftT.getPartial().getValue(i) < rightT.getPartial().getValue(i)) {
              return true;
            }
          }
        }

        else if (op.equals("==")) { return equivalent(leftT, rightT); }
        // Uncertainty wrinkle
        if (leftT.getPartial().size() != rightT.getPartial().size()) { return null; }
        return compare(new BigDecimal(leftT.getPartial().getValue(size-1)).doubleValue(),
                       new BigDecimal(rightT.getPartial().getValue(size-1)).doubleValue(), op);
      }

    throw new IllegalArgumentException(String.format("Cannot Compare arguments of type '%s' and '%s'.", left.getClass().getName(), right.getClass().getName()));
  }

  public static Boolean equals(Object left, Object right) {
    if ((left == null) || (right == null)) {
        return null;
    }

    // Uncertainty - may have mismatched types - this is allowed
    // must never return true -- only false or null
    if (left instanceof Uncertainty || right instanceof Uncertainty) {
      ArrayList<Interval> intervals = Uncertainty.getLeftRightIntervals(left, right);
      Interval leftU = intervals.get(0);
      Interval rightU = intervals.get(1);

      if (Value.compareTo(leftU.getEnd(), rightU.getStart(), "<")) { return false; }
      if (Value.compareTo(leftU.getStart(), rightU.getEnd(), ">")) { return false; }
      return null;
    }

    // mismatched types not allowed
    if (!left.getClass().equals(right.getClass())) { return null; }

    if (left instanceof Interval && right instanceof Interval) {
      Object leftStart = ((Interval)left).getStart();
      Object leftEnd = ((Interval)left).getEnd();
      Object rightStart = ((Interval)right).getStart();
      Object rightEnd = ((Interval)right).getEnd();

      if (leftStart == null || leftEnd == null || rightStart == null || rightEnd == null) { return null; }

      return (compareTo(leftStart, rightStart, "==") && compareTo(leftEnd, rightEnd, "=="));
    }

    // list equal
    else if (left instanceof Iterable) {
        Iterator<Object> leftIterator = ((Iterable<Object>)left).iterator();
        Iterator<Object> rightIterator = ((Iterable<Object>)right).iterator();

        while (leftIterator.hasNext()) {
            Object leftObject = leftIterator.next();
            if (rightIterator.hasNext()) {
                Object rightObject = rightIterator.next();
                Boolean elementEquals = equals(leftObject, rightObject);
                if (elementEquals == null || elementEquals == false) {
                    return elementEquals;
                }
            }
            else {
                return false;
            }
        }

        if (rightIterator.hasNext()) { return rightIterator.next() == null ? null : false; }

        return true;
      }

      // Decimal equal
      // Have to use this because 10.0 != 10.00
      else if (left instanceof BigDecimal) {
          return ((BigDecimal)left).compareTo((BigDecimal)right) == 0;
      }

      else if (left instanceof Tuple) {
        HashMap<String, Object> leftMap = ((Tuple)left).getElements();
        HashMap<String, Object> rightMap = ((Tuple)right).getElements();
        for (String key : rightMap.keySet()) {
          if (leftMap.containsKey(key)) {
            if (equals(rightMap.get(key), leftMap.get(key)) == null) { return null; }
            else if (!equals(rightMap.get(key), leftMap.get(key))) { return false; }
          }
          else { return false; }
        }
        return true;
      }

      else if (left instanceof Time) {
        if (((Time)left).time.getValues().length < 4 || ((Time)right).time.getValues().length < 4) {
          return null;
        }
        return Arrays.equals(((Time)left).time.getValues(), ((Time)right).time.getValues())
               && ((Time)left).getTimezoneOffset().compareTo(((Time)right).getTimezoneOffset()) == 0;
      }

      else if (left instanceof DateTime && right instanceof DateTime) {
        // for DateTime equals, all the DateTime elements must be present -- any null values result in null return
        if (((DateTime)left).dateTime.getValues().length < 7 || ((DateTime)right).dateTime.getValues().length < 7) {
          return null;
        }
        return Arrays.equals(((DateTime)left).dateTime.getValues(), ((DateTime)right).dateTime.getValues())
               && ((DateTime)left).getTimezoneOffset().compareTo(((DateTime)right).getTimezoneOffset()) == 0;
      }

      return left.equals(right);
  }

  public static Boolean equivalent(Object left, Object right) {
    if ((left == null) && (right == null)) {
        return true;
    }

    if ((left == null) || (right == null)) {
      // Code and Concept equivalence never returns null
      if (left instanceof Code || right instanceof Code || left instanceof Concept || right instanceof Concept) {
        return false;
      }
      return null;
    }

    // list equivalent
    else if (left instanceof Iterable) {
      Iterator<Object> leftIterator = ((Iterable<Object>)left).iterator();
      Iterator<Object> rightIterator = ((Iterable<Object>)right).iterator();

      while (leftIterator.hasNext()) {
        Object leftObject = leftIterator.next();
        if (rightIterator.hasNext()) {
          Object rightObject = rightIterator.next();
          Boolean elementEquivalent = equivalent(leftObject, rightObject);
          if (elementEquivalent == null || elementEquivalent == false) {
              return elementEquivalent;
          }
        }
        else { return false; }
      }

      if (rightIterator.hasNext()) { return rightIterator.next() == null ? null : false; }

      return true;
    }

    else if (left instanceof Tuple) {
      HashMap<String, Object> leftMap = ((Tuple)left).getElements();
      HashMap<String, Object> rightMap = ((Tuple)right).getElements();
      for (String key : rightMap.keySet()) {
        if (leftMap.containsKey(key)) {
          if (equivalent(rightMap.get(key), leftMap.get(key)) == null) { return null; }
          else if (!equivalent(rightMap.get(key), leftMap.get(key))) { return false; }
        }
        else { return false; }
      }
      return true;
    }

    // Do not want to call the equals method for DateTime or Time - returns null if missing elements...
    else if (left instanceof DateTime && right instanceof DateTime) {
      DateTime leftDT = (DateTime)left;
      DateTime rightDT = (DateTime)right;
      if (leftDT.getPartial().size() != rightDT.getPartial().size()) { return false; }

      for (int i = 0; i < leftDT.getPartial().size(); ++i) {
        if (leftDT.getPartial().getValue(i) != rightDT.getPartial().getValue(i)) {
          return false;
        }
      }
      return true;
    }

    else if (left instanceof Time && right instanceof Time) {
      Time leftT = (Time)left;
      Time rightT = (Time)right;
      if (leftT.getPartial().size() != rightT.getPartial().size()) { return false; }

      for (int i = 0; i < leftT.getPartial().size(); ++i) {
        if (leftT.getPartial().getValue(i) != rightT.getPartial().getValue(i)) {
          return false;
        }
      }
      return true;
    }

    return equals(left, right);
  }

    public static Iterable<Object> ensureIterable(Object source) {
        if (source instanceof Iterable) {
            return (Iterable<Object>)source;
        }
        else {
            ArrayList sourceList = new ArrayList();
            if (source != null)
                sourceList.add(source);
            return sourceList;
        }
    }
}
