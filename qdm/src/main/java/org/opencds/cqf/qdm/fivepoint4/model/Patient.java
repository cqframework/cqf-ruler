package org.opencds.cqf.qdm.fivepoint4.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.PrePersist;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Patient implements Serializable
{
    @javax.persistence.Id
    @JsonIgnore
    @Column(name = "system_id")
    private String systemId;

    @NotNull
    @Column(nullable = false)
    private String resourceType;

    @Embedded
    private Id id;

    private String birthDatetime;

    @PrePersist
    public void ensureId()
    {
        // Making sure the Id.value and internal systemId elements are set properly
        if (systemId == null && id == null)
        {
            systemId = UUID.randomUUID().toString();
            id = new Id(systemId, null);
        }
        else if (id == null)
        {
            id = new Id(systemId, null);
        }
        else if (id.getValue() != null)
        {
            systemId = id.getValue();
        }
        else
        {
            systemId = UUID.randomUUID().toString();
            id.setValue(systemId);
        }
    }

    @JsonIgnore
    public String getName() {
        return "Patient";
    }

    public void copy(Patient other)
    {
        setBirthDatetime(other.getBirthDatetime());
    }
}
