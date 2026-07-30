package com.aboff.core.service.search.registration;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.dh.Loot;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.dh.LootRepository;
import com.aboff.core.service.dh.LootService;
import com.aboff.core.service.search.SearchTypeRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Search registration for {@link Loot}. See {@link SearchTypeRegistration} for why this exists.
 */
@Component
@RequiredArgsConstructor
public class LootSearchRegistration implements SearchTypeRegistration {

    private final LootRepository lootRepository;
    private final LootService lootService;

    @Override
    public SearchableEntityType type() {
        return SearchableEntityType.LOOT;
    }

    @Override
    public JpaRepository<? extends BaseEntity, Long> repository() {
        return lootRepository;
    }

    @Override
    public Object resolveEntity(Long id, String expand, Authentication auth) {
        return lootService.getLootById(id, expand);
    }
}
