package com.aboff.core.config;

import com.aboff.core.model.entity.dh.Adversary;
import com.aboff.core.model.entity.dh.AncestryCard;
import com.aboff.core.model.entity.dh.Armor;
import com.aboff.core.model.entity.dh.Beastform;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.entity.dh.Class;
import com.aboff.core.model.entity.dh.CommunityCard;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.DomainCard;
import com.aboff.core.model.entity.dh.Encounter;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.Loot;
import com.aboff.core.model.entity.dh.Question;
import com.aboff.core.model.entity.dh.SubclassCard;
import com.aboff.core.model.entity.dh.SubclassPath;
import com.aboff.core.model.entity.dh.Weapon;
import com.aboff.core.model.enums.SearchableEntityType;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Spring component responsible for extracting and mapping searchable field data from
 * {@link com.aboff.core.model.annotation.SearchIndexed}-annotated entities into a
 * normalized {@link SearchIndexData} record.
 *
 * <p>Each entity type has a distinct field-to-weight mapping that determines how its content
 * contributes to the PostgreSQL {@code tsvector} search vector:
 * <ul>
 *   <li><strong>nameText (Weight A)</strong> - highest priority; typically the entity name</li>
 *   <li><strong>descriptionText (Weight B)</strong> - medium priority; description and related narrative text</li>
 *   <li><strong>featureText (Weight C)</strong> - lower priority; concatenated feature names and descriptions</li>
 * </ul>
 *
 * <p>The actual {@code tsvector} computation is delegated to PostgreSQL via an upsert query.
 * This class only extracts and normalizes the Java-side string content and filter column values.
 *
 * <p>Usage example:
 * <pre>
 * {@code
 * SearchFieldMapping.SearchIndexData data = searchFieldMapping.buildSearchIndexData(weapon, SearchableEntityType.WEAPON);
 * }
 * </pre>
 */
@Component
@Slf4j
public class SearchFieldMapping {

    /**
     * Builds a {@link SearchIndexData} record from the given entity and its resolved type.
     *
     * <p>The entity is cast to the concrete type indicated by {@code type} and the appropriate
     * fields are extracted. Filter columns that are not applicable to the given entity type
     * will be left as {@code null}.
     *
     * @param entity the JPA entity instance to extract data from; must match the expected type for {@code type}
     * @param type   the {@link SearchableEntityType} corresponding to the entity's class
     * @return a fully populated {@link SearchIndexData} containing text fields and applicable filter columns,
     *         or {@code null} if the entity's name field is {@code null} (entity cannot be indexed)
     * @throws IllegalArgumentException if the entity cannot be cast to the expected type for {@code type}
     */
    public SearchIndexData buildSearchIndexData(Object entity, SearchableEntityType type) {
        log.debug("Building search index data for entity type={}", type);
        SearchIndexData data = switch (type) {
            case DOMAIN -> buildForDomain((Domain) entity);
            case CLASS -> buildForClass((Class) entity);
            case FEATURE -> buildForFeature((Feature) entity);
            case ANCESTRY_CARD -> buildForAncestryCard((AncestryCard) entity);
            case COMMUNITY_CARD -> buildForCommunityCard((CommunityCard) entity);
            case SUBCLASS_CARD -> buildForSubclassCard((SubclassCard) entity);
            case DOMAIN_CARD -> buildForDomainCard((DomainCard) entity);
            case WEAPON -> buildForWeapon((Weapon) entity);
            case ARMOR -> buildForArmor((Armor) entity);
            case LOOT -> buildForLoot((Loot) entity);
            case ADVERSARY -> buildForAdversary((Adversary) entity);
            case BEASTFORM -> buildForBeastform((Beastform) entity);
            case ENCOUNTER -> buildForEncounter((Encounter) entity);
            case EXPANSION -> buildForExpansion((Expansion) entity);
            case SUBCLASS_PATH -> buildForSubclassPath((SubclassPath) entity);
            case QUESTION -> buildForQuestion((Question) entity);
            case CARD_COST_TAG -> buildForCardCostTag((CardCostTag) entity);
        };

        if (data.getName() == null) {
            log.warn("Entity type={} id={} has a null name and cannot be indexed; skipping",
                    type, data.getEntityId());
            return null;
        }

        return data;
    }

    // -------------------------------------------------------------------------
    // Private builder methods — one per entity type
    // -------------------------------------------------------------------------

    /**
     * Builds search index data for a {@link Domain} entity.
     * Weight A: name. Weight B: description. Filter: expansionId.
     *
     * @param domain the domain entity
     * @return populated search index data
     */
    private SearchIndexData buildForDomain(Domain domain) {
        return SearchIndexData.builder()
                .entityType(SearchableEntityType.DOMAIN.name())
                .entityId(domain.getId())
                .name(domain.getName())
                .nameText(domain.getName())
                .descriptionText(domain.getDescription())
                .expansionId(expansionId(domain.getExpansion()))
                .build();
    }

    /**
     * Builds search index data for a {@link Class} entity.
     * Weight A: name. Weight B: description + startingClassItems. Filter: expansionId.
     *
     * @param cls the class entity
     * @return populated search index data
     */
    private SearchIndexData buildForClass(Class cls) {
        return SearchIndexData.builder()
                .entityType(SearchableEntityType.CLASS.name())
                .entityId(cls.getId())
                .name(cls.getName())
                .nameText(cls.getName())
                .descriptionText(joinNonNull(cls.getDescription(), cls.getStartingClassItems()))
                .expansionId(expansionId(cls.getExpansion()))
                .build();
    }

    /**
     * Builds search index data for a {@link Feature} entity.
     * Weight A: name. Weight B: description. Filter: expansionId, featureType.
     *
     * @param feature the feature entity
     * @return populated search index data
     */
    private SearchIndexData buildForFeature(Feature feature) {
        return SearchIndexData.builder()
                .entityType(SearchableEntityType.FEATURE.name())
                .entityId(feature.getId())
                .name(feature.getName())
                .nameText(feature.getName())
                .descriptionText(feature.getDescription())
                .expansionId(expansionId(feature.getExpansion()))
                .featureType(enumName(feature.getFeatureType()))
                .build();
    }

    /**
     * Builds search index data for an {@link AncestryCard} entity.
     * Weight A: name. Weight B: description. Weight C: features text.
     * Filter: expansionId, isOfficial, isMixed.
     *
     * @param card the ancestry card entity
     * @return populated search index data
     */
    private SearchIndexData buildForAncestryCard(AncestryCard card) {
        return SearchIndexData.builder()
                .entityType(SearchableEntityType.ANCESTRY_CARD.name())
                .entityId(card.getId())
                .name(card.getName())
                .nameText(card.getName())
                .descriptionText(card.getDescription())
                .featureText(extractFeatureText(card.getFeatures()))
                .expansionId(expansionId(card.getExpansion()))
                .isOfficial(card.getIsOfficial())
                .isMixed(card.getIsMixed())
                .build();
    }

    /**
     * Builds search index data for a {@link CommunityCard} entity.
     * Weight A: name. Weight B: description. Weight C: features text.
     * Filter: expansionId.
     *
     * @param card the community card entity
     * @return populated search index data
     */
    private SearchIndexData buildForCommunityCard(CommunityCard card) {
        return SearchIndexData.builder()
                .entityType(SearchableEntityType.COMMUNITY_CARD.name())
                .entityId(card.getId())
                .name(card.getName())
                .nameText(card.getName())
                .descriptionText(card.getDescription())
                .featureText(extractFeatureText(card.getFeatures()))
                .expansionId(expansionId(card.getExpansion()))
                .build();
    }

    /**
     * Builds search index data for a {@link SubclassCard} entity.
     * Weight A: name. Weight B: description. Weight C: features text.
     * Filter: expansionId, isOfficial, subclassLevel.
     *
     * @param card the subclass card entity
     * @return populated search index data
     */
    private SearchIndexData buildForSubclassCard(SubclassCard card) {
        return SearchIndexData.builder()
                .entityType(SearchableEntityType.SUBCLASS_CARD.name())
                .entityId(card.getId())
                .name(card.getName())
                .nameText(card.getName())
                .descriptionText(card.getDescription())
                .featureText(extractFeatureText(card.getFeatures()))
                .expansionId(expansionId(card.getExpansion()))
                .isOfficial(card.getIsOfficial())
                .subclassLevel(enumName(card.getLevel()))
                .build();
    }

    /**
     * Builds search index data for a {@link DomainCard} entity.
     * Weight A: name. Weight B: description. Weight C: features text.
     * Filter: expansionId, isOfficial, domainCardType, associatedDomainId.
     *
     * @param card the domain card entity
     * @return populated search index data
     */
    private SearchIndexData buildForDomainCard(DomainCard card) {
        Long associatedDomainId = card.getAssociatedDomain() != null
                ? card.getAssociatedDomain().getId()
                : null;

        return SearchIndexData.builder()
                .entityType(SearchableEntityType.DOMAIN_CARD.name())
                .entityId(card.getId())
                .name(card.getName())
                .nameText(card.getName())
                .descriptionText(card.getDescription())
                .featureText(extractFeatureText(card.getFeatures()))
                .expansionId(expansionId(card.getExpansion()))
                .isOfficial(card.getIsOfficial())
                .domainCardType(enumName(card.getType()))
                .associatedDomainId(associatedDomainId)
                .build();
    }

    /**
     * Builds search index data for a {@link Weapon} entity.
     * Weight A: name. Weight C: features text.
     * Filter: expansionId, isOfficial, tier, createdByUserId, trait, range, burden, isPrimary, damageType.
     *
     * @param weapon the weapon entity
     * @return populated search index data
     */
    private SearchIndexData buildForWeapon(Weapon weapon) {
        String damageType = weapon.getDamage() != null
                ? enumName(weapon.getDamage().getDamageType())
                : null;

        return SearchIndexData.builder()
                .entityType(SearchableEntityType.WEAPON.name())
                .entityId(weapon.getId())
                .name(weapon.getName())
                .nameText(weapon.getName())
                .featureText(extractFeatureText(weapon.getFeatures()))
                .expansionId(expansionId(weapon.getExpansion()))
                .isOfficial(weapon.getIsOfficial())
                .tier(weapon.getTier())
                .createdByUserId(userId(weapon.getCreatedBy()))
                .trait(enumName(weapon.getTrait()))
                .range(enumName(weapon.getRange()))
                .burden(enumName(weapon.getBurden()))
                .isPrimary(weapon.getIsPrimary())
                .damageType(damageType)
                .build();
    }

    /**
     * Builds search index data for an {@link Armor} entity.
     * Weight A: name. Weight C: features text.
     * Filter: expansionId, isOfficial, tier, createdByUserId.
     *
     * @param armor the armor entity
     * @return populated search index data
     */
    private SearchIndexData buildForArmor(Armor armor) {
        return SearchIndexData.builder()
                .entityType(SearchableEntityType.ARMOR.name())
                .entityId(armor.getId())
                .name(armor.getName())
                .nameText(armor.getName())
                .featureText(extractFeatureText(armor.getFeatures()))
                .expansionId(expansionId(armor.getExpansion()))
                .isOfficial(armor.getIsOfficial())
                .tier(armor.getTier())
                .createdByUserId(userId(armor.getCreatedBy()))
                .build();
    }

    /**
     * Builds search index data for a {@link Loot} entity.
     * Weight A: name. Weight B: description. Weight C: features text.
     * Filter: expansionId, isOfficial, tier, createdByUserId, isConsumable.
     *
     * @param loot the loot entity
     * @return populated search index data
     */
    private SearchIndexData buildForLoot(Loot loot) {
        return SearchIndexData.builder()
                .entityType(SearchableEntityType.LOOT.name())
                .entityId(loot.getId())
                .name(loot.getName())
                .nameText(loot.getName())
                .descriptionText(loot.getDescription())
                .featureText(extractFeatureText(loot.getFeatures()))
                .expansionId(expansionId(loot.getExpansion()))
                .isOfficial(loot.getIsOfficial())
                .tier(loot.getTier())
                .createdByUserId(userId(loot.getCreatedBy()))
                .isConsumable(loot.getIsConsumable())
                .build();
    }

    /**
     * Builds search index data for an {@link Adversary} entity.
     * Weight A: name. Weight B: description + motivesAndTactics + weaponName.
     * Weight C: features text.
     * Filter: expansionId, isOfficial, isPublic, tier, createdByUserId, adversaryType.
     *
     * @param adversary the adversary entity
     * @return populated search index data
     */
    private SearchIndexData buildForAdversary(Adversary adversary) {
        return SearchIndexData.builder()
                .entityType(SearchableEntityType.ADVERSARY.name())
                .entityId(adversary.getId())
                .name(adversary.getName())
                .nameText(adversary.getName())
                .descriptionText(joinNonNull(
                        adversary.getDescription(),
                        adversary.getMotivesAndTactics(),
                        adversary.getWeaponName()))
                .featureText(extractFeatureText(adversary.getFeatures()))
                .expansionId(expansionId(adversary.getExpansion()))
                .isOfficial(adversary.getIsOfficial())
                .isPublic(adversary.getIsPublic())
                .tier(adversary.getTier())
                .createdByUserId(userId(adversary.getCreatedBy()))
                .adversaryType(enumName(adversary.getAdversaryType()))
                .build();
    }

    /**
     * Builds search index data for a {@link Beastform} entity.
     * Weight A: name. Weight B: example + advantages. Weight C: features text.
     * Filter: expansionId, isOfficial, isPublic, createdByUserId.
     *
     * @param beastform the beastform entity
     * @return populated search index data
     */
    private SearchIndexData buildForBeastform(Beastform beastform) {
        return SearchIndexData.builder()
                .entityType(SearchableEntityType.BEASTFORM.name())
                .entityId(beastform.getId())
                .name(beastform.getName())
                .nameText(beastform.getName())
                .descriptionText(joinNonNull(beastform.getExample(), beastform.getAdvantages()))
                .featureText(extractFeatureText(beastform.getFeatures()))
                .expansionId(expansionId(beastform.getExpansion()))
                .isOfficial(beastform.getIsOfficial())
                .isPublic(beastform.getIsPublic())
                .createdByUserId(userId(beastform.getCreatedBy()))
                .build();
    }

    /**
     * Builds search index data for an {@link Encounter} entity.
     * Weight A: name. Weight B: description.
     * Filter: isOfficial, isPublic, tier, createdByUserId.
     *
     * @param encounter the encounter entity
     * @return populated search index data
     */
    private SearchIndexData buildForEncounter(Encounter encounter) {
        return SearchIndexData.builder()
                .entityType(SearchableEntityType.ENCOUNTER.name())
                .entityId(encounter.getId())
                .name(encounter.getName())
                .nameText(encounter.getName())
                .descriptionText(encounter.getDescription())
                .isOfficial(encounter.getIsOfficial())
                .isPublic(encounter.getIsPublic())
                .tier(encounter.getTier())
                .createdByUserId(userId(encounter.getCreatedBy()))
                .build();
    }

    /**
     * Builds search index data for an {@link Expansion} entity.
     * Weight A: name only. No filter columns apply.
     *
     * @param expansion the expansion entity
     * @return populated search index data
     */
    private SearchIndexData buildForExpansion(Expansion expansion) {
        return SearchIndexData.builder()
                .entityType(SearchableEntityType.EXPANSION.name())
                .entityId(expansion.getId())
                .name(expansion.getName())
                .nameText(expansion.getName())
                .build();
    }

    /**
     * Builds search index data for a {@link SubclassPath} entity.
     * Weight A: name only. Filter: expansionId, associatedDomainId (first associated domain).
     *
     * <p>Note: SubclassPath can have multiple associated domains. Only the first domain's ID
     * is stored in the filter column. If broader domain filtering is needed, the search
     * infrastructure should be extended to support multi-value filters.
     *
     * @param path the subclass path entity
     * @return populated search index data
     */
    private SearchIndexData buildForSubclassPath(SubclassPath path) {
        Long firstDomainId = path.getAssociatedDomains() != null && !path.getAssociatedDomains().isEmpty()
                ? path.getAssociatedDomains().iterator().next().getId()
                : null;

        return SearchIndexData.builder()
                .entityType(SearchableEntityType.SUBCLASS_PATH.name())
                .entityId(path.getId())
                .name(path.getName())
                .nameText(path.getName())
                .expansionId(expansionId(path.getExpansion()))
                .associatedDomainId(firstDomainId)
                .build();
    }

    /**
     * Builds search index data for a {@link Question} entity.
     * Weight A: questionText. Filter: expansionId.
     *
     * @param question the question entity
     * @return populated search index data
     */
    private SearchIndexData buildForQuestion(Question question) {
        return SearchIndexData.builder()
                .entityType(SearchableEntityType.QUESTION.name())
                .entityId(question.getId())
                .name(question.getQuestionText())
                .nameText(question.getQuestionText())
                .expansionId(expansionId(question.getExpansion()))
                .build();
    }

    /**
     * Builds search index data for a {@link CardCostTag} entity.
     * Weight A: label. Filter: costTagCategory.
     *
     * @param tag the card cost tag entity
     * @return populated search index data
     */
    private SearchIndexData buildForCardCostTag(CardCostTag tag) {
        return SearchIndexData.builder()
                .entityType(SearchableEntityType.CARD_COST_TAG.name())
                .entityId(tag.getId())
                .name(tag.getLabel())
                .nameText(tag.getLabel())
                .costTagCategory(enumName(tag.getCategory()))
                .build();
    }

    // -------------------------------------------------------------------------
    // Utility helpers
    // -------------------------------------------------------------------------

    /**
     * Concatenates feature names and descriptions from a collection of {@link Feature} entities
     * into a single space-separated string suitable for use as the weight-C search text.
     *
     * <p>Null or empty collections return {@code null}. Individual null name or description
     * values within features are skipped.
     *
     * @param features the collection of features to extract text from; may be null or empty
     * @return concatenated feature text, or {@code null} if no text could be extracted
     */
    private String extractFeatureText(Collection<Feature> features) {
        if (features == null || features.isEmpty()) {
            return null;
        }
        String text = features.stream()
                .flatMap(f -> Stream.of(f.getName(), f.getDescription()))
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" "));
        return text.isBlank() ? null : text;
    }

    /**
     * Joins multiple string values with a space separator, skipping any that are null or blank.
     *
     * @param parts the string values to join; individual nulls are silently ignored
     * @return joined string, or {@code null} if all parts are null or blank
     */
    private String joinNonNull(String... parts) {
        String result = Arrays.stream(parts)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" "));
        return result.isBlank() ? null : result;
    }

    /**
     * Safely extracts the ID from an {@link Expansion} reference.
     *
     * @param expansion the expansion entity reference; may be null
     * @return the expansion's ID, or {@code null} if the expansion is null
     */
    private Long expansionId(Expansion expansion) {
        return expansion != null ? expansion.getId() : null;
    }

    /**
     * Safely extracts the ID from a {@link User} reference.
     *
     * @param user the user entity reference; may be null
     * @return the user's ID, or {@code null} if the user is null
     */
    private Long userId(User user) {
        return user != null ? user.getId() : null;
    }

    /**
     * Returns the name of the given enum constant, or {@code null} if the enum is null.
     *
     * @param e   the enum constant; may be null
     * @param <E> the enum type
     * @return the enum's {@link Enum#name()} string, or {@code null}
     */
    private <E extends Enum<E>> String enumName(E e) {
        return e != null ? e.name() : null;
    }

    // -------------------------------------------------------------------------
    // SearchIndexData inner record
    // -------------------------------------------------------------------------

    /**
     * Immutable data holder that contains all fields required to upsert a row in the
     * {@code search_index} table.
     *
     * <p>Text fields are segmented into three search weights:
     * <ul>
     *   <li>{@link #nameText} (Weight A) — highest relevance</li>
     *   <li>{@link #descriptionText} (Weight B) — medium relevance</li>
     *   <li>{@link #featureText} (Weight C) — lower relevance</li>
     * </ul>
     *
     * <p>All filter columns are nullable. A {@code null} value indicates that the filter
     * is not applicable for the entity type.
     */
    @Data
    @Builder
    public static class SearchIndexData {

        /**
         * The entity type name (matches a {@link SearchableEntityType} enum name).
         */
        private String entityType;

        /**
         * The primary key of the source entity.
         */
        private Long entityId;

        /**
         * The display name of the entity, used for result labeling.
         */
        private String name;

        /**
         * Weight-A text (highest relevance), typically the entity's name.
         */
        private String nameText;

        /**
         * Weight-B text (medium relevance), typically description or narrative content.
         */
        private String descriptionText;

        /**
         * Weight-C text (lower relevance), typically concatenated feature names and descriptions.
         */
        private String featureText;

        // ----- Filter columns (nullable) -----

        /** Numeric tier level (1–4) where applicable. */
        private Integer tier;

        /** ID of the source expansion, where applicable. */
        private Long expansionId;

        /** Whether the entity is official game content, where applicable. */
        private Boolean isOfficial;

        /** Whether the entity is publicly visible to all users, where applicable. */
        private Boolean isPublic;

        /** ID of the user who created the entity, where applicable. */
        private Long createdByUserId;

        /** Card type discriminator string, where applicable. */
        private String cardType;

        /** Feature type string, where applicable. */
        private String featureType;

        /** Adversary type string, where applicable. */
        private String adversaryType;

        /** Domain card type string, where applicable. */
        private String domainCardType;

        /** ID of the associated domain, where applicable. */
        private Long associatedDomainId;

        /** Primary trait name string, where applicable. */
        private String trait;

        /** Range category name string, where applicable. */
        private String range;

        /** Burden category name string, where applicable. */
        private String burden;

        /** Whether the entity is a primary item or ability, where applicable. */
        private Boolean isPrimary;

        /** Damage type name string, where applicable. */
        private String damageType;

        /** Whether the entity is consumable (single-use), where applicable. */
        private Boolean isConsumable;

        /** Whether the entity has a mixed classification, where applicable. */
        private Boolean isMixed;

        /** Subclass level name string (FOUNDATION, SPECIALIZATION, MASTERY), where applicable. */
        private String subclassLevel;

        /** Cost tag category name string, where applicable. */
        private String costTagCategory;
    }
}
