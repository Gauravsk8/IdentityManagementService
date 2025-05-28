package com.example.identitymanagementservice.dto.request;


import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserRoleAssignRequestDto {

    @NotEmpty(message = "employeeCode is required")
    private String employeeCode;

    @NotEmpty(message = "At least one role must be provided")
    private List<String> roles;
}


