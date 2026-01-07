package com.aboff.core.service;

import com.aboff.core.config.DefaultsProperties;
import com.aboff.core.exception.*;
import com.aboff.core.model.dto.UserDto;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.UserIntegration;
import com.aboff.core.model.enums.OAuthProvider;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.UserIntegrationRepository;
import com.aboff.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final UserIntegrationRepository userIntegrationRepository;
    private final DefaultsProperties defaultsProperties;
    
    @Transactional(readOnly = true)
    public UserDto.Profile getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        
        return new UserDto.Profile(
            user.getId(),
            user.getUsername(),
            user.getDisplayName(),
            user.getAvatarUrl() != null ? user.getAvatarUrl() : defaultsProperties.getDefaultAvatarUrl(),
            user.getTimezone(),
            user.getRole(),
            user.getActive(),
            user.getLastLoginAt()
        );
    }
    
    @Transactional(readOnly = true)
    public UserDto.PublicProfile getPublicProfile(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        
        return new UserDto.PublicProfile(
            user.getId(),
            user.getDisplayName(),
            user.getRole()
        );
    }
    
    public UserDto.Profile updateProfile(UUID userId, UserDto.UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        
        // Check if display name is already taken by another user
        if (request.displayName() != null && !request.displayName().equals(user.getDisplayName())) {
            if (userRepository.existsByDisplayName(request.displayName())) {
                throw new DuplicateResourceException("Display name already taken: " + request.displayName());
            }
            user.setDisplayName(request.displayName());
        }
        
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        
        if (request.timezone() != null) {
            user.setTimezone(request.timezone());
        }
        
        User updatedUser = userRepository.save(user);
        
        return new UserDto.Profile(
            updatedUser.getId(),
            updatedUser.getUsername(),
            updatedUser.getDisplayName(),
            updatedUser.getAvatarUrl() != null ? updatedUser.getAvatarUrl() : defaultsProperties.getDefaultAvatarUrl(),
            updatedUser.getTimezone(),
            updatedUser.getRole(),
            updatedUser.getActive(),
            updatedUser.getLastLoginAt()
        );
    }
    
    public User createOrUpdateUserFromOAuth(String providerId, String email, String displayName, OAuthProvider provider) {
        // First check if user integration already exists
        Optional<UserIntegration> existingIntegration = userIntegrationRepository.findByProviderAndProviderId(provider, providerId);
        
        if (existingIntegration.isPresent()) {
            User user = existingIntegration.get().getUser();
            updateLastLogin(user);
            return user;
        }
        
        // Check if user with email already exists
        Optional<User> userByEmail = userRepository.findByEmail(email);
        if (userByEmail.isPresent()) {
            User user = userByEmail.get();
            
            // Link new integration to existing user
            UserIntegration newIntegration = new UserIntegration();
            newIntegration.setUser(user);
            newIntegration.setProvider(provider);
            newIntegration.setProviderId(providerId);
            newIntegration.setProviderEmail(email);
            newIntegration.setLinkedAt(LocalDateTime.now());
            userIntegrationRepository.save(newIntegration);
            
            updateLastLogin(user);
            return user;
        }
        
        // Create new user
        User newUser = new User();
        newUser.setUsername(generateUniqueUsername(email));
        newUser.setEmail(email);
        newUser.setDisplayName(displayName);
        newUser.setRole(Role.USER);
        newUser.setActive(true);
        newUser.setLastLoginAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(newUser);
        
        // Create integration
        UserIntegration integration = new UserIntegration();
        integration.setUser(savedUser);
        integration.setProvider(provider);
        integration.setProviderId(providerId);
        integration.setProviderEmail(email);
        integration.setLinkedAt(LocalDateTime.now());
        userIntegrationRepository.save(integration);
        
        return savedUser;
    }
    
    public void linkIntegration(UUID userId, String providerId, String email, OAuthProvider provider) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        
        if (userIntegrationRepository.existsByUserAndProvider(user, provider)) {
            throw new DuplicateResourceException("Integration already exists for provider: " + provider);
        }
        
        if (userIntegrationRepository.existsByProviderAndProviderId(provider, providerId)) {
            throw new DuplicateResourceException("Provider ID already linked to another account");
        }
        
        UserIntegration integration = new UserIntegration();
        integration.setUser(user);
        integration.setProvider(provider);
        integration.setProviderId(providerId);
        integration.setProviderEmail(email);
        integration.setLinkedAt(LocalDateTime.now());
        userIntegrationRepository.save(integration);
    }
    
    public void unlinkIntegration(UUID userId, OAuthProvider provider) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        
        UserIntegration integration = userIntegrationRepository.findByUserAndProvider(user, provider)
            .orElseThrow(() -> new ResourceNotFoundException("Integration not found for provider: " + provider));
        
        // Check if user has other integrations
        long integrationCount = userIntegrationRepository.findByUser(user).size();
        if (integrationCount <= 1) {
            throw new BusinessException("Cannot unlink last integration. User must have at least one login method.");
        }
        
        userIntegrationRepository.delete(integration);
    }
    
    private void updateLastLogin(User user) {
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }
    
    private String generateUniqueUsername(String email) {
        String baseUsername = email.split("@")[0];
        String username = baseUsername;
        int counter = 1;
        
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }
        
        return username;
    }
}
