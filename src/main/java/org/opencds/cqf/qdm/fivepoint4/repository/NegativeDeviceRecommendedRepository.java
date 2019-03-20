package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.NegativeDeviceRecommended;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface NegativeDeviceRecommendedRepository extends JpaRepository<NegativeDeviceRecommended, String>
{
    @Nonnull
    Optional<NegativeDeviceRecommended> findBySystemId(@Nonnull String id);
}
