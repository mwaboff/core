package com.aboff.core.service.search.registration;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.dh.Beastform;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.dh.BeastformRepository;
import com.aboff.core.service.dh.BeastformService;
import com.aboff.core.service.search.SearchTypeRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Search registration for {@link Beastform}.
 *
 * <p>This is the exact type whose stale {@code case BEASTFORM -> null;} arm (fixed in PR #55,
 * and re-introduced by three of five in-flight branches on 2026-07-30 via stale rebases) is the
 * motivating incident for this whole registry — see {@link SearchTypeRegistration}'s javadoc.
 */
@Component
@RequiredArgsConstructor
public class BeastformSearchRegistration implements SearchTypeRegistration {

    private final BeastformRepository beastformRepository;
    private final BeastformService beastformService;

    @Override
    public SearchableEntityType type() {
        return SearchableEntityType.BEASTFORM;
    }

    @Override
    public JpaRepository<? extends BaseEntity, Long> repository() {
        return beastformRepository;
    }

    @Override
    public Object resolveEntity(Long id, String expand, Authentication auth) {
        return beastformService.getBeastformById(id, expand);
    }
}
