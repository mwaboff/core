package com.aboff.core.service.search.registration;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.dh.CommunityCard;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.dh.CommunityCardRepository;
import com.aboff.core.service.dh.CommunityCardService;
import com.aboff.core.service.search.SearchTypeRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Search registration for {@link CommunityCard}. See {@link SearchTypeRegistration} for why this
 * exists.
 */
@Component
@RequiredArgsConstructor
public class CommunityCardSearchRegistration implements SearchTypeRegistration {

    private final CommunityCardRepository communityCardRepository;
    private final CommunityCardService communityCardService;

    @Override
    public SearchableEntityType type() {
        return SearchableEntityType.COMMUNITY_CARD;
    }

    @Override
    public JpaRepository<? extends BaseEntity, Long> repository() {
        return communityCardRepository;
    }

    @Override
    public Object resolveEntity(Long id, String expand, Authentication auth) {
        return communityCardService.getCommunityCardById(id, expand);
    }
}
