package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PositiveImmunizationOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PositiveImmunizationOrderRepository extends JpaRepository<PositiveImmunizationOrder, String>
{
    @Nonnull
    Optional<PositiveImmunizationOrder> findBySystemId(@Nonnull String id);
}
