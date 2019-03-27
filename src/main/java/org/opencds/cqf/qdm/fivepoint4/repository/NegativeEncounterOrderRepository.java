package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.NegativeEncounterOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface NegativeEncounterOrderRepository extends JpaRepository<NegativeEncounterOrder, String>
{
    @Nonnull
    Optional<NegativeEncounterOrder> findBySystemId(@Nonnull String id);
}
