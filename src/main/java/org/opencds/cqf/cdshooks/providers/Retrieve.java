package org.opencds.cqf.cdshooks.providers;

import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.CqlList;
import org.opencds.cqf.cql.runtime.Interval;

public class Retrieve {

    private String context;
    private Object contextValue;
    private String dataType;
    private String templateId;
    private String codePath;
    private Iterable<Code> codes;
    private String valueSet;
    private String datePath;
    private String dateLowPath;
    private String dateHighPath;
    private Interval dateRange;

    public Retrieve(String context, Object contextValue, String dataType, String templateId,
                    String codePath, Iterable<Code> codes, String valueSet, String datePath,
                    String dateLowPath, String dateHighPath, Interval dateRange)
    {
        this.context = context;
        this.contextValue = contextValue;
        this.dataType = dataType;
        this.templateId = templateId;
        this.codePath = codePath;
        this.codes = codes;
        this.valueSet = valueSet;
        this.datePath = datePath;
        this.dateLowPath = dateLowPath;
        this.dateHighPath = dateHighPath;
        this.dateRange = dateRange;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Retrieve)) {
            return false;
        }

        Retrieve retrieve = (Retrieve) other;

        if (!isEquivalent(context, retrieve.context)
                || !isEquivalent(contextValue, retrieve.contextValue)
                || !isEquivalent(dataType, retrieve.dataType)
                || !isEquivalent(templateId, retrieve.templateId)
                || !isEquivalent(codePath, retrieve.codePath)
                || !isEquivalent(valueSet, retrieve.valueSet)
                || !isEquivalent(datePath, retrieve.datePath)
                || !isEquivalent(dateLowPath, retrieve.dateLowPath)
                || !isEquivalent(dateHighPath, retrieve.dateHighPath))
        {
            return false;
        }

        else {
            if (codes != null) {
                if (retrieve.codes == null) {
                    return false;
                }
                if (!CqlList.equivalent(codes, retrieve.codes)) {
                    return false;
                }
            }
            else if (retrieve.codes != null) {
                return false;
            }

            if (dateRange != null) {
                if (retrieve.dateRange == null) {
                    return false;
                }
                if (!dateRange.equivalent(retrieve.dateRange)) {
                    return false;
                }
            }
            else if (retrieve.dateRange != null) {
                return false;
            }
        }

        return true;
    }

    private boolean isEquivalent(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }
}
