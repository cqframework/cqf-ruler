package org.opencds.cqf.qdm.fivepoint4.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@MappedSuperclass
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class CommunicationPerformed extends BaseType implements Serializable
{
    private String authorDatetime;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "category_code")),
            @AttributeOverride(name = "display", column = @Column(name = "category_display")),
            @AttributeOverride(name = "system", column = @Column(name = "category_system")),
            @AttributeOverride(name = "version", column = @Column(name = "category_version"))
    })
    private Code category;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "medium_code")),
            @AttributeOverride(name = "display", column = @Column(name = "medium_display")),
            @AttributeOverride(name = "system", column = @Column(name = "medium_system")),
            @AttributeOverride(name = "version", column = @Column(name = "medium_version"))
    })
    private Code medium;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "sender_code")),
            @AttributeOverride(name = "display", column = @Column(name = "sender_display")),
            @AttributeOverride(name = "system", column = @Column(name = "sender_system")),
            @AttributeOverride(name = "version", column = @Column(name = "sender_version"))
    })
    private Code sender;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "recipient_code")),
            @AttributeOverride(name = "display", column = @Column(name = "recipient_display")),
            @AttributeOverride(name = "system", column = @Column(name = "recipient_system")),
            @AttributeOverride(name = "version", column = @Column(name = "recipient_version"))
    })
    private Code recipient;

    @ElementCollection
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<Id> relatedTo;

    @AttributeOverrides({
            @AttributeOverride(name = "start", column = @Column(name = "relevant_period_start")),
            @AttributeOverride(name = "end", column = @Column(name = "relevant_period_end"))
    })
    private DateTimeInterval relevantPeriod;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "negation_rationale_code")),
            @AttributeOverride(name = "display", column = @Column(name = "negation_rationale_display")),
            @AttributeOverride(name = "system", column = @Column(name = "negation_rationale_system")),
            @AttributeOverride(name = "version", column = @Column(name = "negation_rationale_version"))
    })
    private Code negationRationale;

    @Override
    public void copy(BaseType other)
    {
        if (other instanceof CommunicationPerformed)
        {
            CommunicationPerformed communicationPerformed = (CommunicationPerformed) other;
            super.copy(communicationPerformed);
            setAuthorDatetime(communicationPerformed.getAuthorDatetime());
            setCategory(communicationPerformed.getCategory());
            setMedium(communicationPerformed.getMedium());
            setSender(communicationPerformed.getSender());
            setRecipient(communicationPerformed.getRecipient());
            setRelatedTo(communicationPerformed.getRelatedTo());
            setRelevantPeriod(communicationPerformed.getRelevantPeriod());
            setNegationRationale(communicationPerformed.getNegationRationale());
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Cannot copy QDM types %s and %s", this.getClass().getName(), other.getClass().getName())
            );
        }
    }
}
