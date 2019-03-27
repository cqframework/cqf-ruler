package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PositiveDiagnosticStudyOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PositiveDiagnosticStudyOrderRepository extends JpaRepository<PositiveDiagnosticStudyOrder, String>
{
    @Nonnull
    Optional<PositiveDiagnosticStudyOrder> findBySystemId(@Nonnull String id);
}
