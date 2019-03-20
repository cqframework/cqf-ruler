package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PositiveInterventionPerformed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PositiveInterventionPerformedRepository extends JpaRepository<PositiveInterventionPerformed, String>
{
    @Nonnull
    Optional<PositiveInterventionPerformed> findBySystemId(@Nonnull String id);
}
