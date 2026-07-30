package com.aboff.core.service.search.registration;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.dh.SubclassCard;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.dh.SubclassCardRepository;
import com.aboff.core.service.dh.SubclassCardService;
import com.aboff.core.service.search.SearchTypeRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Search registration for {@link SubclassCard}. See {@link SearchTypeRegistration} for why this
 * exists.
 */
@Component
@RequiredArgsConstructor
public class SubclassCardSearchRegistration implements SearchTypeRegistration {

    private final SubclassCardRepository subclassCardRepository;
    private final SubclassCardService subclassCardService;

    @Override
    public SearchableEntityType type() {
        return SearchableEntityType.SUBCLASS_CARD;
    }

    @Override
    public JpaRepository<? extends BaseEntity, Long> repository() {
        return subclassCardRepository;
    }

    @Override
    public Object resolveEntity(Long id, String expand, Authentication auth) {
        return subclassCardService.getSubclassCardById(id, expand);
    }
}
