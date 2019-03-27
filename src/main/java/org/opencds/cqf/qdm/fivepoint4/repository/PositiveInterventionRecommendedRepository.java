package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PositiveInterventionRecommended;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PositiveInterventionRecommendedRepository extends JpaRepository<PositiveInterventionRecommended, String>
{
    @Nonnull
    Optional<PositiveInterventionRecommended> findBySystemId(@Nonnull String id);
}
