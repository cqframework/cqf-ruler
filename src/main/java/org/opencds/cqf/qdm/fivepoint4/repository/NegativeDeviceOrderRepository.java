package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.NegativeDeviceOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface NegativeDeviceOrderRepository extends JpaRepository<NegativeDeviceOrder, String>
{
    @Nonnull
    Optional<NegativeDeviceOrder> findBySystemId(@Nonnull String id);
}
