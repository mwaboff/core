package com.aboff.core.service.search.registration;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.dh.Question;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.dh.QuestionRepository;
import com.aboff.core.service.dh.QuestionService;
import com.aboff.core.service.search.SearchTypeRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Search registration for {@link Question}. See {@link SearchTypeRegistration} for why this
 * exists.
 */
@Component
@RequiredArgsConstructor
public class QuestionSearchRegistration implements SearchTypeRegistration {

    private final QuestionRepository questionRepository;
    private final QuestionService questionService;

    @Override
    public SearchableEntityType type() {
        return SearchableEntityType.QUESTION;
    }

    @Override
    public JpaRepository<? extends BaseEntity, Long> repository() {
        return questionRepository;
    }

    @Override
    public Object resolveEntity(Long id, String expand, Authentication auth) {
        return questionService.getQuestionById(id, expand);
    }
}
