package com.aboff.core.repository;

import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.UserIntegration;
import com.aboff.core.model.enums.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserIntegrationRepository extends JpaRepository<UserIntegration, UUID> {
    
    Optional<UserIntegration> findByUserAndProvider(User user, OAuthProvider provider);
    
    Optional<UserIntegration> findByProviderAndProviderId(OAuthProvider provider, String providerId);
    
    List<UserIntegration> findByUser(User user);
    
    @Query("SELECT ui FROM UserIntegration ui WHERE ui.provider = :provider")
    List<UserIntegration> findByProvider(@Param("provider") OAuthProvider provider);
    
    boolean existsByUserAndProvider(User user, OAuthProvider provider);
    
    boolean existsByProviderAndProviderId(OAuthProvider provider, String providerId);
}
