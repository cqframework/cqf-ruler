package org.opencds.cqf.qdm.fivepoint4.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@MappedSuperclass
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class InterventionOrder extends BaseType implements Serializable
{
    private String authorDatetime;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "reason_code")),
            @AttributeOverride(name = "display", column = @Column(name = "reason_display")),
            @AttributeOverride(name = "system", column = @Column(name = "reason_system")),
            @AttributeOverride(name = "version", column = @Column(name = "reason_version"))
    })
    private Code reason;

   @ElementCollection
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<FacilityLocation> facilityLocation;

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
        if (other instanceof InterventionOrder)
        {
            InterventionOrder interventionOrder = (InterventionOrder) other;
            super.copy(interventionOrder);
            setAuthorDatetime(interventionOrder.getAuthorDatetime());
            setReason(interventionOrder.getReason());
            setFacilityLocation(interventionOrder.getFacilityLocation());
            setNegationRationale(interventionOrder.getNegationRationale());
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Cannot copy QDM types %s and %s", this.getClass().getName(), other.getClass().getName())
            );
        }
    }
}
