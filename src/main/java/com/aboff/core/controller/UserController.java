package com.aboff.core.controller;

import com.aboff.core.annotation.RequireMinimumRole;
import com.aboff.core.model.dto.UserDto;
import com.aboff.core.model.enums.Role;
import com.aboff.core.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/me")
    public ResponseEntity<UserDto.Profile> getCurrentUserProfile(@AuthenticationPrincipal UserDetails userDetails) {
        // Extract user ID from security context (this will be implemented with JWT)
        String username = userDetails.getUsername();
        // For now, we'll need to find the user by username
        // In the full implementation, we'll extract UUID from JWT claims
        UUID userId = UUID.randomUUID(); // Placeholder - will be extracted from JWT
        
        UserDto.Profile profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }
    
    @GetMapping("/{username}")
    public ResponseEntity<UserDto.PublicProfile> getPublicProfile(@PathVariable String username) {
        UserDto.PublicProfile profile = userService.getPublicProfile(username);
        return ResponseEntity.ok(profile);
    }
    
    @PutMapping("/me")
    public ResponseEntity<UserDto.Profile> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserDto.UpdateProfileRequest request) {
        
        String username = userDetails.getUsername();
        UUID userId = UUID.randomUUID(); // Placeholder - will be extracted from JWT
        
        UserDto.Profile updatedProfile = userService.updateProfile(userId, request);
        return ResponseEntity.ok(updatedProfile);
    }
    
    @PostMapping("/integrations/link")
    public ResponseEntity<Void> linkIntegration(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String providerId,
            @RequestParam String email,
            @RequestParam String provider) {
        
        String username = userDetails.getUsername();
        UUID userId = UUID.randomUUID(); // Placeholder - will be extracted from JWT
        
        // Convert provider string to enum
        com.aboff.core.model.enums.OAuthProvider oauthProvider = 
            com.aboff.core.model.enums.OAuthProvider.valueOf(provider.toUpperCase());
        
        userService.linkIntegration(userId, providerId, email, oauthProvider);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/integrations/{provider}")
    public ResponseEntity<Void> unlinkIntegration(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String provider) {
        
        String username = userDetails.getUsername();
        UUID userId = UUID.randomUUID(); // Placeholder - will be extracted from JWT
        
        com.aboff.core.model.enums.OAuthProvider oauthProvider = 
            com.aboff.core.model.enums.OAuthProvider.valueOf(provider.toUpperCase());
        
        userService.unlinkIntegration(userId, oauthProvider);
        return ResponseEntity.ok().build();
    }
}
