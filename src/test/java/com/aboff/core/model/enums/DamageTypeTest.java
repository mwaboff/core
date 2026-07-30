package com.aboff.core.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DamageTypeTest {

    // ==================== CODE TESTS ====================

    @Test
    void getCode_Physical_ReturnsPhy() {
        assertThat(DamageType.PHYSICAL.getCode()).isEqualTo("phy");
    }

    @Test
    void getCode_Magic_ReturnsMag() {
        assertThat(DamageType.MAGIC.getCode()).isEqualTo("mag");
    }

    @Test
    void getCode_PhysicalAndMagic_ReturnsPhySlashMag() {
        assertThat(DamageType.PHYSICAL_AND_MAGIC.getCode()).isEqualTo("phy/mag");
    }

    // ==================== FROM CODE TESTS ====================

    @Test
    void fromCode_LowercasePhy_ReturnsPhysical() {
        assertThat(DamageType.fromCode("phy")).isEqualTo(DamageType.PHYSICAL);
    }

    @Test
    void fromCode_LowercaseMag_ReturnsMagic() {
        assertThat(DamageType.fromCode("mag")).isEqualTo(DamageType.MAGIC);
    }

    @Test
    void fromCode_UppercasePHY_ReturnsPhysical() {
        assertThat(DamageType.fromCode("PHY")).isEqualTo(DamageType.PHYSICAL);
    }

    @Test
    void fromCode_UppercaseMAG_ReturnsMagic() {
        assertThat(DamageType.fromCode("MAG")).isEqualTo(DamageType.MAGIC);
    }

    @Test
    void fromCode_MixedCasePhy_ReturnsPhysical() {
        assertThat(DamageType.fromCode("Phy")).isEqualTo(DamageType.PHYSICAL);
    }

    @Test
    void fromCode_WithWhitespace_TrimsAndReturnsCorrectType() {
        assertThat(DamageType.fromCode("  mag  ")).isEqualTo(DamageType.MAGIC);
    }

    @Test
    void fromCode_PhySlashMag_ReturnsPhysicalAndMagic() {
        assertThat(DamageType.fromCode("phy/mag")).isEqualTo(DamageType.PHYSICAL_AND_MAGIC);
    }

    @Test
    void fromCode_UppercasePhySlashMag_ReturnsPhysicalAndMagic() {
        assertThat(DamageType.fromCode("PHY/MAG")).isEqualTo(DamageType.PHYSICAL_AND_MAGIC);
    }

    // ==================== ERROR CASES ====================

    @Test
    void fromCode_NullCode_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> DamageType.fromCode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Damage code cannot be null or blank");
    }

    @Test
    void fromCode_EmptyString_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> DamageType.fromCode(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Damage code cannot be null or blank");
    }

    @Test
    void fromCode_BlankString_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> DamageType.fromCode("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Damage code cannot be null or blank");
    }

    @Test
    void fromCode_InvalidDamageType_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> DamageType.fromCode("fire"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown damage type: fire");
    }

    @Test
    void fromCode_PhysicalFullWord_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> DamageType.fromCode("physical"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown damage type: physical");
    }

    @Test
    void fromCode_MagicFullWord_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> DamageType.fromCode("magic"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown damage type: magic");
    }
}
