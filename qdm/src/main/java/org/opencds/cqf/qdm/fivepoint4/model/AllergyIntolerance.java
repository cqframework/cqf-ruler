package org.opencds.cqf.qdm.fivepoint4.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import java.io.Serializable;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AllergyIntolerance extends BaseType implements Serializable
{
    private String authorDatetime;

    @AttributeOverrides({
            @AttributeOverride(name = "start", column = @Column(name = "prevalence_period_start")),
            @AttributeOverride(name = "end", column = @Column(name = "prevalence_period_end"))
    })
    private DateTimeInterval prevalencePeriod;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "type_code")),
            @AttributeOverride(name = "display", column = @Column(name = "type_display")),
            @AttributeOverride(name = "system", column = @Column(name = "type_system")),
            @AttributeOverride(name = "version", column = @Column(name = "type_version"))
    })
    private Code type;

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
        return "AllergyIntolerance";
    }

    @Override
    public void copy(BaseType other)
    {
        if (other instanceof AllergyIntolerance)
        {
            AllergyIntolerance diagnosis = (AllergyIntolerance) other;
            super.copy(diagnosis);
            setAuthorDatetime(diagnosis.getAuthorDatetime());
            setPrevalencePeriod(diagnosis.getPrevalencePeriod());
            setType(diagnosis.getType());
            setSeverity(diagnosis.getSeverity());
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Cannot copy QDM types %s and %s", this.getClass().getName(), other.getClass().getName())
            );
        }
    }
}
