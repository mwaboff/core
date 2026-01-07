package com.aboff.core.controller;

import com.aboff.core.annotation.RequireMinimumRole;
import com.aboff.core.model.dto.AdminDto;
import com.aboff.core.model.enums.Role;
import com.aboff.core.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    
    private final AdminService adminService;
    
    @GetMapping("/users/{username}")
    @RequireMinimumRole(Role.MODERATOR)
    public ResponseEntity<AdminDto.DetailedUserInfo> getDetailedUserInfo(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String username) {
        
        String adminUsername = userDetails.getUsername();
        AdminDto.DetailedUserInfo userInfo = adminService.getDetailedUserInfo(adminUsername, username);
        return ResponseEntity.ok(userInfo);
    }
    
    @GetMapping("/admin-log")
    @RequireMinimumRole(Role.ADMIN)
    public ResponseEntity<Page<AdminDto.AdminLogEntry>> getAdminLog(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        
        String adminUsername = userDetails.getUsername();
        Page<AdminDto.AdminLogEntry> adminLog = adminService.getAdminLog(adminUsername, pageable);
        return ResponseEntity.ok(adminLog);
    }
    
    @GetMapping("/login-history")
    @RequireMinimumRole(Role.ADMIN)
    public ResponseEntity<Page<AdminDto.LoginHistoryEntry>> getLoginHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        
        String adminUsername = userDetails.getUsername();
        Page<AdminDto.LoginHistoryEntry> loginHistory = adminService.getLoginHistory(adminUsername, pageable);
        return ResponseEntity.ok(loginHistory);
    }
    
    @PostMapping("/users/{username}/ban")
    @RequireMinimumRole(Role.MODERATOR)
    public ResponseEntity<Void> banUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String username,
            @Valid @RequestBody AdminDto.BanUserRequest request,
            HttpServletRequest httpRequest) {
        
        String adminUsername = userDetails.getUsername();
        String ipAddress = getClientIpAddress(httpRequest);
        
        adminService.banUser(adminUsername, username, request, ipAddress);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/users/{username}/role")
    @RequireMinimumRole(Role.ADMIN)
    public ResponseEntity<Void> changeUserRole(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String username,
            @Valid @RequestBody AdminDto.ChangeRoleRequest request,
            HttpServletRequest httpRequest) {
        
        String adminUsername = userDetails.getUsername();
        String ipAddress = getClientIpAddress(httpRequest);
        
        adminService.changeUserRole(adminUsername, username, request, ipAddress);
        return ResponseEntity.ok().build();
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
