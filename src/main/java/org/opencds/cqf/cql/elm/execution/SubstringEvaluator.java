package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;

/*
Substring(stringToSub String, startIndex Integer) String
Substring(stringToSub String, startIndex Integer, length Integer) String

The Substring operator returns the string within stringToSub, starting at the 0-based index startIndex,
  and consisting of length characters.
If length is ommitted, the substring returned starts at startIndex and continues to the end of stringToSub.
If stringToSub or startIndex is null, or startIndex is out of range, the result is null.
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class SubstringEvaluator extends org.cqframework.cql.elm.execution.Substring {

    @Override
    public Object evaluate(Context context) {
        Object stringValue = getStringToSub().evaluate(context);
        Object startIndexValue = getStartIndex().evaluate(context);
        Object lengthValue = getLength() == null ? null : getLength().evaluate(context);

        if (stringValue == null || startIndexValue == null) {
            return null;
        }

        String string = (String)stringValue;
        Integer startIndex = (Integer)startIndexValue;

        if (startIndex < 0 || startIndex >= string.length()) {
            return null;
        }

        if (lengthValue == null) {
            return string.substring(startIndex);
        }
        else {
            int endIndex = startIndex + (Integer)lengthValue;
            if (endIndex > string.length()) {
                endIndex = string.length();
            }

            if (endIndex < startIndex) {
                return null;
            }

            return string.substring(startIndex, endIndex);
        }
    }
}
