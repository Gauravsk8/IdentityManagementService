package com.example.IdentityManagementService.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class UserRoleUpdateRequestDto {

    @NotBlank(message = "employeeCode is required")
    private String employeeCode;

    @NotEmpty(message = "At least one role must be provided")
    private List<String> rolesToAssign;

    @NotEmpty(message = "At least one role must be provided")
    private List<String> rolesToRemove;
}
