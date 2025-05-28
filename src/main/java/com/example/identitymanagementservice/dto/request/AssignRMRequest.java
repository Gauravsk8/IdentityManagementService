package com.example.identitymanagementservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignRMRequest {

    @NotEmpty(message = "employeeCode is required")
    private String employeeCode;

    @NotEmpty(message = "managerCode is required")
    private String managerCode;
}
