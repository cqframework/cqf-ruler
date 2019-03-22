package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.BaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface BaseRepository<T extends BaseType> extends JpaRepository<T, String>
{
    @Nonnull
    Optional<T> findBySystemId(@Nonnull String id);

    @Nonnull
    Optional<List<T>> findByPatientIdValue(@Nonnull String value);
}
