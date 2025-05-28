package com.example.identitymanagementservice.service;

import com.example.identitymanagementservice.common.constants.ErrorCode;
import com.example.identitymanagementservice.common.email.service.EmailService;
import com.example.identitymanagementservice.dto.request.EmployeeRequestDto;
import com.example.identitymanagementservice.exceptions.TimesheetException;
import com.example.identitymanagementservice.model.Employee;
import com.example.identitymanagementservice.repository.EmployeeRepository;
import com.example.identitymanagementservice.service.service.impl.KeycloakCreateUserServiceImpl;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.example.identitymanagementservice.common.constants.ErrorCode.KEYCLOAK_USER_CREATION_FAILED;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakCreateUserServiceImplTest {

    private static final String EMPLOYEE_CODE = "EMP001";
    private static final String USER_ID = "user-123";
    private static final String UPDATED_NAME = "Updated";

    @Mock
    private Keycloak keycloakAdmin;

    @Mock
    private EmailService emailService;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @InjectMocks
    private KeycloakCreateUserServiceImpl keycloakCreateUserService;

    private final String realm = "test-realm";
    private final String userId = "12345-67890";
    private EmployeeRequestDto validEmployeeRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(keycloakCreateUserService, "realm", realm);

        validEmployeeRequest = new EmployeeRequestDto();
        validEmployeeRequest.setFirstName("John");
        validEmployeeRequest.setLastName("Doe");
        validEmployeeRequest.setEmail("john.doe@example.com");
        validEmployeeRequest.setEmployeeCode(EMPLOYEE_CODE);
        validEmployeeRequest.setEmployeeType("Employee");
    }

    @Test
    void createUser_Success() {
        when(employeeRepository.findByEmailAndIsActiveTrue(anyString())).thenReturn(Optional.empty());
        when(employeeRepository.findByEmployeeCodeAndIsActiveTrue(anyString())).thenReturn(Optional.empty());

        Employee savedEmployee = new Employee();
        savedEmployee.setEmployeeCode(EMPLOYEE_CODE);
        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);

        RealmsResource realmsResource = mock(RealmsResource.class);
        when(keycloakAdmin.realms()).thenReturn(realmsResource);
        when(realmsResource.findAll()).thenReturn(Collections.emptyList());

        when(keycloakAdmin.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatus()).thenReturn(201);
        when(mockResponse.getLocation()).thenReturn(URI.create("http://localhost/users/" + userId));
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(mockResponse);
        when(usersResource.get(userId)).thenReturn(userResource);

        RolesResource rolesResource = mock(RolesResource.class);
        RoleResource roleResource = mock(RoleResource.class);
        RoleRepresentation roleRep = new RoleRepresentation();
        roleRep.setName("Employee");

        when(realmResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get("Employee")).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(roleRep);

        RoleMappingResource roleMappingResource = mock(RoleMappingResource.class);
        RoleScopeResource roleScopeResource = mock(RoleScopeResource.class);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);

        String expectedEmailBody = "This is the email content";
        when(emailService.loadTemplate(eq("UserCreationTemplate.txt"), anyMap()))
                .thenReturn(expectedEmailBody);

        doNothing().when(emailService).sendEmail(
                eq("john.doe@example.com"),
                eq("Timesheet Application Login Credentials"),
                eq(expectedEmailBody)
        );

        Map<String, String> result = keycloakCreateUserService.createUser(validEmployeeRequest);

        assertNotNull(result);
        assertEquals(userId, result.get("userId"));
        assertNotNull(result.get("temporaryPassword"));

        verify(employeeRepository, times(2)).save(any(Employee.class));
        verify(emailService).sendEmail(
                eq("john.doe@example.com"),
                eq("Timesheet Application Login Credentials"),
                eq(expectedEmailBody)
        );
    }


    @Test
    void createUser_EmailAlreadyExists_ThrowsException() {
        Employee existingEmployee = new Employee();
        existingEmployee.setEmail(validEmployeeRequest.getEmail());

        when(employeeRepository.findByEmailAndIsActiveTrue(validEmployeeRequest.getEmail()))
                .thenReturn(Optional.of(existingEmployee));

        TimesheetException exception = assertThrows(TimesheetException.class,
                () -> keycloakCreateUserService.createUser(validEmployeeRequest));

        assertEquals(ErrorCode.CONFLICT_ERROR, exception.getErrorCode());
        assertTrue(exception.getMessage().contains(validEmployeeRequest.getEmail()));
    }

    @Test
    void createUser_EmployeeCodeAlreadyExists_ThrowsException() {
        Employee existingEmployee = new Employee();
        existingEmployee.setEmployeeCode(EMPLOYEE_CODE);

        when(employeeRepository.findByEmailAndIsActiveTrue(anyString())).thenReturn(Optional.empty());
        when(employeeRepository.findByEmployeeCodeAndIsActiveTrue(EMPLOYEE_CODE))
                .thenReturn(Optional.of(existingEmployee));

        TimesheetException exception = assertThrows(TimesheetException.class,
                () -> keycloakCreateUserService.createUser(validEmployeeRequest));

        assertEquals(ErrorCode.CONFLICT_ERROR, exception.getErrorCode());
        assertTrue(exception.getMessage().contains(EMPLOYEE_CODE));
    }

    @Test
    void createUser_whenKeycloakCreationFails_rollsBack() {
        when(employeeRepository.findByEmailAndIsActiveTrue(anyString())).thenReturn(Optional.empty());
        when(employeeRepository.findByEmployeeCodeAndIsActiveTrue(anyString())).thenReturn(Optional.empty());

        RealmsResource realmsResource = mock(RealmsResource.class);
        when(keycloakAdmin.realms()).thenReturn(realmsResource);
        when(realmsResource.findAll()).thenReturn(Collections.emptyList());

        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        when(keycloakAdmin.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        when(usersResource.search(anyString(), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(usersResource.list()).thenReturn(Collections.emptyList());

        Response failureResponse = mock(Response.class);
        when(failureResponse.getStatus()).thenReturn(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        when(failureResponse.readEntity(any(Class.class))).thenReturn("{\"error\":\"server error\"}");
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(failureResponse);

        TimesheetException exception = assertThrows(TimesheetException.class, () -> {
            keycloakCreateUserService.createUser(validEmployeeRequest);
        });

        assertEquals(KEYCLOAK_USER_CREATION_FAILED, exception.getErrorCode());

        verify(employeeRepository, times(1)).save(any(Employee.class));
        verify(employeeRepository, never()).save(argThat(emp -> emp.getKeycloakUserId() != null));
    }

    @Test
    void updateOwnProfile_Success() {
        String keycloakUserId = USER_ID;
        EmployeeRequestDto updateRequest = new EmployeeRequestDto();
        updateRequest.setFirstName(UPDATED_NAME);
        updateRequest.setLastName("Name");
        updateRequest.setEmail("updated@example.com");
        updateRequest.setEmployeeType("Manager");

        when(keycloakAdmin.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(keycloakUserId)).thenReturn(userResource);

        UserRepresentation userRep = new UserRepresentation();
        userRep.setId(keycloakUserId);
        when(userResource.toRepresentation()).thenReturn(userRep);

        Employee existingEmployee = new Employee();
        existingEmployee.setKeycloakUserId(keycloakUserId);
        existingEmployee.setEmployeeCode(EMPLOYEE_CODE);
        when(employeeRepository.findByKeycloakUserIdAndIsActiveTrue(keycloakUserId))
                .thenReturn(Optional.of(existingEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(existingEmployee);

        keycloakCreateUserService.updateOwnProfile(keycloakUserId, updateRequest);

        verify(userResource).update(any(UserRepresentation.class));
        verify(employeeRepository).save(existingEmployee);
        assertEquals(UPDATED_NAME, existingEmployee.getFirstName());
        assertEquals("Name", existingEmployee.getLastName());
        assertEquals("updated@example.com", existingEmployee.getEmail());
        assertEquals("Manager", existingEmployee.getEmployeeType());
    }

    @Test
    void updateUserProfile_Success() {
        String employeeCode = EMPLOYEE_CODE;
        EmployeeRequestDto updateRequest = new EmployeeRequestDto();
        updateRequest.setFirstName(UPDATED_NAME);
        updateRequest.setLastName("Name");
        updateRequest.setEmail("updated@example.com");
        updateRequest.setEmployeeType("Manager");

        when(keycloakAdmin.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setId(USER_ID);
        kcUser.setUsername(employeeCode);
        when(usersResource.search(employeeCode, true)).thenReturn(Collections.singletonList(kcUser));
        when(usersResource.get(kcUser.getId())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(kcUser);

        Employee existingEmployee = new Employee();
        existingEmployee.setEmployeeCode(employeeCode);
        when(employeeRepository.findByEmployeeCodeAndIsActiveTrue(employeeCode))
                .thenReturn(Optional.of(existingEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(existingEmployee);

        keycloakCreateUserService.updateUserProfile(employeeCode, updateRequest);

        verify(userResource).update(any(UserRepresentation.class));
        verify(employeeRepository).save(existingEmployee);
        assertEquals(UPDATED_NAME, existingEmployee.getFirstName());
    }

    @Test
    void updateUserPassword_Success() {
        String userId = USER_ID;
        String newPassword = "newPassword123";

        when(keycloakAdmin.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId)).thenReturn(userResource);

        keycloakCreateUserService.updateUserPassword(userId, newPassword);

        verify(userResource).resetPassword(any(CredentialRepresentation.class));
    }

    @Test
    void getUserByemployeeCodekc_Success() {
        String employeeCode = EMPLOYEE_CODE;

        when(keycloakAdmin.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        UserRepresentation expectedUser = new UserRepresentation();
        expectedUser.setUsername(employeeCode);
        when(usersResource.search(employeeCode, true)).thenReturn(Collections.singletonList(expectedUser));

        UserRepresentation result = keycloakCreateUserService.getUserByemployeeCodekc(employeeCode);

        assertNotNull(result);
        assertEquals(employeeCode, result.getUsername());
    }

    @Test
    void getUserById_Success() {
        String userId = USER_ID;
        UserRepresentation expectedUser = new UserRepresentation();
        expectedUser.setId(userId);

        when(keycloakAdmin.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(expectedUser);

        UserRepresentation result = keycloakCreateUserService.getUserById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
    }
}
