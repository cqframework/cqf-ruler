package org.opencds.cqf.qdm.fivepoint4.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Entity;
import java.io.Serializable;

@Entity
public class NegativePhysicalExamPerformed extends PhysicalExamPerformed implements Serializable
{
    @JsonIgnore
    @Override
    public String getName()
    {
        return "NegativePhysicalExamPerformed";
    }

    @Override
    public void copy(BaseType other)
    {
        super.copy(other);
    }
}
