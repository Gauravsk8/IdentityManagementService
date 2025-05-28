package com.example.IdentityManagementService.service;

import com.example.IdentityManagementService.Repository.EmployeeRepository;
import com.example.IdentityManagementService.Service.ServiceImpl.KeycloakAssignRoleServiceImpl;
import com.example.IdentityManagementService.exceptions.TimesheetException;
import com.example.IdentityManagementService.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static com.example.IdentityManagementService.common.constants.ErrorCode.NOT_FOUND_ERROR;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakAssignRoleServiceImplTest {

    @Mock private Keycloak keycloakAdmin;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private RealmResource realmResource;
    @Mock private UsersResource usersResource;
    @Mock private RolesResource rolesResource;
    @Mock private RoleResource roleResource;
    @Mock private UserResource userResource;
    @Mock private RoleMappingResource roleMappingResource;
    @Mock private RoleScopeResource realmLevelResource;

    @InjectMocks
    private KeycloakAssignRoleServiceImpl keycloakAssignRoleService;

    @BeforeEach
    void setUp() {
        keycloakAssignRoleService = new KeycloakAssignRoleServiceImpl(keycloakAdmin, employeeRepository);
        ReflectionTestUtils.setField(keycloakAssignRoleService, "realm", "test-realm");

        // Make these stubbings lenient so they don't cause unnecessary stubbing errors
        lenient().when(keycloakAdmin.realm("test-realm")).thenReturn(realmResource);
        lenient().when(realmResource.users()).thenReturn(usersResource);
        lenient().when(realmResource.roles()).thenReturn(rolesResource);
    }

    @Test
    void assignRealmRoles_shouldAssignRolesSuccessfully() {
        String employeeCode = "emp123";
        String roleName = "employee";
        String userId = "user-id";

        UserRepresentation user = new UserRepresentation();
        user.setId(userId);
        user.setUsername(employeeCode);

        when(usersResource.search(employeeCode)).thenReturn(List.of(user));
        when(usersResource.get(userId)).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(realmLevelResource);
        when(realmLevelResource.listEffective()).thenReturn(Collections.emptyList());

        RoleRepresentation roleRep = new RoleRepresentation();
        roleRep.setName(roleName);
        when(rolesResource.get(roleName)).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(roleRep);

        doNothing().when(realmLevelResource).add(anyList());

        assertDoesNotThrow(() -> keycloakAssignRoleService.assignRealmRoles(employeeCode, List.of(roleName)));
    }

    @Test
    void getAssignedRealmRoles_shouldReturnFilteredRoles() {
        String employeeCode = "emp123";
        String userId = "user-id";

        UserRepresentation user = new UserRepresentation();
        user.setId(userId);
        user.setUsername(employeeCode);

        RoleRepresentation validRole = new RoleRepresentation();
        validRole.setName("manager");
        RoleRepresentation ignoredRole = new RoleRepresentation();
        ignoredRole.setName("offline_access");

        when(usersResource.search(employeeCode, true)).thenReturn(List.of(user));
        when(usersResource.get(userId)).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(realmLevelResource);
        when(realmLevelResource.listEffective()).thenReturn(List.of(validRole, ignoredRole));

        List<String> roles = keycloakAssignRoleService.getAssignedRealmRoles(employeeCode);
        assertEquals(1, roles.size());
        assertTrue(roles.contains("manager"));
    }

    @Test
    void unassignRealmRoles_shouldRemoveAssignedRoles() {
        String employeeCode = "emp123";
        String roleName = "employee";
        String userId = "user-id";

        UserRepresentation user = new UserRepresentation();
        user.setId(userId);

        RoleRepresentation roleRep = new RoleRepresentation();
        roleRep.setName(roleName);

        when(usersResource.search(employeeCode)).thenReturn(List.of(user));
        when(usersResource.get(userId)).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(realmLevelResource);
        when(realmLevelResource.listEffective()).thenReturn(List.of(roleRep));
        when(rolesResource.get(roleName)).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(roleRep);

        doNothing().when(realmLevelResource).remove(anyList());

        assertDoesNotThrow(() -> keycloakAssignRoleService.unassignRealmRoles(employeeCode, List.of(roleName)));
    }

    @Test
    void updateUserRoles_shouldAssignAndRemoveRoles() {
        String employeeCode = "emp123";
        String userId = "user-id";
        String roleAssign = "admin";
        String roleRemove = "viewer";

        UserRepresentation user = new UserRepresentation();
        user.setId(userId);

        RoleRepresentation assignRep = new RoleRepresentation();
        assignRep.setName(roleAssign);
        RoleRepresentation removeRep = new RoleRepresentation();
        removeRep.setName(roleRemove);

        when(usersResource.search(employeeCode)).thenReturn(List.of(user));
        when(usersResource.get(userId)).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(realmLevelResource);

        when(rolesResource.get(roleAssign)).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(assignRep);
        when(rolesResource.get(roleRemove)).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(removeRep);

        doNothing().when(realmLevelResource).add(anyList());
        doNothing().when(realmLevelResource).remove(anyList());

        assertDoesNotThrow(() ->
                keycloakAssignRoleService.updateUserRoles(employeeCode, List.of(roleAssign), List.of(roleRemove))
        );
    }

    @Test
    void hasManagerRole_shouldReturnTrueIfRoleExists() {
        String employeeCode = "emp123";
        String roleName = "manager";
        String userId = "user-id";

        UserRepresentation user = new UserRepresentation();
        user.setId(userId);

        RoleRepresentation role = new RoleRepresentation();
        role.setName(roleName);

        when(usersResource.search(employeeCode)).thenReturn(List.of(user));
        when(usersResource.get(userId)).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(realmLevelResource);
        when(realmLevelResource.listEffective()).thenReturn(List.of(role));

        assertTrue(keycloakAssignRoleService.hasManagerRole(employeeCode, roleName));
    }

    @Test
    void getUsersByRoles_shouldReturnUsersWithMatchingRoles() {
        String roleName = "developer";
        String userId = "user-id";

        UserRepresentation user = new UserRepresentation();
        user.setId(userId);
        user.setUsername("emp123");
        user.setFirstName("John");

        RoleRepresentation role = new RoleRepresentation();
        role.setName(roleName);

        Employee emp = new Employee();
        emp.setKeycloakUserId(userId);

        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.list()).thenReturn(List.of(user));
        when(employeeRepository.findAllByIsActiveTrue()).thenReturn(List.of(emp));
        when(usersResource.get(userId)).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(realmLevelResource);
        when(realmLevelResource.listEffective()).thenReturn(List.of(role));

        Map<String, String> result = keycloakAssignRoleService.getUsersByRoles(List.of(roleName));
        assertEquals(1, result.size());
        assertEquals("John", result.get("emp123"));
    }

    @Test
    void assignRealmRoles_shouldThrowIfUserNotFound() {
        when(usersResource.search("emp123")).thenReturn(Collections.emptyList());

        TimesheetException ex = assertThrows(TimesheetException.class, () ->
                keycloakAssignRoleService.assignRealmRoles("emp123", List.of("admin"))
        );
        assertEquals(NOT_FOUND_ERROR, ex.getErrorCode());
    }
}
