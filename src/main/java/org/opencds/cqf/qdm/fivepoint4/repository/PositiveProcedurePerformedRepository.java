package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PositiveProcedurePerformed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PositiveProcedurePerformedRepository extends JpaRepository<PositiveProcedurePerformed, String>
{
    @Nonnull
    Optional<PositiveProcedurePerformed> findBySystemId(@Nonnull String id);
}
