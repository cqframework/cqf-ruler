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
public class Symptom extends BaseType implements Serializable
{
    @AttributeOverrides({
            @AttributeOverride(name = "start", column = @Column(name = "prevalence_period_start")),
            @AttributeOverride(name = "end", column = @Column(name = "prevalence_period_end"))
    })
    private DateTimeInterval prevalencePeriod;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "severity_code")),
            @AttributeOverride(name = "display", column = @Column(name = "severity_display")),
            @AttributeOverride(name = "system", column = @Column(name = "severity_system")),
            @AttributeOverride(name = "version", column = @Column(name = "severity_version"))
    })
    private Code severity;

    @JsonIgnore
    @Override
    public String getName() {
        return "Symptom";
    }

    @Override
    public void copy(BaseType other)
    {
        if (other instanceof Symptom)
        {
            Symptom symptom = (Symptom) other;
            super.copy(symptom);
            setPrevalencePeriod(symptom.getPrevalencePeriod());
            setSeverity(symptom.getSeverity());
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Cannot copy QDM types %s and %s", this.getClass().getName(), other.getClass().getName())
            );
        }
    }
}
