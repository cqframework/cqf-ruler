package org.opencds.cqf.providers;

import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.qdm.fivepoint4.QdmContext;
import org.opencds.cqf.qdm.fivepoint4.model.BaseType;
import org.opencds.cqf.qdm.fivepoint4.repository.BaseRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Qdm54DataProvider implements DataProvider
{
    @Override
    public Iterable<Object> retrieve(String context, Object contextValue, String dataType, String templateId,
                                     String codePath, Iterable<Code> codes, String valueSet, String datePath,
                                     String dateLowPath, String dateHighPath, Interval dateRange)
    {
        Object retVal = null;

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

        }

        if (dateRange != null)
        {

        }

        return ensureIterable(candidates);
    }

    @Override
    public String getPackageName()
    {
        return "org.opencds.cqf.qdm.fivepoint4.model";
    }

    @Override
    public void setPackageName(String s)
    {

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
