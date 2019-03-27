package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PositiveInterventionOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PositiveInterventionOrderRepository extends JpaRepository<PositiveInterventionOrder, String>
{
    @Nonnull
    Optional<PositiveInterventionOrder> findBySystemId(@Nonnull String id);
}
