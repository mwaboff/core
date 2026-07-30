package com.aboff.core.service.search.registration;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.dh.Armor;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.dh.ArmorRepository;
import com.aboff.core.service.dh.ArmorService;
import com.aboff.core.service.search.SearchTypeRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Search registration for {@link Armor}. See {@link SearchTypeRegistration} for why this exists.
 */
@Component
@RequiredArgsConstructor
public class ArmorSearchRegistration implements SearchTypeRegistration {

    private final ArmorRepository armorRepository;
    private final ArmorService armorService;

    @Override
    public SearchableEntityType type() {
        return SearchableEntityType.ARMOR;
    }

    @Override
    public JpaRepository<? extends BaseEntity, Long> repository() {
        return armorRepository;
    }

    @Override
    public Object resolveEntity(Long id, String expand, Authentication auth) {
        return armorService.getArmorById(id, expand);
    }
}
