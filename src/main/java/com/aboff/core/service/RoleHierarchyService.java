package com.aboff.core.service;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.Role;
import org.springframework.stereotype.Service;

/**
 * Service for managing role hierarchy and permission validation.
 * Provides methods to compare roles and enforce role-based access control.
 */
@Service
public class RoleHierarchyService {

    /**
     * Checks if the actor has a higher role than the target in the hierarchy.
     * Role hierarchy: OWNER > ADMIN > MODERATOR > USER
     *
     * @param actor  the role of the user performing the action
     * @param target the role of the user being acted upon
     * @return true if actor has a higher role than target, false otherwise
     */
    public boolean isHigherRole(Role actor, Role target) {
        return actor.ordinal() < target.ordinal();
    }

    /**
     * Validates that the actor can modify the target user.
     * An actor can only modify users with a lower role in the hierarchy.
     *
     * @param actor  the user attempting to perform the action
     * @param target the user being modified
     * @throws InsufficientPermissionsException if the actor's role is not higher
     *                                          than the target's role
     */
    public void canModifyUser(User actor, User target) {
        if (!isHigherRole(actor.getRole(), target.getRole())) {
            throw new InsufficientPermissionsException(
                    String.format("Cannot modify user with role %s", target.getRole()));
        }
    }

    /**
     * Validates that the user has at least the minimum required role.
     * Throws an exception if the user's role is lower than the required role.
     *
     * @param user        the user to check
     * @param minimumRole the minimum role required
     * @throws InsufficientPermissionsException if the user's role is insufficient
     */
    public void requireRoleOrHigher(User user, Role minimumRole) {
        if (user.getRole().ordinal() > minimumRole.ordinal()) {
            throw new InsufficientPermissionsException(
                    String.format("Requires at least %s role", minimumRole));
        }
    }

    /**
     * Checks if the user has at least the minimum required role.
     * Returns true if the user's role is equal to or higher than the minimum
     * role.
     *
     * @param user        the user to check
     * @param minimumRole the minimum role required
     * @return true if the user has the required role or higher, false otherwise
     */
    public boolean hasRoleOrHigher(User user, Role minimumRole) {
        return user.getRole().ordinal() <= minimumRole.ordinal();
    }

    /**
     * Checks if a role is considered privileged (OWNER, ADMIN, or MODERATOR).
     *
     * @param role the role to check
     * @return true if the role is privileged, false otherwise
     */
    public boolean isPrivilegedRole(Role role) {
        return role == Role.OWNER || role == Role.ADMIN || role == Role.MODERATOR;
    }
}
