package org.opencds.cqf.qdm.fivepoint4.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@MappedSuperclass
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class EncounterPerformed extends BaseType implements Serializable
{
    private String authorDatetime;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "admission_source_code")),
            @AttributeOverride(name = "display", column = @Column(name = "admission_source_display")),
            @AttributeOverride(name = "system", column = @Column(name = "admission_source_system")),
            @AttributeOverride(name = "version", column = @Column(name = "admission_source_version"))
    })
    @NotNull
    @Column(nullable = false)
    private Code admissionSource;

    @AttributeOverrides({
            @AttributeOverride(name = "start", column = @Column(name = "relevant_period_start")),
            @AttributeOverride(name = "end", column = @Column(name = "relevant_period_end"))
    })
    private DateTimeInterval relevantPeriod;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "discharge_disposition_code")),
            @AttributeOverride(name = "display", column = @Column(name = "discharge_disposition_display")),
            @AttributeOverride(name = "system", column = @Column(name = "discharge_disposition_system")),
            @AttributeOverride(name = "version", column = @Column(name = "discharge_disposition_version"))
    })
    private Code dischargeDisposition;

    @ElementCollection
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<Code> diagnosis;

    @ElementCollection
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<FacilityLocation> facilityLocation;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "principal_diagnosis_code")),
            @AttributeOverride(name = "display", column = @Column(name = "principal_diagnosis_display")),
            @AttributeOverride(name = "system", column = @Column(name = "principal_diagnosis_system")),
            @AttributeOverride(name = "version", column = @Column(name = "principal_diagnosis_version"))
    })
    private Code principalDiagnosis;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "negation_rationale_code")),
            @AttributeOverride(name = "display", column = @Column(name = "negation_rationale_display")),
            @AttributeOverride(name = "system", column = @Column(name = "negation_rationale_system")),
            @AttributeOverride(name = "version", column = @Column(name = "negation_rationale_version"))
    })
    private Code negationRationale;

    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "length_of_stay_value")),
            @AttributeOverride(name = "unit", column = @Column(name = "length_of_stay_unit"))
    })
    private Quantity lengthOfStay;

    @Override
    public void copy(BaseType other)
    {
        if (other instanceof EncounterPerformed)
        {
            EncounterPerformed encounterPerformed = (EncounterPerformed) other;
            super.copy(encounterPerformed);
            setAuthorDatetime(encounterPerformed.getAuthorDatetime());
            setAdmissionSource(encounterPerformed.getAdmissionSource());
            setRelevantPeriod(encounterPerformed.getRelevantPeriod());
            setDischargeDisposition(encounterPerformed.getDischargeDisposition());
            setDiagnosis(encounterPerformed.getDiagnosis());
            setFacilityLocation(encounterPerformed.getFacilityLocation());
            setPrincipalDiagnosis(encounterPerformed.getPrincipalDiagnosis());
            setNegationRationale(encounterPerformed.getNegationRationale());
            setLengthOfStay(encounterPerformed.getLengthOfStay());
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Cannot copy QDM types %s and %s", this.getClass().getName(), other.getClass().getName())
            );
        }
    }
}
