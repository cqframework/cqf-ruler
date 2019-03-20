package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.NegativeEncounterPerformed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface NegativeEncounterPerformedRepository extends JpaRepository<NegativeEncounterPerformed, String>
{
    @Nonnull
    Optional<NegativeEncounterPerformed> findBySystemId(@Nonnull String id);
}
