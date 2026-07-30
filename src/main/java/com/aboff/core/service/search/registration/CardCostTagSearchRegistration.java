package com.aboff.core.service.search.registration;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.dh.CardCostTagRepository;
import com.aboff.core.service.dh.CardCostTagService;
import com.aboff.core.service.search.SearchTypeRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Search registration for {@link CardCostTag}. See {@link SearchTypeRegistration} for why this
 * exists.
 */
@Component
@RequiredArgsConstructor
public class CardCostTagSearchRegistration implements SearchTypeRegistration {

    private final CardCostTagRepository cardCostTagRepository;
    private final CardCostTagService cardCostTagService;

    @Override
    public SearchableEntityType type() {
        return SearchableEntityType.CARD_COST_TAG;
    }

    @Override
    public JpaRepository<? extends BaseEntity, Long> repository() {
        return cardCostTagRepository;
    }

    @Override
    public Object resolveEntity(Long id, String expand, Authentication auth) {
        return cardCostTagService.getCostTagById(id);
    }
}
