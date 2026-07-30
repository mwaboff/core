package com.aboff.core.repository;

import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.CharacterSheet;
import com.aboff.core.model.entity.dh.CharacterSheetDomainCard;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.DomainCard;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.TransformationCard;
import com.aboff.core.model.enums.DomainCardType;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.dh.CharacterSheetDomainCardRepository;
import com.aboff.core.repository.dh.CharacterSheetRepository;
import com.aboff.core.repository.dh.DomainCardRepository;
import com.aboff.core.repository.dh.DomainRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.TransformationCardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test proving that {@link TransformationCard} — a standalone entity, deliberately
 * NOT a {@code DomainCard} row (see {@code TransformationCard}'s class javadoc and HF-06's
 * packet contract) — has no effect on the existing 5-card domain-card loadout cap.
 * <p>
 * The cap is enforced in {@code LevelUpService} by counting equipped rows via
 * {@link CharacterSheetDomainCardRepository#countEquippedByCharacterSheetId(Long)}, a query
 * scoped entirely to the {@code character_sheet_domain_cards} join table and the
 * {@code DomainCard} entity it joins to. {@code TransformationCard} has no relationship to
 * either, so this test creates real {@code TransformationCard} rows (including several sharing
 * the exact same expansion as the equipped domain cards) and asserts the equipped-domain-card
 * count is completely unaffected by their existence.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class TransformationCardLoadoutCapRegressionTest {

    @Autowired
    private CharacterSheetRepository characterSheetRepository;

    @Autowired
    private CharacterSheetDomainCardRepository characterSheetDomainCardRepository;

    @Autowired
    private DomainCardRepository domainCardRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private TransformationCardRepository transformationCardRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    @Autowired
    private UserRepository userRepository;

    private Expansion expansion;
    private Domain domain;
    private CharacterSheet sheet;

    @BeforeEach
    void setUp() {
        expansion = expansionRepository.save(
                Expansion.builder().name("Hope & Fear").isPublished(true).build());
        domain = domainRepository.save(
                Domain.builder().name("Blade").expansion(expansion).build());

        User owner = userRepository.save(User.builder()
                .username("loadout-tester")
                .email("loadout-tester@example.com")
                .role(Role.USER)
                .build());

        sheet = characterSheetRepository.save(CharacterSheet.builder()
                .name("Loadout Test Character")
                .majorDamageThreshold(8)
                .severeDamageThreshold(12)
                .owner(owner)
                .build());
    }

    @Test
    void equippedDomainCardCount_AtCap_IsUnaffectedByUnrelatedTransformationCards() {
        // Equip exactly 5 domain cards — the existing cap.
        for (int i = 1; i <= 5; i++) {
            equipDomainCard("Domain Card " + i);
        }

        // Create several TransformationCards, deliberately in the SAME expansion, to prove
        // proximity/shared-expansion alone doesn't leak into the domain-card count.
        for (int i = 1; i <= 8; i++) {
            transformationCardRepository.save(TransformationCard.builder()
                    .name("Transformation Card " + i)
                    .description("Unrelated content")
                    .expansion(expansion)
                    .build());
        }

        long equippedCount = characterSheetDomainCardRepository.countEquippedByCharacterSheetId(sheet.getId());

        assertThat(equippedCount).isEqualTo(5);
        assertThat(transformationCardRepository.count()).isEqualTo(8);
        assertThat(domainCardRepository.count()).isEqualTo(5);
    }

    @Test
    void equippedDomainCardCount_BelowCap_StillCountsOnlyRealDomainCards() {
        equipDomainCard("Domain Card A");
        equipDomainCard("Domain Card B");

        for (int i = 1; i <= 20; i++) {
            transformationCardRepository.save(TransformationCard.builder()
                    .name("Bulk Transformation Card " + i)
                    .expansion(expansion)
                    .build());
        }

        long equippedCount = characterSheetDomainCardRepository.countEquippedByCharacterSheetId(sheet.getId());

        // A large number of unrelated TransformationCard rows must not inflate the count.
        assertThat(equippedCount).isEqualTo(2);
    }

    private void equipDomainCard(String name) {
        DomainCard domainCard = DomainCard.builder()
                .name(name)
                .description("Test domain card")
                .associatedDomain(domain)
                .level(1)
                .recallCost(0)
                .type(DomainCardType.ABILITY)
                .expansion(expansion)
                .isOfficial(true)
                .build();
        domainCard = domainCardRepository.save(domainCard);

        CharacterSheetDomainCard link = CharacterSheetDomainCard.builder()
                .characterSheet(sheet)
                .domainCard(domainCard)
                .equipped(true)
                .build();
        characterSheetDomainCardRepository.save(link);
    }
}
