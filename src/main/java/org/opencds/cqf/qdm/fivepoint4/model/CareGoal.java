package org.opencds.cqf.qdm.fivepoint4.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

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

    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "related_to_value")),
            @AttributeOverride(name = "namingSystem", column = @Column(name = "related_to_system"))
    })
    private Id relatedTo;

    // TODO: add constraint that only one of the result[x] types can be present
    private Integer resultInteger;

    private BigDecimal resultDecimal;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "result_code")),
            @AttributeOverride(name = "display", column = @Column(name = "result_display")),
            @AttributeOverride(name = "system", column = @Column(name = "result_system")),
            @AttributeOverride(name = "version", column = @Column(name = "result_version"))
    })
    private Code resultCode;

    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "result_value")),
            @AttributeOverride(name = "unit", column = @Column(name = "result_unit"))
    })
    private Quantity resultQuantity;

    @AttributeOverrides({
            @AttributeOverride(name = "numerator", column = @Column(name = "result_numerator")),
            @AttributeOverride(name = "denominator", column = @Column(name = "result_denominator"))
    })
    private Ratio resultRatio;

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
            setResultInteger(careGoal.getResultInteger());
            setResultDecimal(careGoal.getResultDecimal());
            setResultCode(careGoal.getResultCode());
            setResultQuantity(careGoal.getResultQuantity());
            setResultRatio(careGoal.getResultRatio());
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Cannot copy QDM types %s and %s", this.getClass().getName(), other.getClass().getName())
            );
        }
    }
}
