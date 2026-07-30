package com.aboff.core.service.search.registration;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.dh.SubclassPath;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.dh.SubclassPathRepository;
import com.aboff.core.service.dh.SubclassPathService;
import com.aboff.core.service.search.SearchTypeRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Search registration for {@link SubclassPath}. See {@link SearchTypeRegistration} for why this
 * exists.
 */
@Component
@RequiredArgsConstructor
public class SubclassPathSearchRegistration implements SearchTypeRegistration {

    private final SubclassPathRepository subclassPathRepository;
    private final SubclassPathService subclassPathService;

    @Override
    public SearchableEntityType type() {
        return SearchableEntityType.SUBCLASS_PATH;
    }

    @Override
    public JpaRepository<? extends BaseEntity, Long> repository() {
        return subclassPathRepository;
    }

    @Override
    public Object resolveEntity(Long id, String expand, Authentication auth) {
        return subclassPathService.getSubclassPathById(id, expand);
    }
}
