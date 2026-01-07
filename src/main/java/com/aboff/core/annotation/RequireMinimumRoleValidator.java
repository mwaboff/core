package com.aboff.core.annotation;

import com.aboff.core.model.enums.Role;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RequireMinimumRoleValidator implements ConstraintValidator<RequireMinimumRole, Object> {
    
    private Role minimumRole;
    
    @Override
    public void initialize(RequireMinimumRole constraintAnnotation) {
        this.minimumRole = constraintAnnotation.value();
    }
    
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // This will be handled by the security interceptor, not validation
        return true;
    }
}
