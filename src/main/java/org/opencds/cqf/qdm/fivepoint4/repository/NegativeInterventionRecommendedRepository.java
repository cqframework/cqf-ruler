package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.NegativeInterventionRecommended;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface NegativeInterventionRecommendedRepository extends JpaRepository<NegativeInterventionRecommended, String>
{
    @Nonnull
    Optional<NegativeInterventionRecommended> findBySystemId(@Nonnull String id);
}
