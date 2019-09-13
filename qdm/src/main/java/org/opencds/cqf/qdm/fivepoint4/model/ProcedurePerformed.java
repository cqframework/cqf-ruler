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
public abstract class ProcedurePerformed extends BaseType implements Serializable
{
    private String authorDatetime;

    @AttributeOverrides({
            @AttributeOverride(name = "start", column = @Column(name = "relevant_period_start")),
            @AttributeOverride(name = "end", column = @Column(name = "relevant_period_end"))
    })
    private DateTimeInterval relevantPeriod;

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

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "status_code")),
            @AttributeOverride(name = "display", column = @Column(name = "status_display")),
            @AttributeOverride(name = "system", column = @Column(name = "status_system")),
            @AttributeOverride(name = "version", column = @Column(name = "status_version"))
    })
    private Code status;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "anatomical_location_site_code")),
            @AttributeOverride(name = "display", column = @Column(name = "anatomical_location_site_display")),
            @AttributeOverride(name = "system", column = @Column(name = "anatomical_location_site_system")),
            @AttributeOverride(name = "version", column = @Column(name = "anatomical_location_site_version"))
    })
    private Code anatomicalLocationSite;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "ordinality_code")),
            @AttributeOverride(name = "display", column = @Column(name = "ordinality_display")),
            @AttributeOverride(name = "system", column = @Column(name = "ordinality_system")),
            @AttributeOverride(name = "version", column = @Column(name = "ordinality_version"))
    })
    private Code ordinality;

    private String incisionDatetime;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "negation_rationale_code")),
            @AttributeOverride(name = "display", column = @Column(name = "negation_rationale_display")),
            @AttributeOverride(name = "system", column = @Column(name = "negation_rationale_system")),
            @AttributeOverride(name = "version", column = @Column(name = "negation_rationale_version"))
    })
    private Code negationRationale;

//    @ElementCollection
//    @LazyCollection(LazyCollectionOption.FALSE)
//    private List<Component> component;

    @Override
    public void copy(BaseType other)
    {
        if (other instanceof ProcedurePerformed)
        {
            ProcedurePerformed procedurePerformed = (ProcedurePerformed) other;
            super.copy(procedurePerformed);
            setAuthorDatetime(procedurePerformed.getAuthorDatetime());
            setRelevantPeriod(procedurePerformed.getRelevantPeriod());
            setReason(procedurePerformed.getReason());
            setMethod(procedurePerformed.getMethod());
            setResultInteger(procedurePerformed.getResultInteger());
            setResultDecimal(procedurePerformed.getResultDecimal());
            setResultCode(procedurePerformed.getResultCode());
            setResultQuantity(procedurePerformed.getResultQuantity());
            setResultRatio(procedurePerformed.getResultRatio());
            setStatus(procedurePerformed.getStatus());
            setAnatomicalLocationSite(procedurePerformed.getAnatomicalLocationSite());
            setOrdinality(procedurePerformed.getOrdinality());
            setIncisionDatetime(procedurePerformed.getIncisionDatetime());
            setNegationRationale(procedurePerformed.getNegationRationale());
//            setComponent(procedurePerformed.getComponent());
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Cannot copy QDM types %s and %s", this.getClass().getName(), other.getClass().getName())
            );
        }
    }
}
