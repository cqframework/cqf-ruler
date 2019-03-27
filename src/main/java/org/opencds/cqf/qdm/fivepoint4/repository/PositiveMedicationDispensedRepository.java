package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PositiveMedicationDispensed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PositiveMedicationDispensedRepository extends JpaRepository<PositiveMedicationDispensed, String>
{
    @Nonnull
    Optional<PositiveMedicationDispensed> findBySystemId(@Nonnull String id);
}
