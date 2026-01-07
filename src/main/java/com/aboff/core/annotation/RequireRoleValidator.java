package com.aboff.core.annotation;

import com.aboff.core.model.enums.Role;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RequireRoleValidator implements ConstraintValidator<RequireRole, Object> {
    
    private Role requiredRole;
    
    @Override
    public void initialize(RequireRole constraintAnnotation) {
        this.requiredRole = constraintAnnotation.value();
    }
    
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // This will be handled by the security interceptor, not validation
        return true;
    }
}
