package org.opencds.cqf.qdm.fivepoint4.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Component implements Serializable
{
    @NotNull
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "component_code_code")),
            @AttributeOverride(name = "display", column = @Column(name = "component_code_display")),
            @AttributeOverride(name = "system", column = @Column(name = "component_code_system")),
            @AttributeOverride(name = "version", column = @Column(name = "component_code_version"))
    })
    @Column(nullable = false)
    private Code code;

    // TODO: add constraint that only one of the result[x] types can be present
    private Integer resultInteger;

    private BigDecimal resultDecimal;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "component_result_code")),
            @AttributeOverride(name = "display", column = @Column(name = "component_result_display")),
            @AttributeOverride(name = "system", column = @Column(name = "component_result_system")),
            @AttributeOverride(name = "version", column = @Column(name = "component_result_version"))
    })
    private Code resultCode;

    @AttributeOverrides({
            @AttributeOverride(name = "component_value", column = @Column(name = "component_result_value")),
            @AttributeOverride(name = "component_unit", column = @Column(name = "component_result_unit"))
    })
    private Quantity resultQuantity;

    @AttributeOverrides({
            @AttributeOverride(name = "component_numerator", column = @Column(name = "component_result_numerator")),
            @AttributeOverride(name = "component_denominator", column = @Column(name = "component_result_denominator"))
    })
    private Ratio resultRatio;

    private String resultDateTime;

    private String resultTime;
}
