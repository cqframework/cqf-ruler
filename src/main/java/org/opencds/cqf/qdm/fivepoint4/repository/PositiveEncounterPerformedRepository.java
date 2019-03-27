package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PositiveEncounterPerformed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PositiveEncounterPerformedRepository extends JpaRepository<PositiveEncounterPerformed, String>
{
    @Nonnull
    Optional<PositiveEncounterPerformed> findBySystemId(@Nonnull String id);
}
