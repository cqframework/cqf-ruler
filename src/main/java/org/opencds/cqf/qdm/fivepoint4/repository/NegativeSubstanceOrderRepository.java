package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.NegativeSubstanceOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface NegativeSubstanceOrderRepository extends JpaRepository<NegativeSubstanceOrder, String>
{
    @Nonnull
    Optional<NegativeSubstanceOrder> findBySystemId(@Nonnull String id);
}
