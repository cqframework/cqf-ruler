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
public class PatientCareExperience extends BaseType implements Serializable
{
    private String authorDatetime;

    @JsonIgnore
    @Override
    public String getName() {
        return "PatientCareExperience";
    }

    @Override
    public void copy(BaseType other)
    {
        if (other instanceof PatientCareExperience)
        {
            PatientCareExperience patientCareExperience = (PatientCareExperience) other;
            super.copy(other);
            setAuthorDatetime(patientCareExperience.getAuthorDatetime());
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Cannot copy QDM types %s and %s", this.getClass().getName(), other.getClass().getName())
            );
        }
    }
}
