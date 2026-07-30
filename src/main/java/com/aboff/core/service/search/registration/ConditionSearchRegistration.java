package com.aboff.core.service.search.registration;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.dh.Condition;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.dh.ConditionRepository;
import com.aboff.core.service.dh.ConditionService;
import com.aboff.core.service.search.SearchTypeRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Search registration for {@link Condition}. See {@link SearchTypeRegistration} for why this
 * exists.
 */
@Component
@RequiredArgsConstructor
public class ConditionSearchRegistration implements SearchTypeRegistration {

    private final ConditionRepository conditionRepository;
    private final ConditionService conditionService;

    @Override
    public SearchableEntityType type() {
        return SearchableEntityType.CONDITION;
    }

    @Override
    public JpaRepository<? extends BaseEntity, Long> repository() {
        return conditionRepository;
    }

    @Override
    public Object resolveEntity(Long id, String expand, Authentication auth) {
        return conditionService.getConditionById(id, expand);
    }
}
