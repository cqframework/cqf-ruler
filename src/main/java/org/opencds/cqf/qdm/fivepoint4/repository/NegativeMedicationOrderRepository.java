package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.NegativeMedicationOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface NegativeMedicationOrderRepository extends JpaRepository<NegativeMedicationOrder, String>
{
    @Nonnull
    Optional<NegativeMedicationOrder> findBySystemId(@Nonnull String id);
}
