package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PositiveSubstanceOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PositiveSubstanceOrderRepository extends JpaRepository<PositiveSubstanceOrder, String>
{
    @Nonnull
    Optional<PositiveSubstanceOrder> findBySystemId(@Nonnull String id);
}
