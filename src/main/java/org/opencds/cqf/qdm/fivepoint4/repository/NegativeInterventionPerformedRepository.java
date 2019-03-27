package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.NegativeInterventionPerformed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface NegativeInterventionPerformedRepository extends JpaRepository<NegativeInterventionPerformed, String>
{
    @Nonnull
    Optional<NegativeInterventionPerformed> findBySystemId(@Nonnull String id);
}
