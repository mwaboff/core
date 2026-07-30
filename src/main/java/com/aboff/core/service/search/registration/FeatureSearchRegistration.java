package com.aboff.core.service.search.registration;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.dh.FeatureRepository;
import com.aboff.core.service.dh.FeatureService;
import com.aboff.core.service.search.SearchTypeRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Search registration for {@link Feature}. See {@link SearchTypeRegistration} for why this
 * exists.
 */
@Component
@RequiredArgsConstructor
public class FeatureSearchRegistration implements SearchTypeRegistration {

    private final FeatureRepository featureRepository;
    private final FeatureService featureService;

    @Override
    public SearchableEntityType type() {
        return SearchableEntityType.FEATURE;
    }

    @Override
    public JpaRepository<? extends BaseEntity, Long> repository() {
        return featureRepository;
    }

    @Override
    public Object resolveEntity(Long id, String expand, Authentication auth) {
        return featureService.getFeatureById(id, expand);
    }
}
