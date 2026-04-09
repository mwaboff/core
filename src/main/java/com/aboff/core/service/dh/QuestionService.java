package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateQuestionRequest;
import com.aboff.core.model.dto.dh.request.QuestionInput;
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

import com.aboff.core.event.EntityChangeEvent;
import com.aboff.core.util.ExpandUtil;
import org.springframework.context.ApplicationEventPublisher;

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
    private final ApplicationEventPublisher eventPublisher;

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
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedQuestion, EntityChangeEvent.ChangeType.CREATED));

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
        savedQuestions.forEach(q -> eventPublisher.publishEvent(new EntityChangeEvent(this, q, EntityChangeEvent.ChangeType.CREATED)));

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
        eventPublisher.publishEvent(new EntityChangeEvent(this, updatedQuestion, EntityChangeEvent.ChangeType.UPDATED));

        return toResponse(updatedQuestion, Set.of());
    }

    @Transactional
    public void deleteQuestion(Long id) {
        log.info("Soft deleting question with id: {}", id);

        Question question = questionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + id));

        question.softDelete();
        questionRepository.save(question);
        eventPublisher.publishEvent(new EntityChangeEvent(this, question, EntityChangeEvent.ChangeType.SOFT_DELETED));

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
        eventPublisher.publishEvent(new EntityChangeEvent(this, restoredQuestion, EntityChangeEvent.ChangeType.RESTORED));

        log.info("Restored question with id: {}", id);

        return toResponse(restoredQuestion, Set.of());
    }

    /**
     * Finds an existing question by text, expansion, and type (case-insensitive) or creates a new one.
     *
     * @param input the question input containing text, type, and expansion ID
     * @return the found or newly created question
     */
    @Transactional
    public Question findOrCreate(QuestionInput input) {
        if (input.getQuestionText() != null && !input.getQuestionText().isBlank()) {
            return questionRepository
                    .findByQuestionTextIgnoreCaseAndExpansionIdAndQuestionTypeAndDeletedAtIsNull(
                            input.getQuestionText(), input.getExpansionId(), input.getQuestionType())
                    .map(existing -> {
                        log.debug("Found existing question with text '{}' (id: {})", input.getQuestionText(), existing.getId());
                        return existing;
                    })
                    .orElseGet(() -> createQuestionFromInput(input));
        }
        return createQuestionFromInput(input);
    }

    /**
     * Creates a new question from the given input.
     *
     * @param input the question input
     * @return the newly created question
     */
    private Question createQuestionFromInput(QuestionInput input) {
        log.info("Creating new question with type '{}', expansion '{}'", input.getQuestionType(), input.getExpansionId());
        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(input.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + input.getExpansionId()));
        Question question = Question.builder()
                .questionText(input.getQuestionText())
                .questionType(input.getQuestionType())
                .expansion(expansion)
                .build();
        return questionRepository.save(question);
    }

    /**
     * Resolves questions from both ID-based and input-based sources, merging the results.
     * Returns null when both inputs are null (signaling no modification).
     * Returns an empty set when signaling a clear operation.
     *
     * @param questionIds list of existing question IDs to include
     * @param questions   list of question inputs to find or create
     * @return the resolved set of questions, or null if both inputs are null
     */
    @Transactional
    public Set<Question> resolveQuestions(List<Long> questionIds, List<QuestionInput> questions) {
        if (questionIds == null && questions == null) {
            return null;
        }
        Set<Question> resolved = new HashSet<>();
        if (questionIds != null && !questionIds.isEmpty()) {
            resolved.addAll(questionRepository.findAllByIdInAndDeletedAtIsNull(questionIds));
        }
        if (questions != null && !questions.isEmpty()) {
            for (QuestionInput input : questions) {
                resolved.add(findOrCreate(input));
            }
        }
        return resolved;
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

        if (ExpandUtil.shouldExpand(expand, "expansion")) {
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
