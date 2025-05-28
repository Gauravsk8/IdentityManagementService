package com.example.IdentityManagementService.service;

import com.example.IdentityManagementService.Repository.EmployeeRepository;
import com.example.IdentityManagementService.Service.ServiceImpl.KeycloakCreateUserServiceImpl;
import com.example.IdentityManagementService.common.constants.ErrorCode;
import com.example.IdentityManagementService.common.constants.ErrorMessage;
import com.example.IdentityManagementService.common.email.service.EmailService;
import com.example.IdentityManagementService.dto.request.EmployeeRequestDto;
import com.example.IdentityManagementService.exceptions.TimesheetException;
import com.example.IdentityManagementService.model.Employee;

import static com.example.IdentityManagementService.common.constants.ErrorCode.KEYCLOAK_USER_CREATION_FAILED;
import static com.example.IdentityManagementService.common.constants.ErrorMessage.KEYCLOAK_USER_ALREADY_EXISTS;
import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakCreateUserServiceImplTest {

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
        validEmployeeRequest.setEmployeeCode("EMP001");
        validEmployeeRequest.setEmployeeType("Employee");
    }

    @Test
    void createUser_Success() {
        // Step 1: Mock repository checks
        when(employeeRepository.findByEmailAndIsActiveTrue(anyString())).thenReturn(Optional.empty());
        when(employeeRepository.findByEmployeeCodeAndIsActiveTrue(anyString())).thenReturn(Optional.empty());

        Employee savedEmployee = new Employee();
        savedEmployee.setEmployeeCode("EMP001");
        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);

        // Step 2: Mock Keycloak admin connection check
        RealmsResource realmsResource = mock(RealmsResource.class);
        when(keycloakAdmin.realms()).thenReturn(realmsResource);
        when(realmsResource.findAll()).thenReturn(Collections.emptyList()); // simulate success

        // Step 3: Mock user creation in Keycloak
        when(keycloakAdmin.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatus()).thenReturn(201);
        when(mockResponse.getLocation()).thenReturn(URI.create("http://localhost/users/" + userId));
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(mockResponse);
        when(usersResource.get(userId)).thenReturn(userResource);

        // Step 4: Mock role assignment
        RolesResource rolesResource = mock(RolesResource.class);
        RoleResource roleResource = mock(RoleResource.class);
        RoleRepresentation roleRep = new RoleRepresentation();
        roleRep.setName("Employee");

        when(realmResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get("Employee")).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(roleRep);

        // Step 5: Mock user roles and role mapping
        RoleMappingResource roleMappingResource = mock(RoleMappingResource.class);
        RoleScopeResource roleScopeResource = mock(RoleScopeResource.class);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);

        // Step 6: Mock email service
        String expectedEmailBody = "This is the email content";
        when(emailService.loadTemplate(eq("UserCreationTemplate.txt"), anyMap()))
                .thenReturn(expectedEmailBody);

        doNothing().when(emailService).sendEmail(
                eq("john.doe@example.com"),
                eq("Timesheet Application Login Credentials"),
                eq(expectedEmailBody)
        );

        // Step 7: Execute
        Map<String, String> result = keycloakCreateUserService.createUser(validEmployeeRequest);

        // Step 8: Verify
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
        existingEmployee.setEmployeeCode(validEmployeeRequest.getEmployeeCode());

        when(employeeRepository.findByEmailAndIsActiveTrue(anyString())).thenReturn(Optional.empty());
        when(employeeRepository.findByEmployeeCodeAndIsActiveTrue(validEmployeeRequest.getEmployeeCode()))
                .thenReturn(Optional.of(existingEmployee));

        TimesheetException exception = assertThrows(TimesheetException.class,
                () -> keycloakCreateUserService.createUser(validEmployeeRequest));

        assertEquals(ErrorCode.CONFLICT_ERROR, exception.getErrorCode());
        assertTrue(exception.getMessage().contains(validEmployeeRequest.getEmployeeCode()));
    }

    @Test
    void createUser_whenKeycloakCreationFails_rollsBack() {
        // Arrange
        when(employeeRepository.findByEmailAndIsActiveTrue(anyString())).thenReturn(Optional.empty());
        when(employeeRepository.findByEmployeeCodeAndIsActiveTrue(anyString())).thenReturn(Optional.empty());

        // Mock Keycloak admin connection check
        RealmsResource realmsResource = mock(RealmsResource.class);
        when(keycloakAdmin.realms()).thenReturn(realmsResource);
        when(realmsResource.findAll()).thenReturn(Collections.emptyList());

        // Mock realm and users resources
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        when(keycloakAdmin.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        // Empty search results for user existence check
        when(usersResource.search(anyString(), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(usersResource.list()).thenReturn(Collections.emptyList());

        // Simulate Keycloak create failure
        Response failureResponse = mock(Response.class);
        when(failureResponse.getStatus()).thenReturn(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        when(failureResponse.readEntity(any(Class.class))).thenReturn("{\"error\":\"server error\"}");
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(failureResponse);

        // Act & Assert
        TimesheetException exception = assertThrows(TimesheetException.class, () -> {
            keycloakCreateUserService.createUser(validEmployeeRequest);
        });

        assertEquals(KEYCLOAK_USER_CREATION_FAILED, exception.getErrorCode());

        // Verify employee was saved once (before Keycloak creation) but then rolled back
        verify(employeeRepository, times(1)).save(any(Employee.class));
        // Verify no attempt to update with Keycloak userId
        verify(employeeRepository, never()).save(argThat(emp -> emp.getKeycloakUserId() != null));
    }

    @Test
    void updateOwnProfile_Success() {
        // Setup test data
        String keycloakUserId = "user-123";
        EmployeeRequestDto updateRequest = new EmployeeRequestDto();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Name");
        updateRequest.setEmail("updated@example.com");
        updateRequest.setEmployeeType("Manager");

        // Mock Keycloak calls
        when(keycloakAdmin.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(keycloakUserId)).thenReturn(userResource);

        UserRepresentation userRep = new UserRepresentation();
        userRep.setId(keycloakUserId);
        when(userResource.toRepresentation()).thenReturn(userRep);

        // Mock DB calls
        Employee existingEmployee = new Employee();
        existingEmployee.setKeycloakUserId(keycloakUserId);
        existingEmployee.setEmployeeCode("EMP001");
        when(employeeRepository.findByKeycloakUserIdAndIsActiveTrue(keycloakUserId))
                .thenReturn(Optional.of(existingEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(existingEmployee);

        // Execute
        keycloakCreateUserService.updateOwnProfile(keycloakUserId, updateRequest);

        // Verify
        verify(userResource).update(any(UserRepresentation.class));
        verify(employeeRepository).save(existingEmployee);
        assertEquals("Updated", existingEmployee.getFirstName());
        assertEquals("Name", existingEmployee.getLastName());
        assertEquals("updated@example.com", existingEmployee.getEmail());
        assertEquals("Manager", existingEmployee.getEmployeeType());
    }

    @Test
    void updateUserProfile_Success() {
        String employeeCode = "EMP001";
        EmployeeRequestDto updateRequest = new EmployeeRequestDto();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Name");
        updateRequest.setEmail("updated@example.com");
        updateRequest.setEmployeeType("Manager");

        // Mock Keycloak user search
        when(keycloakAdmin.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setId("user-123");
        kcUser.setUsername(employeeCode);
        when(usersResource.search(employeeCode, true)).thenReturn(Collections.singletonList(kcUser));
        when(usersResource.get(kcUser.getId())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(kcUser);

        // Mock DB calls
        Employee existingEmployee = new Employee();
        existingEmployee.setEmployeeCode(employeeCode);
        when(employeeRepository.findByEmployeeCodeAndIsActiveTrue(employeeCode))
                .thenReturn(Optional.of(existingEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(existingEmployee);

        // Execute
        keycloakCreateUserService.updateUserProfile(employeeCode, updateRequest);

        // Verify
        verify(userResource).update(any(UserRepresentation.class));
        verify(employeeRepository).save(existingEmployee);
        assertEquals("Updated", existingEmployee.getFirstName());
    }

    @Test
    void updateUserPassword_Success() {
        String userId = "user-123";
        String newPassword = "newPassword123";

        // Mock Keycloak calls
        when(keycloakAdmin.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId)).thenReturn(userResource);

        // Execute
        keycloakCreateUserService.updateUserPassword(userId, newPassword);

        // Verify
        verify(userResource).resetPassword(any(CredentialRepresentation.class));
    }

    @Test
    void getUserByemployeeCodekc_Success() {
        String employeeCode = "EMP001";

        // Mock Keycloak calls
        when(keycloakAdmin.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        UserRepresentation expectedUser = new UserRepresentation();
        expectedUser.setUsername(employeeCode);
        when(usersResource.search(employeeCode, true)).thenReturn(Collections.singletonList(expectedUser));

        // Execute
        UserRepresentation result = keycloakCreateUserService.getUserByemployeeCodekc(employeeCode);

        // Verify
        assertNotNull(result);
        assertEquals(employeeCode, result.getUsername());
    }

    @Test
    void getUserById_Success() {
        String userId = "user-123";
        UserRepresentation expectedUser = new UserRepresentation();
        expectedUser.setId(userId);

        // Mock Keycloak calls
        when(keycloakAdmin.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(expectedUser);

        // Execute
        UserRepresentation result = keycloakCreateUserService.getUserById(userId);

        // Verify
        assertNotNull(result);
        assertEquals(userId, result.getId());
    }
}