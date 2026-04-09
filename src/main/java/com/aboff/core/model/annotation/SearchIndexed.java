package com.aboff.core.model.annotation;

import com.aboff.core.model.enums.SearchableEntityType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for JPA entity classes that should be included in the full-text search index.
 * <p>
 * Apply this annotation to any entity class that should have its data synchronized to the
 * {@code search_index} table. The {@link #type()} attribute declares which
 * {@link SearchableEntityType} the annotated entity maps to, allowing the search indexing
 * infrastructure to route and categorize results correctly.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>
 * {@code
 * @Entity
 * @SearchIndexed(type = SearchableEntityType.WEAPON)
 * public class Weapon extends BaseItem {
 *     // ...
 * }
 * }
 * </pre>
 *
 * <p>
 * This annotation is retained at runtime so that indexing services can discover annotated
 * entity classes via reflection.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SearchIndexed {

    /**
     * The {@link SearchableEntityType} that identifies the category of the annotated entity
     * within the search index.
     *
     * @return the searchable entity type for this entity
     */
    SearchableEntityType type();
}
