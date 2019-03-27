package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.NegativeImmunizationOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface NegativeImmunizationOrderRepository extends JpaRepository<NegativeImmunizationOrder, String>
{
    @Nonnull
    Optional<NegativeImmunizationOrder> findBySystemId(@Nonnull String id);
}
