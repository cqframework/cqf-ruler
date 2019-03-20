package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PositiveDiagnosticStudyPerformed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PositiveDiagnosticStudyPerformedRepository extends JpaRepository<PositiveDiagnosticStudyPerformed, String>
{
    @Nonnull
    Optional<PositiveDiagnosticStudyPerformed> findBySystemId(@Nonnull String id);
}
