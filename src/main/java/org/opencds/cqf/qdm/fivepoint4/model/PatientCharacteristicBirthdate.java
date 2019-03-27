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
public class PatientCharacteristicBirthdate extends BaseType implements Serializable
{
    private String birthDatetime;

    @JsonIgnore
    @Override
    public String getName() {
        return "PatientCharacteristicBirthdate";
    }

    @Override
    public void copy(BaseType other)
    {
        if (other instanceof PatientCharacteristicBirthdate)
        {
            PatientCharacteristicBirthdate pcb = (PatientCharacteristicBirthdate) other;
            super.copy(other);
            setBirthDatetime(pcb.getBirthDatetime());
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Cannot copy QDM types %s and %s", this.getClass().getName(), other.getClass().getName())
            );
        }
    }
}
