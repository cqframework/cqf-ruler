package org.opencds.cqf.providers;

import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.cql.terminology.ValueSetInfo;
import org.opencds.cqf.qdm.fivepoint4.QdmContext;
import org.opencds.cqf.qdm.fivepoint4.model.BaseType;
import org.opencds.cqf.qdm.fivepoint4.repository.BaseRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Qdm54DataProvider implements DataProvider
{

    protected TerminologyProvider terminologyProvider;
    @Override
    public Iterable<Object> retrieve(String context, Object contextValue, String dataType, String templateId,
                                     String codePath, Iterable<Code> codes, String valueSet, String datePath,
                                     String dateLowPath, String dateHighPath, Interval dateRange)
    {
        List<BaseType> retVal = new ArrayList<>();
        List<BaseType> candidates = new ArrayList<>();

        if (valueSet != null && valueSet.startsWith("urn:oid:"))
        {
            valueSet = valueSet.replace("urn:oid:", "");
        }

        if (codePath == null && (codes != null || valueSet != null))
        {
            throw new IllegalArgumentException("A code path must be provided when filtering on codes or a valueset.");
        }

        if (dataType == null)
        {
            throw new IllegalArgumentException("A data type (i.e. Procedure, Valueset, etc...) must be specified for clinical data retrieval");
        }

        if (context != null && context.equals("Patient") && contextValue != null)
        {
            try {
                Optional<List<BaseType>> searchResult = ((BaseRepository<BaseType>) QdmContext.getBean(Class.forName("org.opencds.cqf.qdm.fivepoint4.repository." + dataType + "Repository"))).findByPatientIdValue(contextValue.toString().replace("Patient/", ""));
                if (searchResult.isPresent())
                {
                    candidates = searchResult.get();
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (codePath != null && !codePath.equals(""))
        {
            if (valueSet != null && !valueSet.equals("")) {
                if (terminologyProvider != null) {
                    ValueSetInfo valueSetInfo = new ValueSetInfo().withId(valueSet);
                    codes = terminologyProvider.expand(valueSetInfo);
                }
            }

            if (codes != null) {
                for (Code code : codes) {
                    for (BaseType candidate : candidates)
                    {
                        if (candidate.getCode() != null
                                && candidate.getCode().getCode() != null
                                && candidate.getCode().getCode().equals(code.getCode())
                                && candidate.getCode().getSystem() != null
                                && candidate.getCode().getSystem().equals(code.getSystem()))
                        {
                            retVal.add(candidate);
                        }
                    }
                }
            }
        }

        if (dateRange != null)
        {

        }

        return ensureIterable(retVal);
    }

    private String packageName = "org.opencds.cqf.qdm.fivepoint4.model";
    @Override
    public String getPackageName()
    {
        return packageName;
    }

    @Override
    public void setPackageName(String packageName)
    {
        this.packageName = packageName;
    }

    @Override
    public Object resolvePath(Object o, String s)
    {
        return null;
    }

    @Override
    public Class resolveType(String s)
    {
        return null;
    }

    @Override
    public Class resolveType(Object o)
    {
        return null;
    }

    @Override
    public Object createInstance(String s)
    {
        return null;
    }

    @Override
    public void setValue(Object o, String s, Object o1)
    {

    }

    @Override
    public Boolean objectEqual(Object o, Object o1) {
        return null;
    }

    @Override
    public Boolean objectEquivalent(Object o, Object o1) {
        return null;
    }

    private Iterable<Object> ensureIterable(Object candidate)
    {
        if (candidate instanceof Iterable)
        {
            return (Iterable<Object>) candidate;
        }

        return Collections.singletonList(candidate);
    }
}
