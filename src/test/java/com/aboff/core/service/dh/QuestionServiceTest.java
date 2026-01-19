package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateQuestionRequest;
import com.aboff.core.model.dto.dh.request.UpdateQuestionRequest;
import com.aboff.core.model.dto.dh.response.QuestionResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Question;
import com.aboff.core.model.enums.QuestionType;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.QuestionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for QuestionService.
 * Tests all CRUD operations, pagination, soft deletion, restore functionality, expand parameter, bulk operations, and filtering.
 */
@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private ExpansionRepository expansionRepository;

    @InjectMocks
    private QuestionService questionService;

    // ==================== GET ALL QUESTIONS TESTS ====================

    @Test
    void getAllQuestions_WithoutFilters_ReturnsPagedQuestions() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Question question1 = Question.builder()
                .id(1L)
                .questionText("What drives you to adventure?")
                .questionType(QuestionType.BACKGROUND)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        Question question2 = Question.builder()
                .id(2L)
                .questionText("Who do you trust most?")
                .questionType(QuestionType.CONNECTION)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Question> questionPage = new PageImpl<>(List.of(question1, question2));
        when(questionRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(questionPage);

        // Act
        PagedResponse<QuestionResponse> result = questionService.getAllQuestions(0, 20, false, null, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getCurrentPage()).isZero();
        assertThat(result.getPageSize()).isEqualTo(2);
        assertThat(result.getContent().get(0).getQuestionText()).isEqualTo("What drives you to adventure?");
        assertThat(result.getContent().get(1).getQuestionText()).isEqualTo("Who do you trust most?");
    }

    @Test
    void getAllQuestions_WithExpansionFilter_ReturnsFilteredQuestions() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Question question = Question.builder()
                .id(1L)
                .questionText("What drives you to adventure?")
                .questionType(QuestionType.BACKGROUND)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Question> questionPage = new PageImpl<>(List.of(question));
        when(questionRepository.findByDeletedAtIsNullAndFilters(eq(1L), isNull(), any(Pageable.class)))
                .thenReturn(questionPage);

        // Act
        PagedResponse<QuestionResponse> result = questionService.getAllQuestions(0, 20, false, 1L, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansionId()).isEqualTo(1L);
        verify(questionRepository).findByDeletedAtIsNullAndFilters(eq(1L), isNull(), any(Pageable.class));
    }

    @Test
    void getAllQuestions_WithQuestionTypeFilter_ReturnsFilteredQuestions() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Question question = Question.builder()
                .id(1L)
                .questionText("What drives you to adventure?")
                .questionType(QuestionType.BACKGROUND)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Question> questionPage = new PageImpl<>(List.of(question));
        when(questionRepository.findByDeletedAtIsNullAndFilters(isNull(), eq(QuestionType.BACKGROUND), any(Pageable.class)))
                .thenReturn(questionPage);

        // Act
        PagedResponse<QuestionResponse> result = questionService.getAllQuestions(0, 20, false, null, QuestionType.BACKGROUND, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getQuestionType()).isEqualTo(QuestionType.BACKGROUND);
        verify(questionRepository).findByDeletedAtIsNullAndFilters(isNull(), eq(QuestionType.BACKGROUND), any(Pageable.class));
    }

    @Test
    void getAllQuestions_WithIncludeDeleted_ReturnsAllQuestions() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Question question = Question.builder()
                .id(1L)
                .questionText("Deleted question")
                .questionType(QuestionType.BACKGROUND)
                .expansion(expansion)
                .deletedAt(LocalDateTime.now())
                .build();

        Page<Question> questionPage = new PageImpl<>(List.of(question));
        when(questionRepository.findAllWithFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(questionPage);

        // Act
        PagedResponse<QuestionResponse> result = questionService.getAllQuestions(0, 20, true, null, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeletedAt()).isNotNull();
        verify(questionRepository).findAllWithFilters(isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void getAllQuestions_WithLargePage_LimitsTo100() {
        // Arrange
        Page<Question> questionPage = new PageImpl<>(List.of());
        when(questionRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(questionPage);

        // Act
        questionService.getAllQuestions(0, 500, false, null, null, null);

        // Assert
        verify(questionRepository).findByDeletedAtIsNullAndFilters(
                isNull(),
                isNull(),
                argThat(pageable -> pageable.getPageSize() == 100)
        );
    }

    @Test
    void getAllQuestions_WithExpandParameter_ExpandsExpansion() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .createdAt(LocalDateTime.now())
                .build();

        Question question = Question.builder()
                .id(1L)
                .questionText("What drives you to adventure?")
                .questionType(QuestionType.BACKGROUND)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Question> questionPage = new PageImpl<>(List.of(question));
        when(questionRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(questionPage);

        // Act
        PagedResponse<QuestionResponse> result = questionService.getAllQuestions(0, 20, false, null, null, "expansion");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansion()).isNotNull();
        assertThat(result.getContent().get(0).getExpansion().getName()).isEqualTo("Core Rulebook");
    }

    // ==================== GET QUESTION BY ID TESTS ====================

    @Test
    void getQuestionById_ValidId_ReturnsQuestion() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Question question = Question.builder()
                .id(1L)
                .questionText("What drives you to adventure?")
                .questionType(QuestionType.BACKGROUND)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        when(questionRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(question));

        // Act
        QuestionResponse result = questionService.getQuestionById(1L, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getQuestionText()).isEqualTo("What drives you to adventure?");
        assertThat(result.getQuestionType()).isEqualTo(QuestionType.BACKGROUND);
    }

    @Test
    void getQuestionById_WithExpandParameter_ExpandsExpansion() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .createdAt(LocalDateTime.now())
                .build();

        Question question = Question.builder()
                .id(1L)
                .questionText("What drives you to adventure?")
                .questionType(QuestionType.BACKGROUND)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        when(questionRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(question));

        // Act
        QuestionResponse result = questionService.getQuestionById(1L, "expansion");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getExpansion()).isNotNull();
        assertThat(result.getExpansion().getName()).isEqualTo("Core Rulebook");
    }

    @Test
    void getQuestionById_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(questionRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> questionService.getQuestionById(999L, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Question not found with id: 999");
    }

    // ==================== CREATE QUESTION TESTS ====================

    @Test
    void createQuestion_ValidRequest_CreatesAndReturnsQuestion() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        CreateQuestionRequest request = CreateQuestionRequest.builder()
                .questionText("What drives you to adventure?")
                .questionType(QuestionType.BACKGROUND)
                .expansionId(1L)
                .build();

        Question savedQuestion = Question.builder()
                .id(1L)
                .questionText("What drives you to adventure?")
                .questionType(QuestionType.BACKGROUND)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(expansion));
        when(questionRepository.save(any(Question.class)))
                .thenReturn(savedQuestion);

        // Act
        QuestionResponse result = questionService.createQuestion(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getQuestionText()).isEqualTo("What drives you to adventure?");
        assertThat(result.getQuestionType()).isEqualTo(QuestionType.BACKGROUND);

        verify(questionRepository).save(argThat(question ->
                question.getQuestionText().equals("What drives you to adventure?") &&
                        question.getQuestionType().equals(QuestionType.BACKGROUND)
        ));
    }

    @Test
    void createQuestion_ExpansionNotFound_ThrowsEntityNotFoundException() {
        // Arrange
        CreateQuestionRequest request = CreateQuestionRequest.builder()
                .questionText("What drives you to adventure?")
                .questionType(QuestionType.BACKGROUND)
                .expansionId(999L)
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> questionService.createQuestion(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expansion not found with id: 999");

        verify(questionRepository, never()).save(any());
    }

    // ==================== CREATE QUESTIONS BULK TESTS ====================

    @Test
    void createQuestionsBulk_ValidRequests_CreatesAndReturnsQuestions() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        CreateQuestionRequest request1 = CreateQuestionRequest.builder()
                .questionText("What drives you to adventure?")
                .questionType(QuestionType.BACKGROUND)
                .expansionId(1L)
                .build();

        CreateQuestionRequest request2 = CreateQuestionRequest.builder()
                .questionText("Who do you trust most?")
                .questionType(QuestionType.CONNECTION)
                .expansionId(1L)
                .build();

        Question savedQuestion1 = Question.builder()
                .id(1L)
                .questionText("What drives you to adventure?")
                .questionType(QuestionType.BACKGROUND)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        Question savedQuestion2 = Question.builder()
                .id(2L)
                .questionText("Who do you trust most?")
                .questionType(QuestionType.CONNECTION)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(expansion));
        when(questionRepository.saveAll(anyList()))
                .thenReturn(List.of(savedQuestion1, savedQuestion2));

        // Act
        List<QuestionResponse> results = questionService.createQuestionsBulk(List.of(request1, request2));

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getQuestionText()).isEqualTo("What drives you to adventure?");
        assertThat(results.get(1).getQuestionText()).isEqualTo("Who do you trust most?");
        verify(questionRepository).saveAll(anyList());
    }

    // ==================== UPDATE QUESTION TESTS ====================

    @Test
    void updateQuestion_ValidRequest_UpdatesAndReturnsQuestion() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Question existingQuestion = Question.builder()
                .id(1L)
                .questionText("Old question text")
                .questionType(QuestionType.BACKGROUND)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        UpdateQuestionRequest request = UpdateQuestionRequest.builder()
                .questionText("Updated question text")
                .questionType(QuestionType.CONNECTION)
                .expansionId(1L)
                .build();

        when(questionRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(existingQuestion));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(expansion));
        when(questionRepository.save(any(Question.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        QuestionResponse result = questionService.updateQuestion(1L, request);

        // Assert
        assertThat(result.getQuestionText()).isEqualTo("Updated question text");
        assertThat(result.getQuestionType()).isEqualTo(QuestionType.CONNECTION);

        verify(questionRepository).save(argThat(question ->
                question.getQuestionText().equals("Updated question text") &&
                        question.getQuestionType().equals(QuestionType.CONNECTION)
        ));
    }

    @Test
    void updateQuestion_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        UpdateQuestionRequest request = UpdateQuestionRequest.builder()
                .questionText("Updated question text")
                .questionType(QuestionType.CONNECTION)
                .expansionId(1L)
                .build();

        when(questionRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> questionService.updateQuestion(999L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Question not found with id: 999");

        verify(questionRepository, never()).save(any());
    }

    @Test
    void updateQuestion_ExpansionNotFound_ThrowsEntityNotFoundException() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Question existingQuestion = Question.builder()
                .id(1L)
                .questionText("Old question text")
                .questionType(QuestionType.BACKGROUND)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        UpdateQuestionRequest request = UpdateQuestionRequest.builder()
                .questionText("Updated question text")
                .questionType(QuestionType.CONNECTION)
                .expansionId(999L)
                .build();

        when(questionRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(existingQuestion));
        when(expansionRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> questionService.updateQuestion(1L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expansion not found with id: 999");

        verify(questionRepository, never()).save(any());
    }

    // ==================== DELETE QUESTION TESTS ====================

    @Test
    void deleteQuestion_ValidId_SoftDeletesQuestion() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Question question = Question.builder()
                .id(1L)
                .questionText("To Delete")
                .questionType(QuestionType.BACKGROUND)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        when(questionRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(question));

        // Act
        questionService.deleteQuestion(1L);

        // Assert
        verify(questionRepository).save(argThat(q -> q.getDeletedAt() != null));
    }

    @Test
    void deleteQuestion_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(questionRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> questionService.deleteQuestion(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Question not found with id: 999");

        verify(questionRepository, never()).save(any());
    }

    // ==================== RESTORE QUESTION TESTS ====================

    @Test
    void restoreQuestion_DeletedQuestion_RestoresSuccessfully() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Question deletedQuestion = Question.builder()
                .id(1L)
                .questionText("Deleted Question")
                .questionType(QuestionType.BACKGROUND)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .deletedAt(LocalDateTime.now())
                .build();

        when(questionRepository.findById(1L))
                .thenReturn(Optional.of(deletedQuestion));
        when(questionRepository.save(any(Question.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        QuestionResponse result = questionService.restoreQuestion(1L);

        // Assert
        assertThat(result).isNotNull();
        verify(questionRepository).save(argThat(q -> q.getDeletedAt() == null));
    }

    @Test
    void restoreQuestion_NotDeleted_ThrowsIllegalStateException() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Question activeQuestion = Question.builder()
                .id(1L)
                .questionText("Active Question")
                .questionType(QuestionType.BACKGROUND)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        when(questionRepository.findById(1L))
                .thenReturn(Optional.of(activeQuestion));

        // Act & Assert
        assertThatThrownBy(() -> questionService.restoreQuestion(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Question with id 1 is not deleted");

        verify(questionRepository, never()).save(any());
    }

    @Test
    void restoreQuestion_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(questionRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> questionService.restoreQuestion(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Question not found with id: 999");
    }
}
