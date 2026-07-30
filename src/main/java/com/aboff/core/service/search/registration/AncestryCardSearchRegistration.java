package com.aboff.core.service.search.registration;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.dh.AncestryCard;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.dh.AncestryCardRepository;
import com.aboff.core.service.dh.AncestryCardService;
import com.aboff.core.service.search.SearchTypeRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Search registration for {@link AncestryCard}. See {@link SearchTypeRegistration} for why this
 * exists.
 */
@Component
@RequiredArgsConstructor
public class AncestryCardSearchRegistration implements SearchTypeRegistration {

    private final AncestryCardRepository ancestryCardRepository;
    private final AncestryCardService ancestryCardService;

    @Override
    public SearchableEntityType type() {
        return SearchableEntityType.ANCESTRY_CARD;
    }

    @Override
    public JpaRepository<? extends BaseEntity, Long> repository() {
        return ancestryCardRepository;
    }

    @Override
    public Object resolveEntity(Long id, String expand, Authentication auth) {
        return ancestryCardService.getAncestryCardById(id, expand);
    }
}
