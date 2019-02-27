package org.opencds.cqf.qdm.fivepoint4.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@MappedSuperclass
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EncounterPerformed extends BaseType implements Serializable
{
    private String authorDateTime;

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

    @ElementCollection(fetch = FetchType.LAZY)
//    @CollectionTable(name = "diagnosis_list", joinColumns = @JoinColumn(name = "system_id"))
//    @AttributeOverrides({
//            @AttributeOverride(name = "code", column = @Column(name = "diagnosis_code")),
//            @AttributeOverride(name = "display", column = @Column(name = "diagnosis_display")),
//            @AttributeOverride(name = "system", column = @Column(name = "diagnosis_system")),
//            @AttributeOverride(name = "version", column = @Column(name = "diagnosis_version"))
//    })
    private List<Code> diagnosis = new ArrayList<>();

    @ElementCollection
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
}
