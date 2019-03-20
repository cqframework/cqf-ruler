package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.NegativeDiagnosticStudyOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface NegativeDiagnosticStudyOrderRepository extends JpaRepository<NegativeDiagnosticStudyOrder, String>
{
    @Nonnull
    Optional<NegativeDiagnosticStudyOrder> findBySystemId(@Nonnull String id);
}
