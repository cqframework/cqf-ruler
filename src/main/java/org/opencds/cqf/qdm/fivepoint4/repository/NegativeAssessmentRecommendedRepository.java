package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.NegativeAssessmentRecommended;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface NegativeAssessmentRecommendedRepository extends JpaRepository<NegativeAssessmentRecommended, String>
{
    @Nonnull
    Optional<NegativeAssessmentRecommended> findBySystemId(@Nonnull String id);
}
