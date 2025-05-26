package com.example.IdentityManagementService.Service;

import com.example.IdentityManagementService.dto.request.EmployeeRequestDto;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Map;

public interface KeycloakCreateUserService {
    Map<String, String> createUser(EmployeeRequestDto employee);
    void updateUserPassword(String userId, String newPassword);
    UserRepresentation getUserByemployeeCodekc(String employeeCode);
    UserRepresentation getUserById(String id);
    void updateUserProfile(String employeeCode, EmployeeRequestDto dto);
    void updateOwnProfile(String userId, EmployeeRequestDto dto);
}
