package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.Concept;
import org.opencds.cqf.cql.runtime.Interval;
import org.cqframework.cql.elm.execution.ValueSetRef;
import org.cqframework.cql.elm.execution.ValueSetDef;
import java.util.ArrayList;

/**
 * Created by Bryn on 5/25/2016.
 */
public class RetrieveEvaluator extends org.cqframework.cql.elm.execution.Retrieve {

    public Object evaluate(Context context) {
        DataProvider dataProvider = context.resolveDataProvider(this.dataType);
        Iterable<Code> codes = null;
        String valueSet = null;
        if (this.getCodes() != null) {
            if (this.getCodes() instanceof ValueSetRef) {
                ValueSetRef valueSetRef = (ValueSetRef)this.getCodes();
                ValueSetDef valueSetDef = context.resolveValueSetRef(valueSetRef.getLibraryName(), valueSetRef.getName());
                // TODO: Handle value set versions.....
                valueSet = valueSetDef.getId();
            }
            else {
                Object codesResult = this.getCodes().evaluate(context);
                if (codesResult instanceof Code) {
                    ArrayList<Code> codesList = new ArrayList<>();
                    codesList.add((Code)codesResult);
                    codes = codesList;
                }
                else if (codesResult instanceof Concept) {
                    ArrayList<Code> codesList = new ArrayList<>();
                    for (Code conceptCode : ((Concept)codesResult).getCodes()) {
                        codesList.add(conceptCode);
                    }
                    codes = codesList;
                }
                else {
                    codes = (Iterable<Code>) this.getCodes().evaluate(context);
                }
            }
        }
        Interval dateRange = null;
        if (this.getDateRange() != null) {
            dateRange = (Interval)this.getDateRange().evaluate(context);
        }

        return dataProvider.retrieve(context.getCurrentContext(), context.getCurrentContextValue(), getDataType().getLocalPart(), getTemplateId(),
                getCodeProperty(), codes, valueSet, getDateProperty(), getDateLowProperty(), getDateHighProperty(), dateRange);
    }
}
