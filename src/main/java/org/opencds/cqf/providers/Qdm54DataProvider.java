package org.opencds.cqf.providers;

import lombok.Data;
import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.runtime.TemporalHelper;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.cql.terminology.ValueSetInfo;
import org.opencds.cqf.qdm.fivepoint4.QdmContext;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.BaseRepository;
import org.opencds.cqf.qdm.fivepoint4.repository.PatientRepository;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Data
public class Qdm54DataProvider implements DataProvider
{
    private TerminologyProvider terminologyProvider;

    @Override
    public Iterable<Object> retrieve(String context, Object contextValue, String dataType, String templateId,
                                     String codePath, Iterable<Code> codes, String valueSet, String datePath,
                                     String dateLowPath, String dateHighPath, Interval dateRange)
    {
        List<Object> retVal = new ArrayList<>();
        List<BaseType> candidates = new ArrayList<>();
        boolean includeCandidate = true;

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

        try
        {
            if (context != null && context.equals("Patient") && contextValue != null && !contextValue.equals("null"))
            {
                if (dataType.equals("Patient"))
                {
                    Optional<Patient> searchResult = ((PatientRepository) QdmContext.getBean(Class.forName("org.opencds.cqf.qdm.fivepoint4.repository." + dataType + "Repository"))).findBySystemId(contextValue.toString().replace("Patient/", ""));
                    searchResult.ifPresent(retVal::add);
                }
                else
                {
                    Optional<List<BaseType>> searchResult = ((BaseRepository<BaseType>) QdmContext.getBean(Class.forName("org.opencds.cqf.qdm.fivepoint4.repository." + dataType + "Repository"))).findByPatientIdValue(contextValue.toString().replace("Patient/", ""));
                    if (searchResult.isPresent()) {
                        candidates = searchResult.get();
                    }
                }
            }
            // else if (context != null && context.equals("Patient") && (contextValue == null || contextValue.equals("null"))) {
            //     // No data if Context is Patient and  Patient Id is null.
            // }
            // // If context is not Patient, then it must be Population. Return all data.
            else {
                if (dataType.equals("Patient")) {
                    retVal.add(((PatientRepository) QdmContext.getBean(Class.forName("org.opencds.cqf.qdm.fivepoint4.repository." + dataType + "Repository"))).findAll());
                }
                else
                {
                    candidates = ((BaseRepository<BaseType>) QdmContext.getBean(Class.forName("org.opencds.cqf.qdm.fivepoint4.repository." + dataType + "Repository"))).findAll();
                }
            }
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException("Error retrieving QDM resource of type " + dataType);
        }

        if (codePath != null && !codePath.equals(""))
        {
            if (valueSet != null && !valueSet.equals(""))
            {
                if (terminologyProvider != null)
                {
                    ValueSetInfo valueSetInfo = new ValueSetInfo().withId(valueSet);
                    codes = terminologyProvider.expand(valueSetInfo);
                }
            }

            if (codes != null)
            {
                Set<String> hash = new HashSet<String>();
                for (Code code : codes)
                {
                    hash.add(code.getSystem() + code.getCode());
                }

                for (BaseType candidate : candidates)
                {
                    org.opencds.cqf.qdm.fivepoint4.model.Code c = candidate.getCode();
                    if (c != null && c.getSystem() != null && c.getCode() != null && hash.contains(c.getSystem() + c.getCode()))
                    {
                        retVal.add(candidate);
                    }
                }
            }
        }

        if (dateRange != null)
        {
            // TODO
        }

        if (retVal.isEmpty() && !candidates.isEmpty() && includeCandidate)
        {
            retVal.addAll(candidates);
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
        Method method;
        Object result;
        String methodName = String.format("get%s", s.substring(0, 1).toUpperCase() + s.substring(1));
        try
        {
            method = o.getClass().getMethod(methodName);
            result = method.invoke(o);
        }
        catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e)
        {
            e.printStackTrace();
            throw new RuntimeException(String.format("Unable to resolve method: %s on type: %s", methodName, o.getClass().getName()));
        }

        if (s.toLowerCase().endsWith("datetime")
                && result != null
                && result instanceof String)
        {
            return new DateTime((String) result, TemporalHelper.getDefaultZoneOffset());
        }

        if (result instanceof DateTimeInterval)
        {
            String start = ((DateTimeInterval) result).getStart();
            String end = ((DateTimeInterval) result).getEnd();
            return new Interval(
                    start != null ? new DateTime(start, TemporalHelper.getDefaultZoneOffset()) : null, true,
                    end != null ? new DateTime(end, TemporalHelper.getDefaultZoneOffset()) : null, true
            );
        }

        if (result instanceof org.opencds.cqf.qdm.fivepoint4.model.Code)
        {
            return new Code()
                    .withCode(((org.opencds.cqf.qdm.fivepoint4.model.Code) result).getCode())
                    .withSystem(((org.opencds.cqf.qdm.fivepoint4.model.Code) result).getSystem())
                    .withDisplay(((org.opencds.cqf.qdm.fivepoint4.model.Code) result).getDisplay())
                    .withVersion(((org.opencds.cqf.qdm.fivepoint4.model.Code) result).getVersion());
        }

        if (result instanceof Quantity)
        {
            return new org.opencds.cqf.cql.runtime.Quantity()
                    .withValue(((Quantity) result).getValue())
                    .withUnit(((Quantity) result).getUnit());
        }

        if (result instanceof QuantityInterval)
        {
            return new Interval(
                    new org.opencds.cqf.cql.runtime.Quantity()
                            .withValue(((QuantityInterval) result).getStart().getValue())
                            .withUnit(((QuantityInterval) result).getStart().getUnit()),
                    true,
                    new org.opencds.cqf.cql.runtime.Quantity()
                            .withValue(((QuantityInterval) result).getEnd().getValue())
                            .withUnit(((QuantityInterval) result).getEnd().getUnit()),
                    true
            );
        }

        return result;
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
        return o.equals(o1);
    }

    @Override
    public Boolean objectEquivalent(Object o, Object o1) {
        return o.equals(o1);
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
