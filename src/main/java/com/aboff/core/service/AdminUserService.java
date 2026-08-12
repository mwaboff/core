package com.aboff.core.service;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.exception.UserAlreadyExistsException;
import com.aboff.core.exception.UserNotFoundException;
import com.aboff.core.model.dto.request.BanUserRequest;
import com.aboff.core.model.dto.request.UpdateAdminUserRequest;
import com.aboff.core.model.dto.response.AdminActionResponse;
import com.aboff.core.model.dto.response.AdminUserDetailResponse;
import com.aboff.core.model.dto.response.AdminUserSummaryResponse;
import com.aboff.core.model.dto.response.LoginEventResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.dto.response.UserIdentityResponse;
import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.model.dto.response.UsernameHistoryResponse;
import com.aboff.core.model.entity.AdminActionLog;
import com.aboff.core.model.entity.LoginEvent;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.UserIdentity;
import com.aboff.core.model.entity.UsernameHistory;
import com.aboff.core.model.enums.AdminActionType;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.AdminActionLogRepository;
import com.aboff.core.repository.LoginEventRepository;
import com.aboff.core.repository.UserIdentityRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.UsernameHistoryRepository;
import com.aboff.core.util.ExpandUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Vertical owner of all admin-initiated user read and mutation operations.
 * <p>
 * Centralises the role-hierarchy check, writes durable
 * {@link AdminActionLog} rows inside the same transaction as the state
 * change, and is the single call site for
 * {@link AuthenticationService#invalidateAllUserTokens(Long)} when a
 * ban or role change forces re-authentication.
 * </p>
 */
@Slf4j
@Service
public class AdminUserService {

    /** Hard cap on the admin list page size, per plan. */
    private static final int MAX_PAGE_SIZE = 100;

    /** Sort keys exposed to the admin list endpoint. */
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "createdAt", "lastSeenAt", "username");

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final LoginEventRepository loginEventRepository;
    private final UsernameHistoryRepository usernameHistoryRepository;
    private final AdminActionLogRepository adminActionLogRepository;
    private final AuthenticationService authenticationService;
    private final RoleHierarchyService roleHierarchyService;

    /**
     * Constructs a new {@code AdminUserService}.
     *
     * @param userRepository            user repository
     * @param userIdentityRepository    OAuth identity repository
     * @param loginEventRepository      login event repository
     * @param usernameHistoryRepository username history repository
     * @param adminActionLogRepository  admin action log repository
     * @param authenticationService     token invalidation
     * @param roleHierarchyService      role comparison
     */
    public AdminUserService(
            UserRepository userRepository,
            UserIdentityRepository userIdentityRepository,
            LoginEventRepository loginEventRepository,
            UsernameHistoryRepository usernameHistoryRepository,
            AdminActionLogRepository adminActionLogRepository,
            AuthenticationService authenticationService,
            RoleHierarchyService roleHierarchyService) {
        this.userRepository = userRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.loginEventRepository = loginEventRepository;
        this.usernameHistoryRepository = usernameHistoryRepository;
        this.adminActionLogRepository = adminActionLogRepository;
        this.authenticationService = authenticationService;
        this.roleHierarchyService = roleHierarchyService;
    }

    // ==================== LIST ====================

    /**
     * Lists non-deleted users with optional filters applied.
     *
     * @param isBanned filter by banned/not-banned; null skips
     * @param role     filter by role; null skips
     * @param username case-insensitive substring on username; null skips
     * @param email    case-insensitive substring on email; null skips
     * @param page     zero-based page number
     * @param size     page size; clamped to {@value #MAX_PAGE_SIZE}
     * @param sort     sort property; must be one of
     *                 {@link #ALLOWED_SORT_PROPERTIES}
     * @param ascending direction
     * @return paged summary view
     */
    @Transactional(readOnly = true)
    public PagedResponse<AdminUserSummaryResponse> listUsers(
            Boolean isBanned,
            Role role,
            String username,
            String email,
            int page,
            int size,
            String sort,
            boolean ascending) {

        if (page < 0) {
            throw new IllegalArgumentException("Page must be non-negative");
        }
        int effectiveSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        String effectiveSort = (sort == null || sort.isBlank()) ? "id" : sort;
        if (!ALLOWED_SORT_PROPERTIES.contains(effectiveSort)) {
            throw new IllegalArgumentException("Invalid sort property: " + effectiveSort);
        }

        Pageable pageable = PageRequest.of(page, effectiveSize,
                ascending ? Sort.by(effectiveSort).ascending() : Sort.by(effectiveSort).descending());

        Page<User> result = userRepository.findAllWithAdminFilters(
                isBanned, role, emptyToNull(username), emptyToNull(email), pageable);

        List<AdminUserSummaryResponse> content = result.getContent().stream()
                .map(this::toSummary)
                .toList();

        return PagedResponse.<AdminUserSummaryResponse>builder()
                .content(content)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .currentPage(result.getNumber())
                .pageSize(result.getSize())
                .build();
    }

    // ==================== DETAIL ====================

    /**
     * Returns the admin detail view of a single user.
     *
     * @param userId the user id
     * @param expand comma-separated expand list; see
     *               {@link com.aboff.core.util.ExpandUtil}
     * @return the detail response
     * @throws UserNotFoundException if the user does not exist
     */
    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(Long userId, String expand) {
        User user = findUserOrThrow(userId);
        Set<String> expandSet = ExpandUtil.parseExpand(expand);
        return buildDetail(user, expandSet);
    }

    // ==================== UPDATE (PATCH) ====================

    /**
     * Applies partial updates to a user's profile and role.
     * <p>
     * Each changed field writes a corresponding {@link AdminActionLog} row. A
     * role change additionally triggers a bulk token invalidation for the
     * target. All mutations happen inside the same transaction as the audit
     * rows, so a crash cannot leave the two out of sync.
     * </p>
     *
     * @param actor   the admin performing the update
     * @param userId  the target user's id
     * @param request the partial update
     * @param ipAddress originating ip, captured on audit rows
     * @return the refreshed detail response
     */
    @Transactional
    public AdminUserDetailResponse updateUser(
            User actor, Long userId, UpdateAdminUserRequest request, String ipAddress) {
        User target = findUserOrThrow(userId);
        roleHierarchyService.canModifyUser(actor, target);

        boolean roleChanged = false;

        if (request.getUsername() != null && !request.getUsername().equals(target.getUsername())) {
            if (userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
                throw new UserAlreadyExistsException("Username already taken");
            }
            String previous = target.getUsername();
            target.setUsername(request.getUsername());
            target.setUsernameChosen(true);

            usernameHistoryRepository.save(UsernameHistory.builder()
                    .userId(target.getId())
                    .previousUsername(previous)
                    .newUsername(request.getUsername())
                    .changedByUserId(actor.getId())
                    .build());

            recordAction(actor, target, AdminActionType.USER_USERNAME_CHANGED,
                    String.format("previous_username=%s; new_username=%s", previous, request.getUsername()),
                    ipAddress);
        }

        if (request.getAvatarUrl() != null && !Objects.equals(request.getAvatarUrl(), target.getAvatarUrl())) {
            String previous = target.getAvatarUrl();
            target.setAvatarUrl(request.getAvatarUrl());
            recordAction(actor, target, AdminActionType.USER_AVATAR_CHANGED,
                    String.format("previous_avatar_url=%s; new_avatar_url=%s", previous, request.getAvatarUrl()),
                    ipAddress);
        }

        if (request.getRole() != null && request.getRole() != target.getRole()) {
            // Unified rule: actor must be strictly higher than BOTH current and proposed role.
            if (!roleHierarchyService.isHigherRole(actor.getRole(), request.getRole())) {
                throw new InsufficientPermissionsException(
                        String.format("Cannot grant role %s", request.getRole()));
            }
            Role previous = target.getRole();
            target.setRole(request.getRole());
            roleChanged = true;
            recordAction(actor, target, AdminActionType.USER_ROLE_CHANGED,
                    String.format("previous_role=%s; new_role=%s", previous, request.getRole()),
                    ipAddress);
        }

        if (request.getAccessAllExpansions() != null
                && !request.getAccessAllExpansions().equals(target.getAccessAllExpansions())) {
            Boolean previous = target.getAccessAllExpansions();
            target.setAccessAllExpansions(request.getAccessAllExpansions());
            recordAction(actor, target, AdminActionType.USER_EXPANSION_ACCESS_CHANGED,
                    String.format("previous_access_all_expansions=%s; new_access_all_expansions=%s",
                            previous, request.getAccessAllExpansions()),
                    ipAddress);
        }

        userRepository.save(target);

        if (roleChanged) {
            authenticationService.invalidateAllUserTokens(target.getId());
        }

        return buildDetail(target, Set.of(ExpandUtil.EXPAND_ALL));
    }

    // ==================== BAN / UNBAN ====================

    /**
     * Bans a user and revokes all of their active tokens.
     *
     * @param actor   the admin performing the ban
     * @param userId  the target user's id
     * @param request optional reason wrapper; may be {@code null}
     * @param ipAddress originating ip, captured on audit rows
     * @return the refreshed detail response
     */
    @Transactional
    public AdminUserDetailResponse banUser(
            User actor, Long userId, BanUserRequest request, String ipAddress) {
        User target = findUserOrThrow(userId);
        roleHierarchyService.canModifyUser(actor, target);

        String reason = request != null ? request.getReason() : null;
        target.ban(reason);
        userRepository.save(target);

        authenticationService.invalidateAllUserTokens(target.getId());

        String details = reason != null ? "reason=" + reason : "reason=";
        recordAction(actor, target, AdminActionType.USER_BANNED, details, ipAddress);

        return buildDetail(target, Set.of(ExpandUtil.EXPAND_ALL));
    }

    /**
     * Unbans a user.
     *
     * @param actor     the admin performing the unban
     * @param userId    the target user's id
     * @param ipAddress originating ip, captured on audit rows
     * @return the refreshed detail response
     */
    @Transactional
    public AdminUserDetailResponse unbanUser(User actor, Long userId, String ipAddress) {
        User target = findUserOrThrow(userId);
        roleHierarchyService.canModifyUser(actor, target);

        target.unban();
        userRepository.save(target);

        recordAction(actor, target, AdminActionType.USER_UNBANNED, null, ipAddress);

        return buildDetail(target, Set.of(ExpandUtil.EXPAND_ALL));
    }

    /**
     * Changes a user's role. Enforces the unified role-change rule: the actor
     * must be strictly higher than both the target's current role and the
     * proposed role.
     *
     * @param actor     the admin performing the change
     * @param userId    the target user's id
     * @param newRole   the role to assign
     * @param ipAddress originating ip, captured on audit rows
     * @return the refreshed detail response
     */
    @Transactional
    public AdminUserDetailResponse changeRole(User actor, Long userId, Role newRole, String ipAddress) {
        UpdateAdminUserRequest r = UpdateAdminUserRequest.builder().role(newRole).build();
        return updateUser(actor, userId, r, ipAddress);
    }

    // ==================== CONTENT ACTIONS ====================

    /**
     * Records an admin action that targets game content rather than a specific user, e.g. the
     * bulk SRD-flagging tool.
     * <p>
     * Writes the same {@link AdminActionLog} row shape as {@link #recordAction}, but with a
     * null {@code targetUserId} — content actions have no user target. Public (unlike
     * {@link #recordAction}) so other admin services, e.g. {@code AdminContentService}, can
     * reuse this one write path instead of duplicating it.
     * </p>
     *
     * @param actor     the admin performing the action; may be {@code null}
     * @param action    the action type
     * @param details   free-form {@code key=value} description of what changed
     * @param ipAddress originating ip, captured on the audit row
     */
    @Transactional
    public void recordContentAction(User actor, AdminActionType action, String details, String ipAddress) {
        adminActionLogRepository.save(AdminActionLog.builder()
                .actorUserId(actor != null ? actor.getId() : null)
                .targetUserId(null)
                .action(action)
                .details(details)
                .ipAddress(ipAddress)
                .build());
    }

    // ==================== INTERNAL HELPERS ====================

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private void recordAction(
            User actor, User target, AdminActionType action, String details, String ipAddress) {
        adminActionLogRepository.save(AdminActionLog.builder()
                .actorUserId(actor != null ? actor.getId() : null)
                .targetUserId(target.getId())
                .action(action)
                .details(details)
                .ipAddress(ipAddress)
                .build());
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private AdminUserSummaryResponse toSummary(User u) {
        return AdminUserSummaryResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .avatarUrl(u.getAvatarUrl())
                .role(u.getRole())
                .banned(u.isBanned())
                .bannedAt(u.getBannedAt())
                .banReason(u.getBanReason())
                .createdAt(u.getCreatedAt())
                .lastSeenAt(u.getLastSeenAt())
                .build();
    }

    private UserResponse toPrivilegedUserResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .role(u.getRole())
                .email(u.getEmail())
                .avatarUrl(u.getAvatarUrl())
                .timezone(u.getTimezone())
                .createdAt(u.getCreatedAt())
                .lastModifiedAt(u.getLastModifiedAt())
                .usernameChosen(u.getUsernameChosen())
                .accessAllExpansions(u.getAccessAllExpansions())
                .deletedAt(u.getDeletedAt())
                .bannedAt(u.getBannedAt())
                .banReason(u.getBanReason())
                .lastSeenAt(u.getLastSeenAt())
                .build();
    }

    private AdminUserDetailResponse buildDetail(User user, Set<String> expandSet) {
        AdminUserDetailResponse.AdminUserDetailResponseBuilder builder = AdminUserDetailResponse.builder()
                .user(toPrivilegedUserResponse(user))
                .identities(loadIdentities(user.getId()));

        if (ExpandUtil.shouldExpand(expandSet, "loginEvents")) {
            builder.loginEvents(loadLoginEvents(user.getId()));
        }
        if (ExpandUtil.shouldExpand(expandSet, "usernameHistory")) {
            builder.usernameHistory(loadUsernameHistory(user.getId()));
        }
        if (ExpandUtil.shouldExpand(expandSet, "adminActions")) {
            builder.adminActions(loadAdminActions(user.getId()));
        }
        return builder.build();
    }

    private List<UserIdentityResponse> loadIdentities(Long userId) {
        List<UserIdentity> rows = userIdentityRepository.findByUserId(userId);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream()
                .map(i -> UserIdentityResponse.builder()
                        .provider(i.getProvider())
                        .displayName(i.getDisplayName())
                        .linkedAt(i.getLinkedAt())
                        .lastUsedAt(i.getLastUsedAt())
                        .build())
                .toList();
    }

    private List<LoginEventResponse> loadLoginEvents(Long userId) {
        Pageable p = PageRequest.of(0, 50);
        return loginEventRepository.findByUserIdOrderByCreatedAtDesc(userId, p)
                .map(e -> LoginEventResponse.builder()
                        .id(e.getId())
                        .provider(e.getProvider())
                        .ipAddress(e.getIpAddress())
                        .deviceInfo(e.getDeviceInfo())
                        .createdAt(e.getCreatedAt())
                        .build())
                .getContent();
    }

    private List<UsernameHistoryResponse> loadUsernameHistory(Long userId) {
        Pageable p = PageRequest.of(0, 50);
        Page<UsernameHistory> page = usernameHistoryRepository.findByUserIdOrderByChangedAtDesc(userId, p);
        if (page.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> usernames = resolveUsernames(
                page.stream().map(UsernameHistory::getChangedByUserId).toList());

        return page.map(h -> UsernameHistoryResponse.builder()
                .previousUsername(h.getPreviousUsername())
                .newUsername(h.getNewUsername())
                .changedByUserId(h.getChangedByUserId())
                .changedByUsername(h.getChangedByUserId() == null ? null : usernames.get(h.getChangedByUserId()))
                .changedAt(h.getChangedAt())
                .build()).getContent();
    }

    private List<AdminActionResponse> loadAdminActions(Long userId) {
        Pageable p = PageRequest.of(0, 50);
        Page<AdminActionLog> page = adminActionLogRepository.findByTargetUserIdOrderByCreatedAtDesc(userId, p);
        if (page.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> usernames = resolveUsernames(
                page.stream().map(AdminActionLog::getActorUserId).toList());

        return page.map(a -> AdminActionResponse.builder()
                .id(a.getId())
                .actorUserId(a.getActorUserId())
                .actorUsername(a.getActorUserId() == null ? null : usernames.get(a.getActorUserId()))
                .action(a.getAction())
                .details(a.getDetails())
                .ipAddress(a.getIpAddress())
                .createdAt(a.getCreatedAt())
                .build()).getContent();
    }

    private Map<Long, String> resolveUsernames(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRepository.findAllById(distinct).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername,
                        (a, b) -> a, java.util.HashMap::new));
    }
}
