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
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultComponent extends Component implements Serializable
{
//    @NotNull
//    @AttributeOverrides({
//            @AttributeOverride(name = "code", column = @Column(name = "code_code")),
//            @AttributeOverride(name = "display", column = @Column(name = "code_display")),
//            @AttributeOverride(name = "system", column = @Column(name = "code_system")),
//            @AttributeOverride(name = "version", column = @Column(name = "code_version"))
//    })
//    @Column(nullable = false)
//    private Code code;
//
//    // TODO: add constraint that only one of the result[x] types can be present
//    private Integer resultInteger;
//
//    private BigDecimal resultDecimal;
//
//    @AttributeOverrides({
//            @AttributeOverride(name = "code", column = @Column(name = "result_code")),
//            @AttributeOverride(name = "display", column = @Column(name = "result_display")),
//            @AttributeOverride(name = "system", column = @Column(name = "result_system")),
//            @AttributeOverride(name = "version", column = @Column(name = "result_version"))
//    })
//    private Code resultCode;
//
//    @AttributeOverrides({
//            @AttributeOverride(name = "value", column = @Column(name = "result_value")),
//            @AttributeOverride(name = "unit", column = @Column(name = "result_unit"))
//    })
//    private Quantity resultQuantity;
//
//    @AttributeOverrides({
//            @AttributeOverride(name = "numerator", column = @Column(name = "result_numerator")),
//            @AttributeOverride(name = "denominator", column = @Column(name = "result_denominator"))
//    })
//    private Ratio resultRatio;
//
//    private String resultDateTime;
//
//    private String resultTime;

    @AttributeOverrides({
            @AttributeOverride(name = "start", column = @Column(name = "result_reference_range_start")),
            @AttributeOverride(name = "end", column = @Column(name = "result_reference_range_end"))
    })
    @Embedded
    private QuantityInterval referenceRange;
}
