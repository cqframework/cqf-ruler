package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PositiveMedicationOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PositiveMedicationOrderRepository extends JpaRepository<PositiveMedicationOrder, String>
{
    @Nonnull
    Optional<PositiveMedicationOrder> findBySystemId(@Nonnull String id);
}
