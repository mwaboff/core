package com.aboff.core.service.search.registration;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.dh.Weapon;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.dh.WeaponRepository;
import com.aboff.core.service.dh.WeaponService;
import com.aboff.core.service.search.SearchTypeRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Search registration for {@link Weapon}. See {@link SearchTypeRegistration} for why this
 * exists.
 */
@Component
@RequiredArgsConstructor
public class WeaponSearchRegistration implements SearchTypeRegistration {

    private final WeaponRepository weaponRepository;
    private final WeaponService weaponService;

    @Override
    public SearchableEntityType type() {
        return SearchableEntityType.WEAPON;
    }

    @Override
    public JpaRepository<? extends BaseEntity, Long> repository() {
        return weaponRepository;
    }

    @Override
    public Object resolveEntity(Long id, String expand, Authentication auth) {
        return weaponService.getWeaponById(id, expand);
    }
}
