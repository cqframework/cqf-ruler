package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PositiveEncounterOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PositiveEncounterOrderRepository extends JpaRepository<PositiveEncounterOrder, String>
{
    @Nonnull
    Optional<PositiveEncounterOrder> findBySystemId(@Nonnull String id);
}
