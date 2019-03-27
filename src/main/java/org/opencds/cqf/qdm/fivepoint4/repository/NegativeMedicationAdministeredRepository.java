package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.NegativeMedicationAdministered;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface NegativeMedicationAdministeredRepository extends JpaRepository<NegativeMedicationAdministered, String>
{
    @Nonnull
    Optional<NegativeMedicationAdministered> findBySystemId(@Nonnull String id);
}
