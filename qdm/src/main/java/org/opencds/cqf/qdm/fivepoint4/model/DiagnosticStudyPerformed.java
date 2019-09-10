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
public abstract class DiagnosticStudyPerformed extends BaseType implements Serializable
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

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "facility_location_code")),
            @AttributeOverride(name = "display", column = @Column(name = "facility_location_display")),
            @AttributeOverride(name = "system", column = @Column(name = "facility_location_system")),
            @AttributeOverride(name = "version", column = @Column(name = "facility_location_version"))
    })
    private Code facilityLocation;

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
        if (other instanceof DiagnosticStudyPerformed)
        {
            DiagnosticStudyPerformed diagnosticStudyPerformed = (DiagnosticStudyPerformed) other;
            super.copy(diagnosticStudyPerformed);
            setAuthorDatetime(diagnosticStudyPerformed.getAuthorDatetime());
            setRelevantPeriod(diagnosticStudyPerformed.getRelevantPeriod());
            setReason(diagnosticStudyPerformed.getReason());
            setResultInteger(diagnosticStudyPerformed.getResultInteger());
            setResultDecimal(diagnosticStudyPerformed.getResultDecimal());
            setResultCode(diagnosticStudyPerformed.getResultCode());
            setResultQuantity(diagnosticStudyPerformed.getResultQuantity());
            setResultRatio(diagnosticStudyPerformed.getResultRatio());
            setResultDatetime(diagnosticStudyPerformed.getResultDatetime());
            setStatus(diagnosticStudyPerformed.getStatus());
            setMethod(diagnosticStudyPerformed.getMethod());
            setFacilityLocation(diagnosticStudyPerformed.getFacilityLocation());
            setNegationRationale(diagnosticStudyPerformed.getNegationRationale());
//            setComponent(diagnosticStudyPerformed.getComponent());
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Cannot copy QDM types %s and %s", this.getClass().getName(), other.getClass().getName())
            );
        }
    }
}
