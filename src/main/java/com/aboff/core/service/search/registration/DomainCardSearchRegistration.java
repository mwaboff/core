package com.aboff.core.service.search.registration;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.dh.DomainCard;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.dh.DomainCardRepository;
import com.aboff.core.service.dh.DomainCardService;
import com.aboff.core.service.search.SearchTypeRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Search registration for {@link DomainCard}. See {@link SearchTypeRegistration} for why this
 * exists.
 */
@Component
@RequiredArgsConstructor
public class DomainCardSearchRegistration implements SearchTypeRegistration {

    private final DomainCardRepository domainCardRepository;
    private final DomainCardService domainCardService;

    @Override
    public SearchableEntityType type() {
        return SearchableEntityType.DOMAIN_CARD;
    }

    @Override
    public JpaRepository<? extends BaseEntity, Long> repository() {
        return domainCardRepository;
    }

    @Override
    public Object resolveEntity(Long id, String expand, Authentication auth) {
        return domainCardService.getDomainCardById(id, expand);
    }
}
