package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.NegativeDiagnosticStudyPerformed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface NegativeDiagnosticStudyPerformedRepository extends JpaRepository<NegativeDiagnosticStudyPerformed, String>
{
    @Nonnull
    Optional<NegativeDiagnosticStudyPerformed> findBySystemId(@Nonnull String id);
}
