package com.example.IdentityManagementService.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) //annotation only applied to methods
@Retention(RetentionPolicy.RUNTIME) //make annotation info available at runtime
public @interface RequiresKeycloakAuthorization {
    String resource();
    String scope();
}
