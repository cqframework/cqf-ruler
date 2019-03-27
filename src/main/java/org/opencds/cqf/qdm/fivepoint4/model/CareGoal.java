package org.opencds.cqf.qdm.fivepoint4.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CareGoal extends BaseType implements Serializable
{
    @AttributeOverrides({
            @AttributeOverride(name = "start", column = @Column(name = "relevant_period_start")),
            @AttributeOverride(name = "end", column = @Column(name = "relevant_period_end"))
    })
    private DateTimeInterval relevantPeriod;

    @ElementCollection
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<Id> relatedTo;

    // TODO: add constraint that only one of the targetOutcome[x] types can be present
    private Integer targetOutcomeInteger;

    private BigDecimal targetOutcomeDecimal;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "target_outcome_code")),
            @AttributeOverride(name = "display", column = @Column(name = "target_outcome_display")),
            @AttributeOverride(name = "system", column = @Column(name = "target_outcome_system")),
            @AttributeOverride(name = "version", column = @Column(name = "target_outcome_version"))
    })
    private Code targetOutcomeCode;

    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "target_outcome_value")),
            @AttributeOverride(name = "unit", column = @Column(name = "target_outcome_unit"))
    })
    private Quantity targetOutcomeQuantity;

    @AttributeOverrides({
            @AttributeOverride(name = "numerator", column = @Column(name = "target_outcome_numerator")),
            @AttributeOverride(name = "denominator", column = @Column(name = "target_outcome_denominator"))
    })
    private Ratio targetOutcomeRatio;

    @JsonIgnore
    @Override
    public String getName() {
        return "CareGoal";
    }

    @Override
    public void copy(BaseType other)
    {
        if (other instanceof CareGoal)
        {
            CareGoal careGoal = (CareGoal) other;
            super.copy(careGoal);
            setRelevantPeriod(careGoal.getRelevantPeriod());
            setRelatedTo(careGoal.getRelatedTo());
            setTargetOutcomeInteger(careGoal.getTargetOutcomeInteger());
            setTargetOutcomeDecimal(careGoal.getTargetOutcomeDecimal());
            setTargetOutcomeCode(careGoal.getTargetOutcomeCode());
            setTargetOutcomeQuantity(careGoal.getTargetOutcomeQuantity());
            setTargetOutcomeRatio(careGoal.getTargetOutcomeRatio());
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Cannot copy QDM types %s and %s", this.getClass().getName(), other.getClass().getName())
            );
        }
    }
}
