package com.aboff.core.model.dto.dh.request;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Structural guard: user-authored custom item requests must never declare an {@code srd} field.
 * <p>
 * SRD-ness is a property of published book content, not something a player invents at their
 * table. {@code CreateCustomWeaponRequest}, {@code CreateCustomArmorRequest}, and
 * {@code CreateCustomLootRequest} feed {@code WeaponService#createCustomWeapon},
 * {@code ArmorService#createCustomArmor}, and {@code LootService#createCustomLoot}, which set
 * {@code isOfficial = false} unconditionally — the SRD gate ({@code ContentAccessService#mayView})
 * already exempts non-official content regardless of its {@code srd} flag, so an {@code srd}
 * field on these three types would be dead weight at best and a way to imply a homebrew item is
 * "from the book" at worst. This test exists so nobody adds one later without noticing the rule.
 * </p>
 * <p>
 * The corresponding official/admin request types ({@code CreateWeaponRequest},
 * {@code CreateArmorRequest}, {@code CreateLootRequest}, and their {@code Update*} counterparts)
 * are deliberately exempt — they DO carry an optional {@code srd} field, honoured only for
 * ADMIN+ via {@code ContentAccessService#resolveSrd}.
 * </p>
 */
class CustomItemRequestSrdFieldTest {

    private static final List<Class<?>> CUSTOM_ITEM_REQUEST_TYPES = List.of(
            CreateCustomWeaponRequest.class,
            CreateCustomArmorRequest.class,
            CreateCustomLootRequest.class);

    @Test
    void customItemRequests_DoNotDeclareAnSrdField() {
        for (Class<?> requestType : CUSTOM_ITEM_REQUEST_TYPES) {
            boolean declaresSrd = declaresFieldNamed(requestType, "srd");

            assertThat(declaresSrd)
                    .as("%s must not declare an 'srd' field -- custom content came from no book, "
                            + "and the SRD gate already exempts it via isOfficial=false regardless "
                            + "of any srd flag. See the class-level javadoc on %s.",
                            requestType.getSimpleName(), CustomItemRequestSrdFieldTest.class.getSimpleName())
                    .isFalse();
        }
    }

    private boolean declaresFieldNamed(Class<?> type, String fieldName) {
        for (Field field : type.getDeclaredFields()) {
            if (field.getName().equals(fieldName)) {
                return true;
            }
        }
        return false;
    }
}
