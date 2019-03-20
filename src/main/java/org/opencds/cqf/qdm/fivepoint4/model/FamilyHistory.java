package org.opencds.cqf.qdm.fivepoint4.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FamilyHistory extends BaseType implements Serializable
{
    private String authorDatetime;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "relationship_source_code")),
            @AttributeOverride(name = "display", column = @Column(name = "relationship_source_display")),
            @AttributeOverride(name = "system", column = @Column(name = "relationship_source_system")),
            @AttributeOverride(name = "version", column = @Column(name = "relationship_source_version"))
    })
    private Code relationship;

    @JsonIgnore
    @Override
    public String getName() {
        return "FamilyHistory";
    }

    @Override
    public void copy(BaseType other)
    {
        if (other instanceof FamilyHistory)
        {
            FamilyHistory familyHistory = (FamilyHistory) other;
            super.copy(familyHistory);
            setAuthorDatetime(familyHistory.getAuthorDatetime());
            setRelationship(familyHistory.getRelationship());
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Cannot copy QDM types %s and %s", this.getClass().getName(), other.getClass().getName())
            );
        }
    }
}
