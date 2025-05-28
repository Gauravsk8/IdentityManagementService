package com.example.identitymanagementservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserIdentityDto {
    private String keycloakUserId;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String email;
    private String employeeType;
    private String managerCode;

}