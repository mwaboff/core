package com.aboff.core.service.search.registration;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.dh.Environment;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.dh.EnvironmentRepository;
import com.aboff.core.service.dh.EnvironmentService;
import com.aboff.core.service.search.SearchTypeRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Search registration for {@link Environment}. See {@link SearchTypeRegistration} for why this
 * exists.
 */
@Component
@RequiredArgsConstructor
public class EnvironmentSearchRegistration implements SearchTypeRegistration {

    private final EnvironmentRepository environmentRepository;
    private final EnvironmentService environmentService;

    @Override
    public SearchableEntityType type() {
        return SearchableEntityType.ENVIRONMENT;
    }

    @Override
    public JpaRepository<? extends BaseEntity, Long> repository() {
        return environmentRepository;
    }

    @Override
    public Object resolveEntity(Long id, String expand, Authentication auth) {
        return environmentService.getEnvironmentById(id, expand, auth);
    }
}
