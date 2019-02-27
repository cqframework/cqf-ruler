package org.opencds.cqf.qdm.fivepoint4.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.UUID;

@MappedSuperclass
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseType implements Serializable
{
    @javax.persistence.Id
    @JsonIgnore
    @Column(name = "system_id")
    private String systemId;

    @Embedded
    @NotNull
    @Column(nullable = false)
    private Id id;

    @Embedded
    @NotNull
    @Column(nullable = false)
    private Code code;

    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "patient_id_value")),
            @AttributeOverride(name = "namingSystem", column = @Column(name = "patient_id_naming_system"))
    })
    @NotNull
    @Column(nullable = false)
    private Id patientId;

    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "reporter_value")),
            @AttributeOverride(name = "namingSystem", column = @Column(name = "reporter_naming_system"))
    })
    private Id reporter;

    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "recorder_value")),
            @AttributeOverride(name = "namingSystem", column = @Column(name = "recorder_naming_system"))
    })
    private Id recorder;

    @PrePersist
    public void ensureId()
    {
        if (systemId == null && id == null)
        {
            systemId = UUID.randomUUID().toString();
            id = new Id(systemId, null);
        }
        else if (id == null)
        {
            id = new Id(systemId, null);
        }
        else {
            if (id.getValue() != null)
            {
                systemId = id.getValue();
            }
            else
            {
                systemId = UUID.randomUUID().toString();
                id.setValue(systemId);
            }
        }
    }
}
