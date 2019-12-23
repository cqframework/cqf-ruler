package org.opencds.cqf.qdm.providers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.opencds.cqf.cql.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.cql.terminology.ValueSetInfo;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.QdmContext;
import org.opencds.cqf.qdm.fivepoint4.repository.BaseRepository;
import org.opencds.cqf.qdm.fivepoint4.repository.PatientRepository;

public class Qdm54RetrieveProvider implements RetrieveProvider {

    public Qdm54RetrieveProvider(TerminologyProvider terminologyProvider) {
        this.terminologyProvider = terminologyProvider;
    }

	private TerminologyProvider terminologyProvider;

    @Override
    public Iterable<Object> retrieve(String context, String contextPath, Object contextValue, String dataType, String templateId,
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

    private Iterable<Object> ensureIterable(Object candidate)
    {
        if (candidate instanceof Iterable)
        {
            return (Iterable<Object>) candidate;
        }

        return Collections.singletonList(candidate);
    }
}