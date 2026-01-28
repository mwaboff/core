package com.aboff.core.service;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.Role;
import com.aboff.core.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class RoleHierarchyServiceTest {

    @InjectMocks
    private RoleHierarchyService roleHierarchyService;

    // ==================== IS_HIGHER_ROLE TESTS ====================

    @Test
    void isHigherRole_OwnerVsAdmin_ReturnsTrue() {
        // Act
        boolean result = roleHierarchyService.isHigherRole(Role.OWNER, Role.ADMIN);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isHigherRole_OwnerVsModerator_ReturnsTrue() {
        // Act
        boolean result = roleHierarchyService.isHigherRole(Role.OWNER, Role.MODERATOR);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isHigherRole_OwnerVsUser_ReturnsTrue() {
        // Act
        boolean result = roleHierarchyService.isHigherRole(Role.OWNER, Role.USER);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isHigherRole_AdminVsModerator_ReturnsTrue() {
        // Act
        boolean result = roleHierarchyService.isHigherRole(Role.ADMIN, Role.MODERATOR);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isHigherRole_AdminVsUser_ReturnsTrue() {
        // Act
        boolean result = roleHierarchyService.isHigherRole(Role.ADMIN, Role.USER);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isHigherRole_ModeratorVsUser_ReturnsTrue() {
        // Act
        boolean result = roleHierarchyService.isHigherRole(Role.MODERATOR, Role.USER);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isHigherRole_SameRole_ReturnsFalse() {
        // Act & Assert
        assertThat(roleHierarchyService.isHigherRole(Role.OWNER, Role.OWNER)).isFalse();
        assertThat(roleHierarchyService.isHigherRole(Role.ADMIN, Role.ADMIN)).isFalse();
        assertThat(roleHierarchyService.isHigherRole(Role.MODERATOR, Role.MODERATOR)).isFalse();
        assertThat(roleHierarchyService.isHigherRole(Role.USER, Role.USER)).isFalse();
    }

    @Test
    void isHigherRole_LowerRole_ReturnsFalse() {
        // Act & Assert
        assertThat(roleHierarchyService.isHigherRole(Role.ADMIN, Role.OWNER)).isFalse();
        assertThat(roleHierarchyService.isHigherRole(Role.MODERATOR, Role.ADMIN)).isFalse();
        assertThat(roleHierarchyService.isHigherRole(Role.USER, Role.MODERATOR)).isFalse();
    }

    // ==================== CAN_MODIFY_USER TESTS ====================

    @Test
    void canModifyUser_HigherRole_DoesNotThrow() {
        // Arrange
        User owner = User.builder().id(1L).role(Role.OWNER).build();
        User admin = User.builder().id(2L).role(Role.ADMIN).build();

        // Act & Assert - should not throw
        roleHierarchyService.canModifyUser(owner, admin);
    }

    @Test
    void canModifyUser_SameRole_ThrowsException() {
        // Arrange
        User admin1 = User.builder().id(1L).role(Role.ADMIN).build();
        User admin2 = User.builder().id(2L).role(Role.ADMIN).build();

        // Act & Assert
        assertThatThrownBy(() -> roleHierarchyService.canModifyUser(admin1, admin2))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessageContaining("Cannot modify user with role ADMIN");
    }

    @Test
    void canModifyUser_LowerRole_ThrowsException() {
        // Arrange
        User moderator = User.builder().id(1L).role(Role.MODERATOR).build();
        User admin = User.builder().id(2L).role(Role.ADMIN).build();

        // Act & Assert
        assertThatThrownBy(() -> roleHierarchyService.canModifyUser(moderator, admin))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessageContaining("Cannot modify user with role ADMIN");
    }

    // ==================== REQUIRE_ROLE_OR_HIGHER TESTS ====================

    @Test
    void requireRoleOrHigher_HasExactRole_DoesNotThrow() {
        // Arrange
        User admin = User.builder().id(1L).role(Role.ADMIN).build();

        // Act & Assert - should not throw
        roleHierarchyService.requireRoleOrHigher(admin, Role.ADMIN);
    }

    @Test
    void requireRoleOrHigher_HasHigherRole_DoesNotThrow() {
        // Arrange
        User owner = User.builder().id(1L).role(Role.OWNER).build();

        // Act & Assert - should not throw
        roleHierarchyService.requireRoleOrHigher(owner, Role.ADMIN);
    }

    @Test
    void requireRoleOrHigher_LacksRole_ThrowsException() {
        // Arrange
        User moderator = User.builder().id(1L).role(Role.MODERATOR).build();

        // Act & Assert
        assertThatThrownBy(() -> roleHierarchyService.requireRoleOrHigher(moderator, Role.ADMIN))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessageContaining("Requires at least ADMIN role");
    }

    // ==================== HAS_ROLE_OR_HIGHER TESTS ====================

    @Test
    void hasRoleOrHigher_HasExactRole_ReturnsTrue() {
        // Arrange
        User admin = User.builder().id(1L).role(Role.ADMIN).build();

        // Act
        boolean result = roleHierarchyService.hasRoleOrHigher(admin, Role.ADMIN);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void hasRoleOrHigher_HasHigherRole_ReturnsTrue() {
        // Arrange
        User owner = User.builder().id(1L).role(Role.OWNER).build();

        // Act
        boolean result = roleHierarchyService.hasRoleOrHigher(owner, Role.ADMIN);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void hasRoleOrHigher_LacksRole_ReturnsFalse() {
        // Arrange
        User moderator = User.builder().id(1L).role(Role.MODERATOR).build();

        // Act
        boolean result = roleHierarchyService.hasRoleOrHigher(moderator, Role.ADMIN);

        // Assert
        assertThat(result).isFalse();
    }

    // ==================== IS_PRIVILEGED_ROLE TESTS ====================

    @Test
    void isPrivilegedRole_Owner_ReturnsTrue() {
        assertThat(roleHierarchyService.isPrivilegedRole(Role.OWNER)).isTrue();
    }

    @Test
    void isPrivilegedRole_Admin_ReturnsTrue() {
        assertThat(roleHierarchyService.isPrivilegedRole(Role.ADMIN)).isTrue();
    }

    @Test
    void isPrivilegedRole_Moderator_ReturnsTrue() {
        assertThat(roleHierarchyService.isPrivilegedRole(Role.MODERATOR)).isTrue();
    }

    @Test
    void isPrivilegedRole_User_ReturnsFalse() {
        assertThat(roleHierarchyService.isPrivilegedRole(Role.USER)).isFalse();
    }

    // ==================== HAS_MODERATOR_OR_HIGHER (User) TESTS ====================

    @Test
    void hasModeratorOrHigher_User_Owner_ReturnsTrue() {
        // Arrange
        User owner = User.builder().id(1L).role(Role.OWNER).build();

        // Act
        boolean result = roleHierarchyService.hasModeratorOrHigher(owner);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void hasModeratorOrHigher_User_Admin_ReturnsTrue() {
        // Arrange
        User admin = User.builder().id(1L).role(Role.ADMIN).build();

        // Act
        boolean result = roleHierarchyService.hasModeratorOrHigher(admin);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void hasModeratorOrHigher_User_Moderator_ReturnsTrue() {
        // Arrange
        User moderator = User.builder().id(1L).role(Role.MODERATOR).build();

        // Act
        boolean result = roleHierarchyService.hasModeratorOrHigher(moderator);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void hasModeratorOrHigher_User_RegularUser_ReturnsFalse() {
        // Arrange
        User user = User.builder().id(1L).role(Role.USER).build();

        // Act
        boolean result = roleHierarchyService.hasModeratorOrHigher(user);

        // Assert
        assertThat(result).isFalse();
    }

    // ==================== HAS_MODERATOR_OR_HIGHER (CustomUserDetails) TESTS ====================

    @Test
    void hasModeratorOrHigher_CustomUserDetails_Owner_ReturnsTrue() {
        // Arrange
        User owner = User.builder().id(1L).role(Role.OWNER).build();
        CustomUserDetails userDetails = new CustomUserDetails(owner);

        // Act
        boolean result = roleHierarchyService.hasModeratorOrHigher(userDetails);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void hasModeratorOrHigher_CustomUserDetails_Admin_ReturnsTrue() {
        // Arrange
        User admin = User.builder().id(1L).role(Role.ADMIN).build();
        CustomUserDetails userDetails = new CustomUserDetails(admin);

        // Act
        boolean result = roleHierarchyService.hasModeratorOrHigher(userDetails);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void hasModeratorOrHigher_CustomUserDetails_Moderator_ReturnsTrue() {
        // Arrange
        User moderator = User.builder().id(1L).role(Role.MODERATOR).build();
        CustomUserDetails userDetails = new CustomUserDetails(moderator);

        // Act
        boolean result = roleHierarchyService.hasModeratorOrHigher(userDetails);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void hasModeratorOrHigher_CustomUserDetails_RegularUser_ReturnsFalse() {
        // Arrange
        User user = User.builder().id(1L).role(Role.USER).build();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        // Act
        boolean result = roleHierarchyService.hasModeratorOrHigher(userDetails);

        // Assert
        assertThat(result).isFalse();
    }
}
