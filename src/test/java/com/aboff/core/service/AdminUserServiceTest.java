package com.aboff.core.service;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.exception.UserAlreadyExistsException;
import com.aboff.core.model.dto.request.BanUserRequest;
import com.aboff.core.model.dto.request.UpdateAdminUserRequest;
import com.aboff.core.model.entity.AdminActionLog;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.UsernameHistory;
import com.aboff.core.model.enums.AdminActionType;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.AdminActionLogRepository;
import com.aboff.core.repository.LoginEventRepository;
import com.aboff.core.repository.UserIdentityRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.UsernameHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminUserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserIdentityRepository userIdentityRepository;
    @Mock private LoginEventRepository loginEventRepository;
    @Mock private UsernameHistoryRepository usernameHistoryRepository;
    @Mock private AdminActionLogRepository adminActionLogRepository;
    @Mock private AuthenticationService authenticationService;

    private RoleHierarchyService roleHierarchyService;
    private AdminUserService service;

    @BeforeEach
    void setUp() {
        roleHierarchyService = new RoleHierarchyService();
        service = new AdminUserService(
                userRepository, userIdentityRepository, loginEventRepository,
                usernameHistoryRepository, adminActionLogRepository,
                authenticationService, roleHierarchyService);

        // Stub default empty returns for detail-building queries so tests that
        // don't care about them don't NPE. Marked lenient — some tests never
        // reach buildDetail and shouldn't fail on unused stubs.
        lenient().when(userIdentityRepository.findByUserId(anyLong())).thenReturn(List.of());
        lenient().when(loginEventRepository.findByUserIdOrderByCreatedAtDesc(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        lenient().when(usernameHistoryRepository.findByUserIdOrderByChangedAtDesc(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        lenient().when(adminActionLogRepository.findByTargetUserIdOrderByCreatedAtDesc(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
    }

    private User userWith(Long id, Role role) {
        return User.builder().id(id).username("u" + id).role(role).build();
    }

    @Test
    void updateUser_UsernameChange_WritesHistoryAndAuditRow() {
        User actor = userWith(1L, Role.ADMIN);
        User target = userWith(2L, Role.USER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.existsByUsernameIgnoreCase("new")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateUser(actor, 2L,
                UpdateAdminUserRequest.builder().username("new").build(), "10.0.0.1");

        ArgumentCaptor<UsernameHistory> hist = ArgumentCaptor.forClass(UsernameHistory.class);
        verify(usernameHistoryRepository).save(hist.capture());
        assertThat(hist.getValue().getChangedByUserId()).isEqualTo(1L);

        ArgumentCaptor<AdminActionLog> log = ArgumentCaptor.forClass(AdminActionLog.class);
        verify(adminActionLogRepository).save(log.capture());
        assertThat(log.getValue().getAction()).isEqualTo(AdminActionType.USER_USERNAME_CHANGED);
    }

    @Test
    void updateUser_UsernameCollision_Throws() {
        User actor = userWith(1L, Role.ADMIN);
        User target = userWith(2L, Role.USER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.existsByUsernameIgnoreCase("taken")).thenReturn(true);

        assertThatThrownBy(() -> service.updateUser(actor, 2L,
                UpdateAdminUserRequest.builder().username("taken").build(), null))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void updateUser_AdminCannotGrantAdmin() {
        User actor = userWith(1L, Role.ADMIN);
        User target = userWith(2L, Role.USER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.updateUser(actor, 2L,
                UpdateAdminUserRequest.builder().role(Role.ADMIN).build(), null))
                .isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void updateUser_OwnerCanGrantAdminAndInvalidatesTokens() {
        User actor = userWith(1L, Role.OWNER);
        User target = userWith(2L, Role.USER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateUser(actor, 2L,
                UpdateAdminUserRequest.builder().role(Role.ADMIN).build(), null);

        verify(authenticationService).invalidateAllUserTokens(2L);
        ArgumentCaptor<AdminActionLog> log = ArgumentCaptor.forClass(AdminActionLog.class);
        verify(adminActionLogRepository).save(log.capture());
        assertThat(log.getValue().getAction()).isEqualTo(AdminActionType.USER_ROLE_CHANGED);
    }

    @Test
    void updateUser_AdminCannotModifyAdmin() {
        User actor = userWith(1L, Role.ADMIN);
        User target = userWith(2L, Role.ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.updateUser(actor, 2L,
                UpdateAdminUserRequest.builder().username("x").build(), null))
                .isInstanceOf(InsufficientPermissionsException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void banUser_WithReason_PopulatesReasonAndRevokesTokens() {
        User actor = userWith(1L, Role.ADMIN);
        User target = userWith(2L, Role.USER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.banUser(actor, 2L,
                BanUserRequest.builder().reason("spam").build(), "1.1.1.1");

        assertThat(target.isBanned()).isTrue();
        assertThat(target.getBanReason()).isEqualTo("spam");
        verify(authenticationService).invalidateAllUserTokens(2L);
    }

    @Test
    void listUsers_InvalidSort_Throws() {
        assertThatThrownBy(() -> service.listUsers(
                null, null, null, null, 0, 50, "drop_table", false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listUsers_SizeClampedToMax() {
        when(userRepository.findAllWithAdminFilters(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        service.listUsers(null, null, null, null, 0, 999, "id", true);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAllWithAdminFilters(eq(null), eq(null), eq(null), eq(null), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }
}
