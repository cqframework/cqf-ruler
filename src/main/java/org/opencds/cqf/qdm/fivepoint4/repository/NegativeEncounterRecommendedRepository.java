package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.NegativeEncounterRecommended;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface NegativeEncounterRecommendedRepository extends JpaRepository<NegativeEncounterRecommended, String>
{
    @Nonnull
    Optional<NegativeEncounterRecommended> findBySystemId(@Nonnull String id);
}
