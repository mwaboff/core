package com.aboff.core.annotation;

import com.aboff.core.model.enums.Role;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = RequireRoleValidator.class)
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    Role value();
    
    String message() default "User must have role: {value}";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
