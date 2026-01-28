package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateQuestionRequest;
import com.aboff.core.model.dto.dh.request.UpdateQuestionRequest;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.dh.response.QuestionResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Question;
import com.aboff.core.model.enums.QuestionType;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.QuestionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aboff.core.util.ExpandUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing Question entities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final ExpansionRepository expansionRepository;

    @Transactional(readOnly = true)
    public PagedResponse<QuestionResponse> getAllQuestions(
            int page,
            int size,
            boolean includeDeleted,
            Long expansionId,
            QuestionType questionType,
            String expand) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Question> questionPage;

        if (includeDeleted) {
            questionPage = questionRepository.findAllWithFilters(expansionId, questionType, pageable);
        } else {
            questionPage = questionRepository.findByDeletedAtIsNullAndFilters(expansionId, questionType, pageable);
        }

        Set<String> expandSet = ExpandUtil.parseExpand(expand);

        return PagedResponse.<QuestionResponse>builder()
                .content(questionPage.getContent().stream()
                        .map(question -> toResponse(question, expandSet))
                        .toList())
                .totalElements(questionPage.getTotalElements())
                .totalPages(questionPage.getTotalPages())
                .currentPage(questionPage.getNumber())
                .pageSize(questionPage.getSize())
                .build();
    }

    @Transactional(readOnly = true)
    public QuestionResponse getQuestionById(Long id, String expand) {
        Question question = questionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + id));

        Set<String> expandSet = ExpandUtil.parseExpand(expand);
        return toResponse(question, expandSet);
    }

    @Transactional
    public QuestionResponse createQuestion(CreateQuestionRequest request) {
        log.info("Creating new question");

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        Question question = Question.builder()
                .questionText(request.getQuestionText())
                .questionType(request.getQuestionType())
                .expansion(expansion)
                .build();

        Question savedQuestion = questionRepository.save(question);
        log.info("Created question with id: {}", savedQuestion.getId());

        return toResponse(savedQuestion, Set.of());
    }

    @Transactional
    public List<QuestionResponse> createQuestionsBulk(List<CreateQuestionRequest> requests) {
        log.info("Creating {} questions in bulk", requests.size());

        List<Question> questions = requests.stream()
                .map(request -> {
                    Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Expansion not found with id: " + request.getExpansionId()));

                    return Question.builder()
                            .questionText(request.getQuestionText())
                            .questionType(request.getQuestionType())
                            .expansion(expansion)
                            .build();
                })
                .collect(Collectors.toList());

        List<Question> savedQuestions = questionRepository.saveAll(questions);
        log.info("Created {} questions in bulk", savedQuestions.size());

        return savedQuestions.stream()
                .map(question -> toResponse(question, Set.of()))
                .toList();
    }

    @Transactional
    public QuestionResponse updateQuestion(Long id, UpdateQuestionRequest request) {
        log.info("Updating question with id: {}", id);

        Question question = questionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + id));

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        question.setQuestionText(request.getQuestionText());
        question.setQuestionType(request.getQuestionType());
        question.setExpansion(expansion);

        Question updatedQuestion = questionRepository.save(question);
        log.info("Updated question with id: {}", updatedQuestion.getId());

        return toResponse(updatedQuestion, Set.of());
    }

    @Transactional
    public void deleteQuestion(Long id) {
        log.info("Soft deleting question with id: {}", id);

        Question question = questionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + id));

        question.softDelete();
        questionRepository.save(question);

        log.info("Soft deleted question with id: {}", id);
    }

    @Transactional
    public QuestionResponse restoreQuestion(Long id) {
        log.info("Restoring question with id: {}", id);

        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + id));

        if (!question.isDeleted()) {
            throw new IllegalStateException("Question with id " + id + " is not deleted");
        }

        question.restore();
        Question restoredQuestion = questionRepository.save(question);

        log.info("Restored question with id: {}", id);

        return toResponse(restoredQuestion, Set.of());
    }

    private QuestionResponse toResponse(Question question, Set<String> expand) {
        QuestionResponse.QuestionResponseBuilder builder = QuestionResponse.builder()
                .id(question.getId())
                .questionText(question.getQuestionText())
                .questionType(question.getQuestionType())
                .expansionId(question.getExpansion().getId())
                .createdAt(question.getCreatedAt())
                .lastModifiedAt(question.getLastModifiedAt())
                .deletedAt(question.getDeletedAt());

        if (expand.contains("expansion")) {
            Expansion expansion = question.getExpansion();
            builder.expansion(ExpansionResponse.builder()
                    .id(expansion.getId())
                    .name(expansion.getName())
                    .isPublished(expansion.getIsPublished())
                    .createdAt(expansion.getCreatedAt())
                    .lastModifiedAt(expansion.getLastModifiedAt())
                    .deletedAt(expansion.getDeletedAt())
                    .build());
        }

        return builder.build();
    }
}
