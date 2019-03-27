package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.ProviderCareExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface ProviderCareExperienceRepository extends JpaRepository<ProviderCareExperience, String>
{
    @Nonnull
    Optional<ProviderCareExperience> findBySystemId(@Nonnull String id);
}
