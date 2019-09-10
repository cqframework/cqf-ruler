package org.opencds.cqf.qdm.fivepoint4.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FacilityLocation implements Serializable
{
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "facility_location_code")),
            @AttributeOverride(name = "display", column = @Column(name = "facility_location_display")),
            @AttributeOverride(name = "system", column = @Column(name = "facility_location_system")),
            @AttributeOverride(name = "version", column = @Column(name = "facility_location_version"))
    })
    @NotNull
    @Column(nullable = false)
    private Code code;

    @AttributeOverrides({
            @AttributeOverride(name = "start", column = @Column(name = "location_period_start")),
            @AttributeOverride(name = "end", column = @Column(name = "location_period_end"))
    })
    private DateTimeInterval locationPeriod;
}
