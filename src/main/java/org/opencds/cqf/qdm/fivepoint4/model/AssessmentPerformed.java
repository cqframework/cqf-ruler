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
public abstract class AssessmentPerformed extends BaseType implements Serializable
{
    private String authorDatetime;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "negation_rationale_code")),
            @AttributeOverride(name = "display", column = @Column(name = "negation_rationale_display")),
            @AttributeOverride(name = "system", column = @Column(name = "negation_rationale_system")),
            @AttributeOverride(name = "version", column = @Column(name = "negation_rationale_version"))
    })
    private Code negationRationale;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "reason_code")),
            @AttributeOverride(name = "display", column = @Column(name = "reason_display")),
            @AttributeOverride(name = "system", column = @Column(name = "reason_system")),
            @AttributeOverride(name = "version", column = @Column(name = "reason_version"))
    })
    private Code reason;

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
    private String resultTime;

//    @ElementCollection
//    @LazyCollection(LazyCollectionOption.FALSE)
//    private List<Component> component;

    @ElementCollection
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<Id> relatedTo;

    @Override
    public void copy(BaseType other)
    {
        if (other instanceof AssessmentPerformed)
        {
            AssessmentPerformed assessmentPerformed = (AssessmentPerformed) other;
            super.copy(assessmentPerformed);
            setAuthorDatetime(assessmentPerformed.getAuthorDatetime());
            setNegationRationale(assessmentPerformed.getNegationRationale());
            setReason(assessmentPerformed.getReason());
            setMethod(assessmentPerformed.getMethod());
            setResultInteger(assessmentPerformed.getResultInteger());
            setResultDecimal(assessmentPerformed.getResultDecimal());
            setResultCode(assessmentPerformed.getResultCode());
            setResultQuantity(assessmentPerformed.getResultQuantity());
            setResultRatio(assessmentPerformed.getResultRatio());
            setResultDatetime(assessmentPerformed.getResultDatetime());
            setResultTime(assessmentPerformed.getResultTime());
//            setComponent(assessmentPerformed.getComponent());
            setRelatedTo(assessmentPerformed.getRelatedTo());
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Cannot copy QDM types %s and %s", this.getClass().getName(), other.getClass().getName())
            );
        }
    }
}
