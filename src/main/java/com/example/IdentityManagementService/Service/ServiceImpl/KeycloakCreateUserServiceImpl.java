package com.example.IdentityManagementService.Service.ServiceImpl;


import com.example.IdentityManagementService.Service.KeycloakCreateUserService;
import com.example.IdentityManagementService.Repository.EmployeeRepository;
import com.example.IdentityManagementService.dto.request.EmployeeRequestDto;
import com.example.IdentityManagementService.exceptions.TimesheetException;
import com.example.IdentityManagementService.model.Employee;
import com.example.IdentityManagementService.common.constants.ErrorCode;
import com.example.IdentityManagementService.common.constants.ErrorMessage;
import com.example.IdentityManagementService.common.constants.MessageConstants;
import com.example.IdentityManagementService.common.email.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.ErrorResponseException;

import java.io.IOException;
import java.util.*;

import static com.example.IdentityManagementService.common.constants.ErrorCode.KEYCLOAK_USER_CREATION_FAILED;
import static com.example.IdentityManagementService.common.constants.ErrorCode.NOT_FOUND_ERROR;
import static com.example.IdentityManagementService.common.constants.ErrorMessage.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakCreateUserServiceImpl implements KeycloakCreateUserService {

    private final Keycloak keycloakAdmin;
    private final EmailService emailService;
    private final EmployeeRepository employeeRepository;



    @Value("${keycloak.realm}")
    private String realm;

    @Transactional
    public Map<String, String> createUser(EmployeeRequestDto employee) {
        String userId = null;
        String randomPassword = null;

        try {
            // Check if email already exists
            Optional<Employee> existingEmail = employeeRepository.findByEmailAndIsActiveTrue(employee.getEmail());
            if (existingEmail.isPresent()) {
                throw new TimesheetException(ErrorCode.CONFLICT_ERROR, EMPLOYEE_ALREADY_EXISTS + employee.getEmail());
            }

            // Check if employeeCode already exists
            Optional<Employee> existingCode = employeeRepository.findByEmployeeCodeAndIsActiveTrue(employee.getEmployeeCode());
            if (existingCode.isPresent()) {
                throw new TimesheetException(ErrorCode.CONFLICT_ERROR, EMPLOYEE_ALREADY_EXISTS + employee.getEmployeeCode());
            }

            // Save employee in DB first
            Employee newEmployee = new Employee();
            newEmployee.setFirstName(employee.getFirstName());
            newEmployee.setLastName(employee.getLastName());
            newEmployee.setEmail(employee.getEmail());
            newEmployee.setEmployeeCode(employee.getEmployeeCode());
            String empType = employee.getEmployeeType();
            if (empType == null || empType.isEmpty()) {
                empType = "Employee";
            }
            newEmployee.setEmployeeType(empType);


            Employee savedEmployee = employeeRepository.save(newEmployee);

            // Connect to Keycloak
            verifyAdminConnection();
            RealmResource realmResource = keycloakAdmin.realm(realm);

            // Check if user exists in Keycloak
            checkExistingUser(employee.getEmail(), employee.getEmployeeCode(), realmResource);

            // Create user in Keycloak
            UserRepresentation user = createUserRepresentation(employee);
            UsersResource usersResource = realmResource.users();
            Response response = usersResource.create(user);
            handleCreateUserResponse(response);

            userId = extractUserIdFromResponse(response);

            // Set random temporary password
            randomPassword = generateRandomPassword();
            CredentialRepresentation passwordCred = new CredentialRepresentation();
            passwordCred.setTemporary(true);
            passwordCred.setType(CredentialRepresentation.PASSWORD);
            passwordCred.setValue(randomPassword);
            usersResource.get(userId).resetPassword(passwordCred);

            RoleRepresentation employeeRole = realmResource.roles().get("Employee").toRepresentation();
            realmResource.users().get(userId).roles().realmLevel().add(Collections.singletonList(employeeRole));

            // Update employee with Keycloak userId
            savedEmployee.setKeycloakUserId(userId);
            employeeRepository.save(savedEmployee);  // second DB update

            // Send  email
            Map<String, String> variables = new HashMap<>();
            variables.put("firstName", employee.getFirstName());
            variables.put("username", employee.getEmployeeCode());
            variables.put("password", randomPassword);

            String emailSubject = MessageConstants.CREATION_EMAIL;
            String emailBody = emailService.loadTemplate("UserCreationTemplate.txt", variables);
            emailService.sendEmail(employee.getEmail(), emailSubject, emailBody);


            Map<String, String> result = new HashMap<>();
            result.put("userId", userId);
            result.put("temporaryPassword", randomPassword);
            return result;

        } catch (Exception e) {
            if (userId != null) {
                try {
                    keycloakAdmin.realm(realm).users().get(userId).remove();
                    log.info("Rolled back Keycloak user creation for userId={}", userId);
                } catch (Exception ex) {
                    log.error("Failed to rollback Keycloak user", ex);
                }
            }
            if (e instanceof TimesheetException) {
                throw e;
            } else if (e instanceof IllegalArgumentException) {
                throw e;
            } else {
                throw new TimesheetException(KEYCLOAK_USER_CREATION_FAILED, "User creation failed", e);
            }
        }
    }

    private String generateRandomPassword() {
        return UUID.randomUUID().toString().substring(0, 8); // 8 chars random password
    }


    private void verifyAdminConnection() {
        try {
            keycloakAdmin.realms().findAll();
        } catch (Exception e) {
            throw new TimesheetException(
                    ErrorCode.KEYCLOAK_CONNECTION_ERROR,
                    KEYCLOAK_ADMIN_CONNECTION_FAILED,
                    e
            );
        }
    }

    private void checkExistingUser(String email, String employeeCode, RealmResource realmResource) {
        // Check for existing user by employeeCode
        List<UserRepresentation> usersByemployeeCode = realmResource.users().search(employeeCode, 0, 1, true);
        for (UserRepresentation user : usersByemployeeCode) {
            if (user.getUsername().equalsIgnoreCase(employeeCode)) {
                throw new TimesheetException(
                        ErrorCode.CONFLICT_ERROR,
                        String.format(KEYCLOAK_USER_ALREADY_EXISTS, employeeCode)
                );
            }
        }

        // Check for existing user by email
        List<UserRepresentation> allUsers = realmResource.users().list(); // optionally paginate for performance
        for (UserRepresentation user : allUsers) {
            if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(email)) {
                throw new TimesheetException(
                        ErrorCode.CONFLICT_ERROR,
                        String.format(KEYCLOAK_USER_ALREADY_EXISTS, email)
                );
            }
        }
    }


    private UserRepresentation createUserRepresentation(EmployeeRequestDto employee) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(employee.getEmployeeCode());
        user.setFirstName(employee.getFirstName());
        user.setLastName(employee.getLastName());
        user.setEmail(employee.getEmail());
        user.setEnabled(true);

        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("source", List.of("timesheet-app"));

        String employeeType = (employee.getEmployeeType() == null || employee.getEmployeeType().isBlank())
                ? "Employee"
                : employee.getEmployeeType();
        attributes.put("employeeType", List.of(employeeType));

        user.setAttributes(attributes);


        return user;
    }


    private void handleCreateUserResponse(Response response) {
        if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
            try {
                String errorBody = response.readEntity(String.class);
                JsonNode errorNode = new ObjectMapper().readTree(errorBody);
                String errorDetail = errorNode.path("error_description").asText(null);

                String message = errorDetail != null ? errorDetail : errorBody;
                throw new TimesheetException(KEYCLOAK_USER_CREATION_FAILED, KEYCLOAK_USER_CREATION_FAILED + ": " + message);
            } catch (IOException e) {
                throw new TimesheetException(KEYCLOAK_USER_CREATION_FAILED, KEYCLOAK_USER_CREATION_FAILED, e);
            }
        }
    }

    private String extractUserIdFromResponse(Response response) {
        try {
            String location = response.getLocation().toString();
            return location.substring(location.lastIndexOf('/') + 1);
        } catch (Exception e) {
            throw new TimesheetException(
                    ErrorCode.KEYCLOAK_RESPONSE_PARSING_ERROR,
                    USERID_EXTRACTION_FAILED,
                    e
            );
        }
    }

    @Override
    @Transactional
    public void updateOwnProfile(String keycloakUserId, EmployeeRequestDto dto) {
        // Update Keycloak
        try {
            RealmResource realmResource = keycloakAdmin.realm(realm);
            UsersResource usersResource = realmResource.users();

            UserResource userResource = usersResource.get(keycloakUserId);
            UserRepresentation user = userResource.toRepresentation();

            if (dto.getFirstName() != null) user.setFirstName(dto.getFirstName());
            if (dto.getLastName() != null) user.setLastName(dto.getLastName());
            if (dto.getEmail() != null) user.setEmail(dto.getEmail());

            Map<String, List<String>> attributes = user.getAttributes() != null ? user.getAttributes() : new HashMap<>();
            if (dto.getEmployeeType() != null) {
                attributes.put("employeeType", List.of(dto.getEmployeeType()));
            }
            user.setAttributes(attributes);

            userResource.update(user); // If this fails, exception is thrown and DB update is skipped

        } catch (ErrorResponseException e) {
            String message = e.getMessage().toLowerCase();

            if (message.contains("403")) {
                throw new TimesheetException(ErrorCode.FORBIDDEN_ERROR, UNAUTHORIZED_ACCESS, e);
            } else if (message.contains("404")) {
                throw new TimesheetException(NOT_FOUND_ERROR, USER_NOT_FOUND, e);
            } else {
                throw new TimesheetException(ErrorCode.INTERNAL_SERVER_ERROR, USER_UPDATE_FAILED + ": " + message, e);
            }
        } catch (Exception e) {
            throw new TimesheetException(ErrorCode.FORBIDDEN_ERROR, UNAUTHORIZED_ACCESS, e);
        }

        //Update DB
        Employee employee = employeeRepository.findByKeycloakUserIdAndIsActiveTrue(keycloakUserId)
                .orElseThrow(() -> new TimesheetException(NOT_FOUND_ERROR, ErrorMessage.USER_NOT_FOUND));

        if (dto.getFirstName() != null) employee.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) employee.setLastName(dto.getLastName());
        if (dto.getEmail() != null) employee.setEmail(dto.getEmail());
        if (dto.getEmployeeType() != null) employee.setEmployeeType(dto.getEmployeeType());

        employeeRepository.save(employee);
    }

    @Override
    @Transactional
    public void updateUserProfile(String employeeCode, EmployeeRequestDto dto) {
        try {
            // Step 1: Find user by employeeCode (to get Keycloak ID)
            var user = getUserByemployeeCodekc(employeeCode);
            if (user == null) {
                throw new TimesheetException(NOT_FOUND_ERROR, ErrorMessage.USER_NOT_FOUND);
            }

            // Step 2: Update Keycloak
            RealmResource realmResource = keycloakAdmin.realm(realm);
            UsersResource usersResource = realmResource.users();
            UserRepresentation userRep = usersResource.get(user.getId()).toRepresentation();

            if (dto.getFirstName() != null) userRep.setFirstName(dto.getFirstName());
            if (dto.getLastName() != null) userRep.setLastName(dto.getLastName());
            if (dto.getEmail() != null) userRep.setEmail(dto.getEmail());

            Map<String, List<String>> attributes = userRep.getAttributes();
            if (attributes == null) attributes = new HashMap<>();
            if (dto.getEmployeeType() != null) {
                attributes.put("employeeType", List.of(dto.getEmployeeType()));
            }
            userRep.setAttributes(attributes);

            usersResource.get(user.getId()).update(userRep); // If this fails, DB is untouched

            // Step 3: Update DB
            Employee employee = employeeRepository.findByEmployeeCodeAndIsActiveTrue(employeeCode)
                    .orElseThrow(() -> new TimesheetException(NOT_FOUND_ERROR, USER_NOT_FOUND));

            if (dto.getFirstName() != null) employee.setFirstName(dto.getFirstName());
            if (dto.getLastName() != null) employee.setLastName(dto.getLastName());
            if (dto.getEmail() != null) employee.setEmail(dto.getEmail());
            if (dto.getEmployeeType() != null) employee.setEmployeeType(dto.getEmployeeType());

            employeeRepository.save(employee); // This is within @Transactional

        } catch (Exception e) {
            log.error("Error updating user profile (Keycloak or DB)", e);
            throw new TimesheetException(
                    ErrorCode.FORBIDDEN_ERROR,
                    UNAUTHORIZED_ACCESS + e.getMessage(),
                    e
            );
        }
    }


    @Override
    public void updateUserPassword(String userId, String newPassword) {
        try {
            RealmResource realmResource = keycloakAdmin.realm(realm);
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(newPassword);
            credential.setTemporary(false);

            realmResource.users().get(userId).resetPassword(credential);

        } catch (ErrorResponseException e) {
            String message = e.getMessage().toLowerCase();

            if (message.contains("403")) {
                log.warn("Forbidden - not allowed to reset password for user {}", userId);
                throw new TimesheetException(ErrorCode.FORBIDDEN_ERROR, UNAUTHORIZED_ACCESS, e);
            } else if (message.contains("404")) {
                log.warn("User not found in Keycloak for ID {}", userId);
                throw new TimesheetException(NOT_FOUND_ERROR, USER_NOT_FOUND, e);
            } else {
                log.error("Unexpected Keycloak error while resetting password: {}", message);
                throw new TimesheetException(ErrorCode.INTERNAL_SERVER_ERROR, PASSWORD_UPDATE_FAILED + ": " + message, e);
            }

        } catch (Exception e) {
            log.error("Error updating user password", e);
            throw new TimesheetException(
                    ErrorCode.KEYCLOAK_CONNECTION_ERROR,
                    PASSWORD_UPDATE_FAILED + ": " + e.getMessage(),
                    e
            );
        }
    }


    public UserRepresentation getUserByemployeeCodekc(String employeeCode) {
        RealmResource realmResource = keycloakAdmin.realm(realm);
        List<UserRepresentation> users = realmResource.users().search(employeeCode, true);
        return users.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(employeeCode))
                .findFirst()
                .orElseThrow(() -> new TimesheetException(NOT_FOUND_ERROR, USER_NOT_FOUND + employeeCode));
    }


    public UserRepresentation getUserById(String id) {
        RealmResource realmResource = keycloakAdmin.realm(realm);
        return realmResource.users().get(id).toRepresentation();
    }




}
