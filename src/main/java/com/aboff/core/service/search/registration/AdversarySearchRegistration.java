package com.aboff.core.service.search.registration;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.dh.Adversary;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.dh.AdversaryRepository;
import com.aboff.core.service.dh.AdversaryService;
import com.aboff.core.service.search.SearchTypeRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Search registration for {@link Adversary}. See {@link SearchTypeRegistration} for why this
 * exists.
 */
@Component
@RequiredArgsConstructor
public class AdversarySearchRegistration implements SearchTypeRegistration {

    private final AdversaryRepository adversaryRepository;
    private final AdversaryService adversaryService;

    @Override
    public SearchableEntityType type() {
        return SearchableEntityType.ADVERSARY;
    }

    @Override
    public JpaRepository<? extends BaseEntity, Long> repository() {
        return adversaryRepository;
    }

    @Override
    public Object resolveEntity(Long id, String expand, Authentication auth) {
        return adversaryService.getAdversaryById(id, expand, auth);
    }
}
