package org.opencds.cqf.qdm.fivepoint4.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;

@MappedSuperclass
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class ImmunizationAdministered extends BaseType implements Serializable
{
    private String authorDatetime;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "reason_code")),
            @AttributeOverride(name = "display", column = @Column(name = "reason_display")),
            @AttributeOverride(name = "system", column = @Column(name = "reason_system")),
            @AttributeOverride(name = "version", column = @Column(name = "reason_version"))
    })
    private Code reason;

    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "dosage_value")),
            @AttributeOverride(name = "unit", column = @Column(name = "dosage_unit"))
    })
    private Quantity dosage;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "route_code")),
            @AttributeOverride(name = "display", column = @Column(name = "route_display")),
            @AttributeOverride(name = "system", column = @Column(name = "route_system")),
            @AttributeOverride(name = "version", column = @Column(name = "route_version"))
    })
    private Code route;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "negation_rationale_code")),
            @AttributeOverride(name = "display", column = @Column(name = "negation_rationale_display")),
            @AttributeOverride(name = "system", column = @Column(name = "negation_rationale_system")),
            @AttributeOverride(name = "version", column = @Column(name = "negation_rationale_version"))
    })
    private Code negationRationale;

    @Override
    public void copy(BaseType other)
    {
        if (other instanceof ImmunizationAdministered)
        {
            ImmunizationAdministered interventionPerformed = (ImmunizationAdministered) other;
            super.copy(interventionPerformed);
            setAuthorDatetime(interventionPerformed.getAuthorDatetime());
            setReason(interventionPerformed.getReason());
            setDosage(interventionPerformed.getDosage());
            setRoute(interventionPerformed.getRoute());
            setNegationRationale(interventionPerformed.getNegationRationale());
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Cannot copy QDM types %s and %s", this.getClass().getName(), other.getClass().getName())
            );
        }
    }
}
