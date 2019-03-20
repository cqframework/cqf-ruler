package org.opencds.cqf.qdm.fivepoint4.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Entity;
import java.io.Serializable;

@Entity
public class PositiveDiagnosticStudyRecommended extends DiagnosticStudyRecommended implements Serializable
{
    @JsonIgnore
    @Override
    public String getName()
    {
        return "PositiveDiagnosticStudyRecommended";
    }

    @Override
    public void copy(BaseType other)
    {
        super.copy(other);
    }
}