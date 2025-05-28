package com.example.identitymanagementservice.controller;

import com.example.identitymanagementservice.service.KeycloakAssignRoleService;
import com.example.identitymanagementservice.service.KeycloakCreateUserService;
import com.example.identitymanagementservice.common.annotations.RequiresKeycloakAuthorization;
import com.example.identitymanagementservice.common.constants.MessageConstants;
import com.example.identitymanagementservice.dto.request.UserRoleAssignRequestDto;
import com.example.identitymanagementservice.dto.request.UserRoleUpdateRequestDto;
import com.example.identitymanagementservice.dto.request.pagenationdto.PasswordUpdateRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

import com.example.identitymanagementservice.dto.request.EmployeeRequestDto;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/ims")
@RequiredArgsConstructor
public class KeycloakController {

    private final KeycloakCreateUserService keycloakAdminService;
    private final KeycloakAssignRoleService keycloakAssignRoleService;

    // Repeated string literals
    private static final String RESOURCE_IDMS_ADMIN = "idms:admin";
    private static final String SCOPE_IDMS_USER_UPDATE = "idms:user:update";
    private static final String USERS_ROLES_PATH = "/users/roles";

    //Create User - single function to save in both keycloak and DB
    @PostMapping("/users")
    @RequiresKeycloakAuthorization(resource = RESOURCE_IDMS_ADMIN, scope = "idms:user:add")
    public ResponseEntity<Map<String, String>> createUser(
            @Valid @RequestBody EmployeeRequestDto dto
    ) {
        Map<String, String> result = keycloakAdminService.createUser(dto);

        Map<String, String> response = new HashMap<>();
        response.put("message", "User created successfully");
        return ResponseEntity.ok().body(response);
    }

    //Edit My Profile
    @PatchMapping("/users/my")
    @RequiresKeycloakAuthorization(resource = "idms:user", scope = SCOPE_IDMS_USER_UPDATE)
    public ResponseEntity<String> editOwnProfile(@Valid @RequestBody EmployeeRequestDto dto) {
        String keycloakUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        keycloakAdminService.updateOwnProfile(keycloakUserId, dto);

        return ResponseEntity.ok(MessageConstants.EMPLOYEE_UPDATED_SUCCESSFULLY);
    }

    //Edit User Profile
    @PatchMapping("/users/{employeeCode}")
    @RequiresKeycloakAuthorization(resource = RESOURCE_IDMS_ADMIN, scope = SCOPE_IDMS_USER_UPDATE)
    public ResponseEntity<String> editEmployeeProfile(
            @PathVariable String employeeCode,
            @Valid @RequestBody EmployeeRequestDto dto
    ) {
        keycloakAdminService.updateUserProfile(employeeCode, dto); // Now handles both Keycloak + DB
        return ResponseEntity.ok(MessageConstants.EMPLOYEE_UPDATED_SUCCESSFULLY);
    }

    //Update Password
    @PostMapping("/users/my/password")
    @RequiresKeycloakAuthorization(resource = "idms:user", scope = SCOPE_IDMS_USER_UPDATE)
    public ResponseEntity<String> updateOwnPassword(@Valid @RequestBody PasswordUpdateRequestDto request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        keycloakAdminService.updateUserPassword(userId, request.getNewPassword());

        return ResponseEntity.ok(MessageConstants.PASSWORD_UPDATED_SUCCESSFULLY);
    }

    //ROLES

    //Assign Role
    @PostMapping(USERS_ROLES_PATH)
    @RequiresKeycloakAuthorization(resource = RESOURCE_IDMS_ADMIN, scope = "idms:user:add")
    public ResponseEntity<String> assignRoles(
            @RequestBody UserRoleAssignRequestDto requestDto
    ) {
        keycloakAssignRoleService.assignRealmRoles(requestDto.getEmployeeCode(), requestDto.getRoles());
        return ResponseEntity.ok(MessageConstants.ROLES_ASSIGNED_SUCCESSFULLY);
    }

    @DeleteMapping(USERS_ROLES_PATH)
    @RequiresKeycloakAuthorization(resource = RESOURCE_IDMS_ADMIN, scope = "idms:user:add")
    public ResponseEntity<String> unassignRoles(
            @RequestBody UserRoleAssignRequestDto requestDto
    ) {
        keycloakAssignRoleService.unassignRealmRoles(requestDto.getEmployeeCode(), requestDto.getRoles());
        return ResponseEntity.ok(MessageConstants.ROLES_UNASSIGNED_SUCCESSFULLY);
    }

    @PutMapping(USERS_ROLES_PATH)
    @RequiresKeycloakAuthorization(resource = RESOURCE_IDMS_ADMIN, scope = SCOPE_IDMS_USER_UPDATE)
    public ResponseEntity<String> updateUserRoles(
            @Valid @RequestBody UserRoleUpdateRequestDto requestDto
    ) {
        keycloakAssignRoleService.updateUserRoles(
                requestDto.getEmployeeCode(),
                requestDto.getRolesToAssign(),
                requestDto.getRolesToRemove()
        );
        return ResponseEntity.ok(MessageConstants.ROLES_UPDATED_SUCCESSFULLY);
    }

    //Check Has ManagerRole
    @GetMapping("/users/{employeeCode}/manager-roles")
    @RequiresKeycloakAuthorization(resource = "tms:com", scope = "tms:com:get")
    public ResponseEntity<Boolean> hasManagerRole(@PathVariable String employeeCode, @RequestParam String roleName) {
        boolean hasRole = keycloakAssignRoleService.hasManagerRole(employeeCode, roleName);
        return ResponseEntity.ok(hasRole);
    }

    @GetMapping("/users/{employeeCode}/roles")
    @RequiresKeycloakAuthorization(resource = RESOURCE_IDMS_ADMIN, scope = "idms:user:get")
    public ResponseEntity<List<String>> getUserAssignedRealmRoles(@PathVariable String employeeCode) {
        List<String> roles = keycloakAssignRoleService.getAssignedRealmRoles(employeeCode);
        return ResponseEntity.ok(roles);
    }

    @GetMapping(USERS_ROLES_PATH)
    @RequiresKeycloakAuthorization(resource = RESOURCE_IDMS_ADMIN, scope = "idms:user:get")
    public ResponseEntity<Map<String, String>> getUsersByRoles(@RequestParam List<String> roles) {
        Map<String, String> users = keycloakAssignRoleService.getUsersByRoles(roles);
        return ResponseEntity.ok(users);
    }

}
