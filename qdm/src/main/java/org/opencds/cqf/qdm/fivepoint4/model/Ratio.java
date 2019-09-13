package org.opencds.cqf.qdm.fivepoint4.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Ratio implements Serializable
{
    @NotNull
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "ratio_numerator_value")),
            @AttributeOverride(name = "unit", column = @Column(name = "ratio_numerator_unit"))
    })
    @Column(nullable = false)
    private Quantity numerator;

    @NotNull
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "ratio_denominator_value")),
            @AttributeOverride(name = "unit", column = @Column(name = "ratio_denominator_unit"))
    })
    @Column(nullable = false)
    private Quantity denominator;
}
