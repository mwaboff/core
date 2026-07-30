package com.aboff.core.service.search.registration;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.dh.MartialStance;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.dh.MartialStanceRepository;
import com.aboff.core.service.dh.MartialStanceService;
import com.aboff.core.service.search.SearchTypeRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Search registration for {@link MartialStance}. See {@link SearchTypeRegistration} for why this
 * exists.
 */
@Component
@RequiredArgsConstructor
public class MartialStanceSearchRegistration implements SearchTypeRegistration {

    private final MartialStanceRepository martialStanceRepository;
    private final MartialStanceService martialStanceService;

    @Override
    public SearchableEntityType type() {
        return SearchableEntityType.MARTIAL_STANCE;
    }

    @Override
    public JpaRepository<? extends BaseEntity, Long> repository() {
        return martialStanceRepository;
    }

    @Override
    public Object resolveEntity(Long id, String expand, Authentication auth) {
        return martialStanceService.getMartialStanceById(id, expand);
    }
}
