package com.aboff.core.service.search.registration;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.dh.Encounter;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.dh.EncounterRepository;
import com.aboff.core.service.dh.EncounterService;
import com.aboff.core.service.search.SearchTypeRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Search registration for {@link Encounter}. See {@link SearchTypeRegistration} for why this
 * exists.
 */
@Component
@RequiredArgsConstructor
public class EncounterSearchRegistration implements SearchTypeRegistration {

    private final EncounterRepository encounterRepository;
    private final EncounterService encounterService;

    @Override
    public SearchableEntityType type() {
        return SearchableEntityType.ENCOUNTER;
    }

    @Override
    public JpaRepository<? extends BaseEntity, Long> repository() {
        return encounterRepository;
    }

    @Override
    public Object resolveEntity(Long id, String expand, Authentication auth) {
        return encounterService.getEncounterById(id, expand, auth);
    }
}
