package com.aboff.core.service.search.registration;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.dh.DomainRepository;
import com.aboff.core.service.dh.DomainService;
import com.aboff.core.service.search.SearchTypeRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Search registration for {@link Domain}. See {@link SearchTypeRegistration} for why this exists.
 */
@Component
@RequiredArgsConstructor
public class DomainSearchRegistration implements SearchTypeRegistration {

    private final DomainRepository domainRepository;
    private final DomainService domainService;

    @Override
    public SearchableEntityType type() {
        return SearchableEntityType.DOMAIN;
    }

    @Override
    public JpaRepository<? extends BaseEntity, Long> repository() {
        return domainRepository;
    }

    @Override
    public Object resolveEntity(Long id, String expand, Authentication auth) {
        return domainService.getDomainById(id, expand);
    }
}
