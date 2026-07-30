package com.aboff.core.service.search.registration;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.service.dh.ExpansionService;
import com.aboff.core.service.search.SearchTypeRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Search registration for {@link Expansion}. See {@link SearchTypeRegistration} for why this
 * exists.
 */
@Component
@RequiredArgsConstructor
public class ExpansionSearchRegistration implements SearchTypeRegistration {

    private final ExpansionRepository expansionRepository;
    private final ExpansionService expansionService;

    @Override
    public SearchableEntityType type() {
        return SearchableEntityType.EXPANSION;
    }

    @Override
    public JpaRepository<? extends BaseEntity, Long> repository() {
        return expansionRepository;
    }

    @Override
    public Object resolveEntity(Long id, String expand, Authentication auth) {
        return expansionService.getExpansionById(id);
    }
}
