package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PositiveProcedureOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PositiveProcedureOrderRepository extends JpaRepository<PositiveProcedureOrder, String>
{
    @Nonnull
    Optional<PositiveProcedureOrder> findBySystemId(@Nonnull String id);
}
