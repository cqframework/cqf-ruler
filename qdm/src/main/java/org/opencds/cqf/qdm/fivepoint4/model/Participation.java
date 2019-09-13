package org.opencds.cqf.qdm.fivepoint4.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Participation extends BaseType implements Serializable
{
    @AttributeOverrides({
            @AttributeOverride(name = "start", column = @Column(name = "participation_period_start")),
            @AttributeOverride(name = "end", column = @Column(name = "participation_period_end"))
    })
    private DateTimeInterval participationPeriod;

    @JsonIgnore
    @Override
    public String getName() {
        return "Participation";
    }

    @Override
    public void copy(BaseType other)
    {
        if (other instanceof Participation)
        {
            Participation participation = (Participation) other;
            super.copy(participation);
            setParticipationPeriod(participation.getParticipationPeriod());
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Cannot copy QDM types %s and %s", this.getClass().getName(), other.getClass().getName())
            );
        }
    }
}
