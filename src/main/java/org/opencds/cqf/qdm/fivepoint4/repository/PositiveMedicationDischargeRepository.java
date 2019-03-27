package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PositiveMedicationDischarge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PositiveMedicationDischargeRepository extends JpaRepository<PositiveMedicationDischarge, String>
{
    @Nonnull
    Optional<PositiveMedicationDischarge> findBySystemId(@Nonnull String id);
}
