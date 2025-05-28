package com.example.identitymanagementservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class EmployeeRequestDto {

    @NotBlank(message = "Username is EmployeeCode- required")
    private String employeeCode;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "First name is required")
    @Size(max = 20, message = "FistName can't exceed 20 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 20, message = "lastName can't exceed 20 characters")
    private String lastName;

    @SuppressWarnings("checkstyle:MemberName")
    @Size(max = 20, message = "EmployeeType can't exceed 20 characters")
    private String EmployeeType;



}
