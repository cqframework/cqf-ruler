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
public class MedicationActive extends BaseType implements Serializable
{
    @AttributeOverrides({
            @AttributeOverride(name = "start", column = @Column(name = "prevalence_period_start")),
            @AttributeOverride(name = "end", column = @Column(name = "prevalence_period_end"))
    })
    private DateTimeInterval relevantPeriod;

    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "dosage_value")),
            @AttributeOverride(name = "unit", column = @Column(name = "dosage_unit"))
    })
    private Quantity dosage;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "frequency_code")),
            @AttributeOverride(name = "display", column = @Column(name = "frequency_display")),
            @AttributeOverride(name = "system", column = @Column(name = "frequency_system")),
            @AttributeOverride(name = "version", column = @Column(name = "frequency_version"))
    })
    private Code frequency;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "route_code")),
            @AttributeOverride(name = "display", column = @Column(name = "route_display")),
            @AttributeOverride(name = "system", column = @Column(name = "route_system")),
            @AttributeOverride(name = "version", column = @Column(name = "route_version"))
    })
    private Code route;

    @JsonIgnore
    @Override
    public String getName() {
        return "MedicationActive";
    }

    @Override
    public void copy(BaseType other)
    {
        if (other instanceof MedicationActive)
        {
            MedicationActive medicationActive = (MedicationActive) other;
            super.copy(medicationActive);
            setRelevantPeriod(medicationActive.getRelevantPeriod());
            setDosage(medicationActive.getDosage());
            setFrequency(medicationActive.getFrequency());
            setRoute(medicationActive.getRoute());
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Cannot copy QDM types %s and %s", this.getClass().getName(), other.getClass().getName())
            );
        }
    }
}
