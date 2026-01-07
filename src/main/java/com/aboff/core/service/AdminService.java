package com.aboff.core.service;

import com.aboff.core.exception.*;
import com.aboff.core.model.dto.AdminDto;
import com.aboff.core.model.entity.*;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.AdminLogRepository;
import com.aboff.core.repository.LoginHistoryRepository;
import com.aboff.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminService {
    
    private final UserRepository userRepository;
    private final AdminLogRepository adminLogRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    
    @Transactional(readOnly = true)
    public AdminDto.DetailedUserInfo getDetailedUserInfo(String adminUsername, String targetUsername) {
        User admin = userRepository.findByUsername(adminUsername)
            .orElseThrow(() -> new UserNotFoundException("Admin user not found: " + adminUsername));
        
        User target = userRepository.findByUsername(targetUsername)
            .orElseThrow(() -> new UserNotFoundException("Target user not found: " + targetUsername));
        
        // Check if admin can view this user's info
        if (!canViewUserInfo(admin, target)) {
            throw new InsufficientPermissionException("Admin cannot view user info for user with equal or higher role");
        }
        
        return new AdminDto.DetailedUserInfo(
            target.getId(),
            target.getUsername(),
            target.getEmail(),
            target.getDisplayName(),
            target.getAvatarUrl(),
            target.getTimezone(),
            target.getRole(),
            target.getActive(),
            target.getLastLoginAt(),
            target.getCreatedDate(),
            target.getLastUpdatedDate()
        );
    }
    
    @Transactional(readOnly = true)
    public Page<AdminDto.AdminLogEntry> getAdminLog(String adminUsername, Pageable pageable) {
        User admin = userRepository.findByUsername(adminUsername)
            .orElseThrow(() -> new UserNotFoundException("Admin user not found: " + adminUsername));
        
        if (admin.getRole().getLevel() < Role.ADMIN.getLevel()) {
            throw new InsufficientPermissionException("Only Admin and Owner can view admin logs");
        }
        
        Page<AdminLog> adminLogs = adminLogRepository.findAll(pageable);
        
        return adminLogs.map(log -> new AdminDto.AdminLogEntry(
            log.getId(),
            log.getAdminUser().getUsername(),
            log.getTargetUser() != null ? log.getTargetUser().getUsername() : null,
            log.getAction(),
            log.getDetails(),
            log.getIpAddress(),
            log.getPerformedAt()
        ));
    }
    
    @Transactional(readOnly = true)
    public Page<AdminDto.LoginHistoryEntry> getLoginHistory(String adminUsername, Pageable pageable) {
        User admin = userRepository.findByUsername(adminUsername)
            .orElseThrow(() -> new UserNotFoundException("Admin user not found: " + adminUsername));
        
        if (admin.getRole().getLevel() < Role.ADMIN.getLevel()) {
            throw new InsufficientPermissionException("Only Admin and Owner can view login history");
        }
        
        Page<LoginHistory> loginHistory = loginHistoryRepository.findAll(pageable);
        
        return loginHistory.map(history -> new AdminDto.LoginHistoryEntry(
            history.getId(),
            history.getUser() != null ? history.getUser().getUsername() : null,
            history.getEmail(),
            history.getSuccess(),
            history.getIpAddress(),
            history.getUserAgent(),
            history.getAttemptedAt()
        ));
    }
    
    public void banUser(String adminUsername, String targetUsername, AdminDto.BanUserRequest request, String ipAddress) {
        User admin = userRepository.findByUsername(adminUsername)
            .orElseThrow(() -> new UserNotFoundException("Admin user not found: " + adminUsername));
        
        User target = userRepository.findByUsername(targetUsername)
            .orElseThrow(() -> new UserNotFoundException("Target user not found: " + targetUsername));
        
        // Check if admin can ban this user
        if (!canBanUser(admin, target)) {
            throw new InsufficientPermissionException("Admin cannot ban user with equal or higher role");
        }
        
        boolean wasActive = target.getActive();
        target.setActive(!request.banned());
        userRepository.save(target);
        
        // Log the action
        String action = request.banned() ? "BAN_USER" : "UNBAN_USER";
        String details = request.reason() != null ? request.reason() : "No reason provided";
        
        logAdminAction(admin, target, action, details, ipAddress);
        
        log.info("Admin {} {} user {}", adminUsername, request.banned() ? "banned" : "unbanned", targetUsername);
    }
    
    public void changeUserRole(String adminUsername, String targetUsername, AdminDto.ChangeRoleRequest request, String ipAddress) {
        User admin = userRepository.findByUsername(adminUsername)
            .orElseThrow(() -> new UserNotFoundException("Admin user not found: " + adminUsername));
        
        User target = userRepository.findByUsername(targetUsername)
            .orElseThrow(() -> new UserNotFoundException("Target user not found: " + targetUsername));
        
        // Check if admin can change this user's role
        if (!canChangeRole(admin, target, request.role())) {
            throw new InsufficientPermissionException("Admin cannot change role for user with equal or higher role");
        }
        
        Role oldRole = target.getRole();
        target.setRole(request.role());
        userRepository.save(target);
        
        // Log the action
        String details = String.format("Changed role from %s to %s", oldRole, request.role());
        logAdminAction(admin, target, "CHANGE_ROLE", details, ipAddress);
        
        log.info("Admin {} changed role of user {} from {} to {}", adminUsername, targetUsername, oldRole, request.role());
    }
    
    private boolean canViewUserInfo(User admin, User target) {
        return admin.getRole().getLevel() >= Role.ADMIN.getLevel() && 
               admin.getRole().getLevel() > target.getRole().getLevel();
    }
    
    private boolean canBanUser(User admin, User target) {
        return admin.getRole().getLevel() > target.getRole().getLevel();
    }
    
    private boolean canChangeRole(User admin, User target, Role newRole) {
        // Admin can change roles of users with lower level
        // Cannot change role to equal or higher than admin's role
        // Cannot change Owner role at all
        if (target.getRole() == Role.OWNER) {
            return false;
        }
        
        return admin.getRole().getLevel() > target.getRole().getLevel() && 
               admin.getRole().getLevel() > newRole.getLevel() &&
               newRole != Role.OWNER;
    }
    
    private void logAdminAction(User adminUser, User targetUser, String action, String details, String ipAddress) {
        AdminLog adminLog = new AdminLog();
        adminLog.setAdminUser(adminUser);
        adminLog.setTargetUser(targetUser);
        adminLog.setAction(action);
        adminLog.setDetails(details);
        adminLog.setIpAddress(ipAddress);
        adminLog.setPerformedAt(LocalDateTime.now());
        
        adminLogRepository.save(adminLog);
    }
}
