package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.NegativeDiagnosticStudyRecommended;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface NegativeDiagnosticStudyRecommendedRepository extends JpaRepository<NegativeDiagnosticStudyRecommended, String>
{
    @Nonnull
    Optional<NegativeDiagnosticStudyRecommended> findBySystemId(@Nonnull String id);
}
