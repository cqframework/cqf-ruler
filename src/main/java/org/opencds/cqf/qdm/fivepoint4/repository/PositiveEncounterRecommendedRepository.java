package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PositiveEncounterRecommended;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PositiveEncounterRecommendedRepository extends JpaRepository<PositiveEncounterRecommended, String>
{
    @Nonnull
    Optional<PositiveEncounterRecommended> findBySystemId(@Nonnull String id);
}
