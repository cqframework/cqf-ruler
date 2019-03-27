package org.opencds.cqf.qdm.fivepoint4.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import java.io.Serializable;

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PatientCharacteristicRace extends BaseType implements Serializable
{
    @JsonIgnore
    @Override
    public String getName() {
        return "PatientCharacteristicRace";
    }

    @Override
    public void copy(BaseType other)
    {
        super.copy(other);
    }
}
