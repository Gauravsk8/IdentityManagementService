package com.example.IdentityManagementService.dto.request.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String email;
    private String managerCode;
    private String employeeType;

}

