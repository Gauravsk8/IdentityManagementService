package com.example.identitymanagementservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class UserRoleUpdateRequestDto {

    @NotBlank(message = "employeeCode is required")
    private String employeeCode;

    private List<String> rolesToAssign;

    private List<String> rolesToRemove;
}
