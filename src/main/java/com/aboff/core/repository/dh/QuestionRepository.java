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

    @Query("SELECT q FROM Question q WHERE q.deletedAt IS NULL " +
           "AND (:expansionId IS NULL OR q.expansion.id = :expansionId) " +
           "AND (:questionType IS NULL OR q.questionType = :questionType)")
    Page<Question> findByDeletedAtIsNullAndFilters(
            @Param("expansionId") Long expansionId,
            @Param("questionType") QuestionType questionType,
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
}
