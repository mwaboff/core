package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.Question;
import com.aboff.core.model.enums.QuestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Question entities.
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    /**
     * Finds all non-deleted questions with optional filters.
     *
     * @param expansionId Optional filter for expansion ID
     * @param questionType Optional filter for question type
     * @param includeNonSrd Whether the caller may see paid-expansion (non-SRD) questions; when
     *                      false, only SRD-flagged questions are returned
     * @param pageable Pagination information
     * @return Page of non-deleted questions matching the criteria
     */
    @Query("SELECT q FROM Question q WHERE q.deletedAt IS NULL " +
           "AND (:expansionId IS NULL OR q.expansion.id = :expansionId) " +
           "AND (:questionType IS NULL OR q.questionType = :questionType) " +
           "AND (:includeNonSrd = true OR q.srd = true)")
    Page<Question> findByDeletedAtIsNullAndFilters(
            @Param("expansionId") Long expansionId,
            @Param("questionType") QuestionType questionType,
            @Param("includeNonSrd") boolean includeNonSrd,
            Pageable pageable);

    @Query("SELECT q FROM Question q WHERE " +
           "(:expansionId IS NULL OR q.expansion.id = :expansionId) " +
           "AND (:questionType IS NULL OR q.questionType = :questionType)")
    Page<Question> findAllWithFilters(
            @Param("expansionId") Long expansionId,
            @Param("questionType") QuestionType questionType,
            Pageable pageable);

    @Query("SELECT q FROM Question q WHERE q.id = :id AND q.deletedAt IS NULL")
    Optional<Question> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    @Query("SELECT q FROM Question q WHERE q.id IN :ids AND q.deletedAt IS NULL")
    List<Question> findAllByIdInAndDeletedAtIsNull(@Param("ids") List<Long> ids);

    @Query("SELECT q FROM Question q WHERE LOWER(q.questionText) = LOWER(:questionText) " +
           "AND q.expansion.id = :expansionId " +
           "AND q.questionType = :questionType " +
           "AND q.deletedAt IS NULL")
    Optional<Question> findByQuestionTextIgnoreCaseAndExpansionIdAndQuestionTypeAndDeletedAtIsNull(
            @Param("questionText") String questionText,
            @Param("expansionId") Long expansionId,
            @Param("questionType") QuestionType questionType);
}
