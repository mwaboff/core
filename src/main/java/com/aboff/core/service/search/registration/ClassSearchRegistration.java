package com.aboff.core.service.search.registration;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.dh.ClassRepository;
import com.aboff.core.service.dh.ClassService;
import com.aboff.core.service.search.SearchTypeRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Search registration for {@link com.aboff.core.model.entity.dh.Class}. See
 * {@link SearchTypeRegistration} for why this exists.
 */
@Component
@RequiredArgsConstructor
public class ClassSearchRegistration implements SearchTypeRegistration {

    private final ClassRepository classRepository;
    private final ClassService classService;

    @Override
    public SearchableEntityType type() {
        return SearchableEntityType.CLASS;
    }

    @Override
    public JpaRepository<? extends BaseEntity, Long> repository() {
        return classRepository;
    }

    @Override
    public Object resolveEntity(Long id, String expand, Authentication auth) {
        return classService.getClassById(id, expand);
    }
}
