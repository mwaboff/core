package com.aboff.core.repository;

import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByDisplayName(String displayName);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    boolean existsByDisplayName(String displayName);
    
    @Query("SELECT u FROM User u WHERE u.role = :role")
    Page<User> findByRole(@Param("role") Role role, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.active = :active")
    Page<User> findByActive(@Param("active") Boolean active, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.active = :active")
    Page<User> findByRoleAndActive(@Param("role") Role role, @Param("active") Boolean active, Pageable pageable);
}
