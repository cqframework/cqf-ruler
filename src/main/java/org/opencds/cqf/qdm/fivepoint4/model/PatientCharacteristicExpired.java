package org.opencds.cqf.qdm.fivepoint4.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import java.io.Serializable;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PatientCharacteristicExpired extends BaseType implements Serializable
{
    private String expiredDatetime;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "cause_code")),
            @AttributeOverride(name = "display", column = @Column(name = "cause_display")),
            @AttributeOverride(name = "system", column = @Column(name = "cause_system")),
            @AttributeOverride(name = "version", column = @Column(name = "cause_version"))
    })
    private Code cause;

    @JsonIgnore
    @Override
    public String getName() {
        return "PatientCharacteristicExpired";
    }

    @Override
    public void copy(BaseType other)
    {
        if (other instanceof PatientCharacteristicExpired)
        {
            PatientCharacteristicExpired pce = (PatientCharacteristicExpired) other;
            super.copy(pce);
            setExpiredDatetime(pce.getExpiredDatetime());
            setCause(pce.getCause());
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Cannot copy QDM types %s and %s", this.getClass().getName(), other.getClass().getName())
            );
        }
    }
}
