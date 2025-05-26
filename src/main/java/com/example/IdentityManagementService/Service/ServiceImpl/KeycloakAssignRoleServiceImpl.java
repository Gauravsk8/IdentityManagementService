package com.example.IdentityManagementService.Service.ServiceImpl;

import com.example.IdentityManagementService.Repository.EmployeeRepository;
import com.example.IdentityManagementService.exceptions.TimesheetException;
import com.example.IdentityManagementService.Service.KeycloakAssignRoleService;
import com.example.IdentityManagementService.model.Employee;
import com.example.IdentityManagementService.common.constants.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.IdentityManagementService.common.constants.ErrorCode.*;
import static com.example.IdentityManagementService.common.constants.ErrorMessage.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAssignRoleServiceImpl implements KeycloakAssignRoleService {

    private final Keycloak keycloakAdmin;
    private final EmployeeRepository employeeRepository;

    @Value("${keycloak.realm}")
    private String realm;

    public void assignRealmRoles(String employeeCode, List<String> roles) {
        RealmResource realmResource = keycloakAdmin.realm(realm);

        // Search for user by employeeCode
        List<UserRepresentation> users = realmResource.users().search(employeeCode);
        if (users.isEmpty()) {
            throw new TimesheetException(NOT_FOUND_ERROR, USER_NOT_FOUND + " " + employeeCode);
        }

        // Assuming employeeCode is unique, use the first user found
        UserRepresentation userRepresentation = users.get(0);
        String userId = userRepresentation.getId();
        UserResource userResource = realmResource.users().get(userId);


        // Get currently assigned roles
        List<String> alreadyAssignedRoles = userResource.roles().realmLevel().listEffective().stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());

        List<RoleRepresentation> roleRepresentations = new ArrayList<>();
        for (String roleName : roles) {
            if (alreadyAssignedRoles.contains(roleName)) {
                throw new TimesheetException(CONFLICT_ERROR, ROLE_ALREADY_ASSIGNED + roleName);
            }

            try {
                RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
                roleRepresentations.add(role);
            } catch (Exception e) {
                throw new TimesheetException(NOT_FOUND_ERROR, ROLE_NOT_FOUND + roleName);
            }
        }

        // Get the UserResource and assign roles
        try {
            userResource.roles().realmLevel().add(roleRepresentations);
            log.info("Assigned roles {} to user {}", roles, employeeCode);
        } catch (Exception e) {
            throw new TimesheetException(NOT_FOUND_ERROR, ROLE_ASSIGNMENT_FAILED + employeeCode);
        }

    }

    public List<String> getAssignedRealmRoles(String employeeCode) {
        RealmResource realmResource = keycloakAdmin.realm(realm);

        // Find user by employeeCode
        List<UserRepresentation> users = realmResource.users().search(employeeCode, true);
        if (users.isEmpty()) {
            throw new TimesheetException(
                    NOT_FOUND_ERROR,
                    USER_NOT_FOUND + employeeCode
            );
        }

        String userId = users.get(0).getId();

        // Get realm-level roles assigned to the user
        List<RoleRepresentation> realmRoles = realmResource.users()
                .get(userId)
                .roles()
                .realmLevel()
                .listEffective();

        // Filter out unwanted roles
        return realmRoles.stream()
                .map(RoleRepresentation::getName)
                .filter(role -> !role.equalsIgnoreCase("offline_access"))
                .filter(role -> !role.equalsIgnoreCase("uma_authorization"))
                .filter(role -> !role.equalsIgnoreCase("default-roles-" + realm))
                .collect(Collectors.toList());
    }

    public void unassignRealmRoles(String employeeCode, List<String> roles) {
        RealmResource realmResource = keycloakAdmin.realm(realm);

        // Search for user by employeeCode
        List<UserRepresentation> users = realmResource.users().search(employeeCode);
        if (users.isEmpty()) {
            throw new TimesheetException(NOT_FOUND_ERROR, USER_NOT_FOUND + " " + employeeCode);
        }

        // Use first user found (assuming employeeCode is unique)
        UserRepresentation userRepresentation = users.get(0);
        String userId = userRepresentation.getId();
        UserResource userResource = realmResource.users().get(userId);

        // Get currently assigned roles
        List<String> alreadyAssignedRoles = userResource.roles().realmLevel().listEffective().stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());

        List<RoleRepresentation> rolesToUnassign = new ArrayList<>();
        for (String roleName : roles) {
            if (!alreadyAssignedRoles.contains(roleName)) {
                throw new TimesheetException(NOT_FOUND_ERROR, ROLE_NOT_ASSIGNED + roleName);
            }

            try {
                RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
                rolesToUnassign.add(role);
            } catch (Exception e) {
                throw new TimesheetException(NOT_FOUND_ERROR, ROLE_NOT_FOUND + roleName);
            }
        }

        try {
            userResource.roles().realmLevel().remove(rolesToUnassign);
            log.info("Unassigned roles {} from user {}", roles, employeeCode);
        } catch (Exception e) {
            throw new TimesheetException(ErrorCode.NOT_FOUND_ERROR, ROLE_NOT_FOUND + employeeCode);
        }


    }


    public Map<String, String> getUsersByRoles(List<String> roleNames) {

        try {
            RealmResource realmResource = keycloakAdmin.realm(realm);
            List<UserRepresentation> allUsers = realmResource.users().list();


            Map<String, Employee> activeEmployees =
                    employeeRepository.findAllByIsActiveTrue()
                            .stream()
                            .filter(e -> e.getKeycloakUserId() != null)
                            .collect(Collectors.toMap(Employee::getKeycloakUserId,
                                    Function.identity()));

            Map<String, String> matchedUsers = new HashMap<>();

            for (UserRepresentation user : allUsers) {

                // Skip if the user is not active in the DB
                if (!activeEmployees.containsKey(user.getId())) {
                    continue;
                }

                List<String> userRoleNames = realmResource.users()
                        .get(user.getId())
                        .roles()
                        .realmLevel()
                        .listEffective()
                        .stream()
                        .map(RoleRepresentation::getName)
                        .toList();

                boolean hasAllRoles = roleNames.stream()
                        .allMatch(userRoleNames::contains);

                if (hasAllRoles) {
                    matchedUsers.put(user.getUsername(), user.getFirstName());
                }
            }

            if (matchedUsers.isEmpty()) {
                throw new TimesheetException(NOT_FOUND_ERROR,
                        USER_NOT_FOUND + " with role(s): " + roleNames);
            }
            return matchedUsers;

        } catch (TimesheetException ex) {
            throw ex;               // preserve the original code-path
        } catch (Exception ex) {
            log.error("Error while fetching users by roles", ex);
            throw new TimesheetException(INTERNAL_SERVER_ERROR,
                    INTERNAL_SERVER_ERROR);
        }
    }


    public void updateUserRoles(String employeeCode, List<String> rolesToAssign, List<String> rolesToRemove) {
        RealmResource realmResource = keycloakAdmin.realm(realm);

        List<UserRepresentation> users = realmResource.users().search(employeeCode);
        if (users.isEmpty()) {
            throw new TimesheetException(NOT_FOUND_ERROR, USER_NOT_FOUND + " " + employeeCode);
        }

        UserRepresentation userRepresentation = users.get(0);
        String userId = userRepresentation.getId();
        UserResource userResource = realmResource.users().get(userId);

        try {
            if (rolesToAssign != null && !rolesToAssign.isEmpty()) {
                List<RoleRepresentation> assignRoles = new ArrayList<>();
                for (String roleName : rolesToAssign) {

                    RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
                    assignRoles.add(role);
                }
                userResource.roles().realmLevel().add(assignRoles);
                log.info("Assigned roles {} to user {}", rolesToAssign, employeeCode);
            }

            if (rolesToRemove != null && !rolesToRemove.isEmpty()) {
                List<RoleRepresentation> removeRoles = new ArrayList<>();
                for (String roleName : rolesToRemove) {
                    RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
                    removeRoles.add(role);
                }
                userResource.roles().realmLevel().remove(removeRoles);
                log.info("Removed roles {} from user {}", rolesToRemove, employeeCode);
            }

        } catch (Exception e) {
            throw new TimesheetException(NOT_FOUND_ERROR, "Role update failed for user " + employeeCode);
        }


    }


    public boolean hasManagerRole(String employeeCode, String role) {
        RealmResource realmResource = keycloakAdmin.realm(realm);

        List<UserRepresentation> users = realmResource.users().search(employeeCode);
        if (users.isEmpty()) {
            throw new TimesheetException(NOT_FOUND_ERROR, USER_NOT_FOUND + " " + employeeCode);
        }

        String userId = users.get(0).getId();

        List<String> assignedRoles = realmResource.users()
                .get(userId)
                .roles()
                .realmLevel()
                .listEffective()
                .stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());

        return assignedRoles.contains(role);
    }
}