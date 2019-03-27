package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PositiveCommunicationPerformed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PositiveCommunicationPerformedRepository extends JpaRepository<PositiveCommunicationPerformed, String>
{
    @Nonnull
    Optional<PositiveCommunicationPerformed> findBySystemId(@Nonnull String id);
}
