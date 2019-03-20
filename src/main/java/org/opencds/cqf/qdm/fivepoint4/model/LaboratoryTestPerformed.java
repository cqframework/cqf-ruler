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
import java.math.BigDecimal;
import java.util.List;

@MappedSuperclass
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class LaboratoryTestPerformed extends BaseType implements Serializable
{
    private String authorDatetime;

    @AttributeOverrides({
            @AttributeOverride(name = "start", column = @Column(name = "relevant_period_start")),
            @AttributeOverride(name = "end", column = @Column(name = "relevant_period_end"))
    })
    private DateTimeInterval relevantPeriod;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "status_code")),
            @AttributeOverride(name = "display", column = @Column(name = "status_display")),
            @AttributeOverride(name = "system", column = @Column(name = "status_system")),
            @AttributeOverride(name = "version", column = @Column(name = "status_version"))
    })
    private Code status;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "method_code")),
            @AttributeOverride(name = "display", column = @Column(name = "method_display")),
            @AttributeOverride(name = "system", column = @Column(name = "method_system")),
            @AttributeOverride(name = "version", column = @Column(name = "method_version"))
    })
    private Code method;

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

    private String resultDatetime;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "reason_code")),
            @AttributeOverride(name = "display", column = @Column(name = "reason_display")),
            @AttributeOverride(name = "system", column = @Column(name = "reason_system")),
            @AttributeOverride(name = "version", column = @Column(name = "reason_version"))
    })
    private Code reason;

    @AttributeOverrides({
            @AttributeOverride(name = "start", column = @Column(name = "ltp_reference_range_start")),
            @AttributeOverride(name = "end", column = @Column(name = "ltp_reference_range_end"))
    })
    private QuantityInterval referenceRange;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "negation_rationale_code")),
            @AttributeOverride(name = "display", column = @Column(name = "negation_rationale_display")),
            @AttributeOverride(name = "system", column = @Column(name = "negation_rationale_system")),
            @AttributeOverride(name = "version", column = @Column(name = "negation_rationale_version"))
    })
    private Code negationRationale;

//    @ElementCollection
//    @LazyCollection(LazyCollectionOption.FALSE)
//    private List<ResultComponent> components;

    @Override
    public void copy(BaseType other)
    {
        if (other instanceof LaboratoryTestPerformed)
        {
            LaboratoryTestPerformed laboratoryTestPerformed = (LaboratoryTestPerformed) other;
            super.copy(laboratoryTestPerformed);
            setAuthorDatetime(laboratoryTestPerformed.getAuthorDatetime());
            setRelevantPeriod(laboratoryTestPerformed.getRelevantPeriod());
            setStatus(laboratoryTestPerformed.getStatus());
            setMethod(laboratoryTestPerformed.getMethod());
            setResultInteger(laboratoryTestPerformed.getResultInteger());
            setResultDecimal(laboratoryTestPerformed.getResultDecimal());
            setResultCode(laboratoryTestPerformed.getResultCode());
            setResultQuantity(laboratoryTestPerformed.getResultQuantity());
            setResultRatio(laboratoryTestPerformed.getResultRatio());
            setResultDatetime(laboratoryTestPerformed.getResultDatetime());
            setReason(laboratoryTestPerformed.getReason());
            setReferenceRange(laboratoryTestPerformed.getReferenceRange());
            setNegationRationale(laboratoryTestPerformed.getNegationRationale());
//            setComponents(laboratoryTestPerformed.getComponents());
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Cannot copy QDM types %s and %s", this.getClass().getName(), other.getClass().getName())
            );
        }
    }
}
