package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PositiveSubstanceRecommended;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PositiveSubstanceRecommendedRepository extends JpaRepository<PositiveSubstanceRecommended, String>
{
    @Nonnull
    Optional<PositiveSubstanceRecommended> findBySystemId(@Nonnull String id);
}
