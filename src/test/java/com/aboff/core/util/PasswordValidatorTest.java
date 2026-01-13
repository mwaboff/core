package com.aboff.core.util;

import com.aboff.core.exception.InvalidPasswordException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordValidatorTest {

    // ==================== MINIMUM LENGTH TESTS ====================

    @Test
    void validatePassword_MeetsMinLength_Passes() {
        // Arrange
        PasswordValidator validator = new PasswordValidator(8, false, false, false, false);

        // Act & Assert
        assertThatCode(() -> validator.validatePassword("12345678"))
                .doesNotThrowAnyException();
    }

    @Test
    void validatePassword_BelowMinLength_ThrowsException() {
        // Arrange
        PasswordValidator validator = new PasswordValidator(8, false, false, false, false);

        // Act & Assert
        assertThatThrownBy(() -> validator.validatePassword("1234567"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("Password must be at least 8 characters long");
    }

    @Test
    void validatePassword_NullPassword_ThrowsException() {
        // Arrange
        PasswordValidator validator = new PasswordValidator(8, false, false, false, false);

        // Act & Assert
        assertThatThrownBy(() -> validator.validatePassword(null))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("Password must be at least 8 characters long");
    }

    // ==================== UPPERCASE REQUIREMENT TESTS ====================

    @Test
    void validatePassword_RequireUppercase_WithUppercase_Passes() {
        // Arrange
        PasswordValidator validator = new PasswordValidator(8, true, false, false, false);

        // Act & Assert
        assertThatCode(() -> validator.validatePassword("Abcdefgh"))
                .doesNotThrowAnyException();
    }

    @Test
    void validatePassword_RequireUppercase_WithoutUppercase_ThrowsException() {
        // Arrange
        PasswordValidator validator = new PasswordValidator(8, true, false, false, false);

        // Act & Assert
        assertThatThrownBy(() -> validator.validatePassword("abcdefgh"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("Password must contain at least one uppercase letter");
    }

    // ==================== LOWERCASE REQUIREMENT TESTS ====================

    @Test
    void validatePassword_RequireLowercase_WithLowercase_Passes() {
        // Arrange
        PasswordValidator validator = new PasswordValidator(8, false, true, false, false);

        // Act & Assert
        assertThatCode(() -> validator.validatePassword("ABCDEFGh"))
                .doesNotThrowAnyException();
    }

    @Test
    void validatePassword_RequireLowercase_WithoutLowercase_ThrowsException() {
        // Arrange
        PasswordValidator validator = new PasswordValidator(8, false, true, false, false);

        // Act & Assert
        assertThatThrownBy(() -> validator.validatePassword("ABCDEFGH"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("Password must contain at least one lowercase letter");
    }

    // ==================== DIGIT REQUIREMENT TESTS ====================

    @Test
    void validatePassword_RequireDigit_WithDigit_Passes() {
        // Arrange
        PasswordValidator validator = new PasswordValidator(8, false, false, true, false);

        // Act & Assert
        assertThatCode(() -> validator.validatePassword("abcdefg1"))
                .doesNotThrowAnyException();
    }

    @Test
    void validatePassword_RequireDigit_WithoutDigit_ThrowsException() {
        // Arrange
        PasswordValidator validator = new PasswordValidator(8, false, false, true, false);

        // Act & Assert
        assertThatThrownBy(() -> validator.validatePassword("abcdefgh"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("Password must contain at least one digit");
    }

    // ==================== SPECIAL CHARACTER REQUIREMENT TESTS ====================

    @Test
    void validatePassword_RequireSpecial_WithSpecial_Passes() {
        // Arrange
        PasswordValidator validator = new PasswordValidator(8, false, false, false, true);

        // Act & Assert
        // Test multiple special characters
        assertThatCode(() -> validator.validatePassword("abcdefg!")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg@")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg#")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg$")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg%")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg^")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg&")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg*")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg(")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg)")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg_")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg+")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg-")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg=")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg[")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg]")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg{")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg}")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg;")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg:")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg'")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg\"")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg\\")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg|")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg,")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg.")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg<")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg>")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg/")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("abcdefg?")).doesNotThrowAnyException();
    }

    @Test
    void validatePassword_RequireSpecial_WithoutSpecial_ThrowsException() {
        // Arrange
        PasswordValidator validator = new PasswordValidator(8, false, false, false, true);

        // Act & Assert
        assertThatThrownBy(() -> validator.validatePassword("abcdefgh"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("Password must contain at least one special character");
    }

    // ==================== ALL REQUIREMENTS TESTS ====================

    @Test
    void validatePassword_AllRequirements_ValidPassword_Passes() {
        // Arrange
        PasswordValidator validator = new PasswordValidator(8, true, true, true, true);

        // Act & Assert
        assertThatCode(() -> validator.validatePassword("Password123!"))
                .doesNotThrowAnyException();
    }

    @Test
    void validatePassword_AllRequirements_MissingUppercase_ThrowsException() {
        // Arrange
        PasswordValidator validator = new PasswordValidator(8, true, true, true, true);

        // Act & Assert
        assertThatThrownBy(() -> validator.validatePassword("password123!"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("Password must contain at least one uppercase letter");
    }

    @Test
    void validatePassword_AllRequirements_MissingLowercase_ThrowsException() {
        // Arrange
        PasswordValidator validator = new PasswordValidator(8, true, true, true, true);

        // Act & Assert
        assertThatThrownBy(() -> validator.validatePassword("PASSWORD123!"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("Password must contain at least one lowercase letter");
    }

    @Test
    void validatePassword_AllRequirements_MissingDigit_ThrowsException() {
        // Arrange
        PasswordValidator validator = new PasswordValidator(8, true, true, true, true);

        // Act & Assert
        assertThatThrownBy(() -> validator.validatePassword("Password!!!"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("Password must contain at least one digit");
    }

    @Test
    void validatePassword_AllRequirements_MissingSpecial_ThrowsException() {
        // Arrange
        PasswordValidator validator = new PasswordValidator(8, true, true, true, true);

        // Act & Assert
        assertThatThrownBy(() -> validator.validatePassword("Password123"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("Password must contain at least one special character");
    }

    @Test
    void validatePassword_AllRequirements_TooShort_ThrowsException() {
        // Arrange
        PasswordValidator validator = new PasswordValidator(8, true, true, true, true);

        // Act & Assert
        assertThatThrownBy(() -> validator.validatePassword("Pass1!"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("Password must be at least 8 characters long");
    }

    // ==================== NO REQUIREMENTS TESTS ====================

    @Test
    void validatePassword_NoRequirements_AnyPasswordMeetsMinLength_Passes() {
        // Arrange
        PasswordValidator validator = new PasswordValidator(5, false, false, false, false);

        // Act & Assert
        assertThatCode(() -> validator.validatePassword("12345")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("aaaaa")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePassword("AAAAA")).doesNotThrowAnyException();
    }

    // ==================== EDGE CASES ====================

    @Test
    void validatePassword_ExactlyMinLength_Passes() {
        // Arrange
        PasswordValidator validator = new PasswordValidator(10, false, false, false, false);

        // Act & Assert
        assertThatCode(() -> validator.validatePassword("1234567890"))
                .doesNotThrowAnyException();
    }

    @Test
    void validatePassword_VeryLongPassword_Passes() {
        // Arrange
        PasswordValidator validator = new PasswordValidator(8, true, true, true, true);
        String longPassword = "Password123!" + "a".repeat(1000);

        // Act & Assert
        assertThatCode(() -> validator.validatePassword(longPassword))
                .doesNotThrowAnyException();
    }

    @Test
    void validatePassword_MultipleUppercaseDigitsSpecial_Passes() {
        // Arrange
        PasswordValidator validator = new PasswordValidator(8, true, true, true, true);

        // Act & Assert
        assertThatCode(() -> validator.validatePassword("Password123!!!"))
                .doesNotThrowAnyException();
    }
}
