package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.NegativeAssessmentPerformed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface NegativeAssessmentPerformedRepository extends JpaRepository<NegativeAssessmentPerformed, String>
{
    @Nonnull
    Optional<NegativeAssessmentPerformed> findBySystemId(@Nonnull String id);
}
