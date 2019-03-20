package org.opencds.cqf.qdm.fivepoint4.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuantityInterval implements Serializable
{
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "qty_ivl_start_value")),
            @AttributeOverride(name = "unit", column = @Column(name = "qty_ivl_start_unit"))
    })
    @Embedded
    private Quantity start;

    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "qty_ivl_end_value")),
            @AttributeOverride(name = "unit", column = @Column(name = "qty_ivl_end_unit"))
    })
    @Embedded
    private Quantity end;
}
