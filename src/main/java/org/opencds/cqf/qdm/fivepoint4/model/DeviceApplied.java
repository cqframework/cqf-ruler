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
public abstract class DeviceApplied extends BaseType implements Serializable
{
    private String authorDatetime;

    @AttributeOverrides({
            @AttributeOverride(name = "start", column = @Column(name = "relevant_period_start")),
            @AttributeOverride(name = "end", column = @Column(name = "relevant_period_end"))
    })
    private DateTimeInterval relevantPeriod;

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
            @AttributeOverride(name = "code", column = @Column(name = "anatomical_location_site_code")),
            @AttributeOverride(name = "display", column = @Column(name = "anatomical_location_site_display")),
            @AttributeOverride(name = "system", column = @Column(name = "anatomical_location_site_system")),
            @AttributeOverride(name = "version", column = @Column(name = "anatomical_location_site_version"))
    })
    private Code anatomicalLocationSite;

    @Override
    public void copy(BaseType other)
    {
        if (other instanceof DeviceApplied)
        {
            DeviceApplied deviceApplied = (DeviceApplied) other;
            super.copy(deviceApplied);
            setAuthorDatetime(deviceApplied.getAuthorDatetime());
            setRelevantPeriod(deviceApplied.getRelevantPeriod());
            setNegationRationale(deviceApplied.getNegationRationale());
            setReason(deviceApplied.getReason());
            setAnatomicalLocationSite(deviceApplied.getAnatomicalLocationSite());
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Cannot copy QDM types %s and %s", this.getClass().getName(), other.getClass().getName())
            );
        }
    }
}
