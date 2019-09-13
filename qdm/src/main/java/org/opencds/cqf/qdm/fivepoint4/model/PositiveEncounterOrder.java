package org.opencds.cqf.qdm.fivepoint4.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Entity;
import java.io.Serializable;

@Entity
public class PositiveEncounterOrder extends EncounterOrder implements Serializable
{
    @JsonIgnore
    @Override
    public String getName()
    {
        return "PositiveEncounterOrder";
    }

    @Override
    public void copy(BaseType other)
    {
        super.copy(other);
    }
}