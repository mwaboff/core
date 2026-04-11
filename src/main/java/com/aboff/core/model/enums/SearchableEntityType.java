package com.aboff.core.model.enums;

/**
 * Enum representing all entity types that can be indexed and searched via the full-text search system.
 * <p>
 * Each value corresponds to a distinct Daggerheart game content type. The search index stores the
 * entity type as a string column using the enum name, allowing efficient filtering by content type.
 * </p>
 *
 * <ul>
 *   <li>{@link #DOMAIN} - Game domains (e.g., Blade, Codex, Grace)</li>
 *   <li>{@link #CLASS} - Playable character classes</li>
 *   <li>{@link #FEATURE} - Class or subclass features</li>
 *   <li>{@link #ANCESTRY_CARD} - Ancestry cards representing character lineage</li>
 *   <li>{@link #COMMUNITY_CARD} - Community cards representing character background</li>
 *   <li>{@link #SUBCLASS_CARD} - Subclass selection cards</li>
 *   <li>{@link #DOMAIN_CARD} - Domain ability cards</li>
 *   <li>{@link #WEAPON} - Weapon items</li>
 *   <li>{@link #ARMOR} - Armor items</li>
 *   <li>{@link #LOOT} - Loot and miscellaneous items</li>
 *   <li>{@link #ADVERSARY} - Adversaries (NPCs and enemies)</li>
 *   <li>{@link #BEASTFORM} - Beastform transformations</li>
 *   <li>{@link #ENCOUNTER} - Pre-built encounters</li>
 *   <li>{@link #EXPANSION} - Content expansions / source books</li>
 *   <li>{@link #SUBCLASS_PATH} - Subclass progression paths</li>
 *   <li>{@link #QUESTION} - Character creation questions</li>
 *   <li>{@link #CARD_COST_TAG} - Tags that describe card costs</li>
 * </ul>
 */
public enum SearchableEntityType {
    DOMAIN,
    CLASS,
    FEATURE,
    ANCESTRY_CARD,
    COMMUNITY_CARD,
    SUBCLASS_CARD,
    DOMAIN_CARD,
    WEAPON,
    ARMOR,
    LOOT,
    ADVERSARY,
    BEASTFORM,
    ENCOUNTER,
    EXPANSION,
    SUBCLASS_PATH,
    QUESTION,
    CARD_COST_TAG
}
