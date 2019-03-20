package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PositiveLaboratoryTestRecommended;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PositiveLaboratoryTestRecommendedRepository extends JpaRepository<PositiveLaboratoryTestRecommended, String>
{
    @Nonnull
    Optional<PositiveLaboratoryTestRecommended> findBySystemId(@Nonnull String id);
}
