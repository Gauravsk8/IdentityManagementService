package com.example.identitymanagementservice.service;

import com.example.identitymanagementservice.exceptions.TimesheetException;
import com.example.identitymanagementservice.model.Employee;
import com.example.identitymanagementservice.repository.EmployeeRepository;
import com.example.identitymanagementservice.service.service.impl.KeycloakAssignRoleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.example.identitymanagementservice.common.constants.ErrorCode.NOT_FOUND_ERROR;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakAssignRoleServiceImplTest {

    private static final String EMPLOYEE_CODE = "emp123";
    private static final String USER_ID = "user-id";

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
        String roleName = "employee";

        UserRepresentation user = new UserRepresentation();
        user.setId(USER_ID);
        user.setUsername(EMPLOYEE_CODE);

        when(usersResource.search(EMPLOYEE_CODE)).thenReturn(List.of(user));
        when(usersResource.get(USER_ID)).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(realmLevelResource);
        when(realmLevelResource.listEffective()).thenReturn(Collections.emptyList());

        RoleRepresentation roleRep = new RoleRepresentation();
        roleRep.setName(roleName);
        when(rolesResource.get(roleName)).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(roleRep);

        doNothing().when(realmLevelResource).add(anyList());

        assertDoesNotThrow(() -> keycloakAssignRoleService.assignRealmRoles(EMPLOYEE_CODE, List.of(roleName)));
    }

    @Test
    void getAssignedRealmRoles_shouldReturnFilteredRoles() {
        UserRepresentation user = new UserRepresentation();
        user.setId(USER_ID);
        user.setUsername(EMPLOYEE_CODE);

        RoleRepresentation validRole = new RoleRepresentation();
        validRole.setName("manager");
        RoleRepresentation ignoredRole = new RoleRepresentation();
        ignoredRole.setName("offline_access");

        when(usersResource.search(EMPLOYEE_CODE, true)).thenReturn(List.of(user));
        when(usersResource.get(USER_ID)).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(realmLevelResource);
        when(realmLevelResource.listEffective()).thenReturn(List.of(validRole, ignoredRole));

        List<String> roles = keycloakAssignRoleService.getAssignedRealmRoles(EMPLOYEE_CODE);
        assertEquals(1, roles.size());
        assertTrue(roles.contains("manager"));
    }

    @Test
    void unassignRealmRoles_shouldRemoveAssignedRoles() {
        String roleName = "employee";

        UserRepresentation user = new UserRepresentation();
        user.setId(USER_ID);

        RoleRepresentation roleRep = new RoleRepresentation();
        roleRep.setName(roleName);

        when(usersResource.search(EMPLOYEE_CODE)).thenReturn(List.of(user));
        when(usersResource.get(USER_ID)).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(realmLevelResource);
        when(realmLevelResource.listEffective()).thenReturn(List.of(roleRep));
        when(rolesResource.get(roleName)).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(roleRep);

        doNothing().when(realmLevelResource).remove(anyList());

        assertDoesNotThrow(() -> keycloakAssignRoleService.unassignRealmRoles(EMPLOYEE_CODE, List.of(roleName)));
    }

    @Test
    void updateUserRoles_shouldAssignAndRemoveRoles() {
        String roleAssign = "admin";
        String roleRemove = "viewer";

        UserRepresentation user = new UserRepresentation();
        user.setId(USER_ID);

        RoleRepresentation assignRep = new RoleRepresentation();
        assignRep.setName(roleAssign);
        RoleRepresentation removeRep = new RoleRepresentation();
        removeRep.setName(roleRemove);

        when(usersResource.search(EMPLOYEE_CODE)).thenReturn(List.of(user));
        when(usersResource.get(USER_ID)).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(realmLevelResource);

        when(rolesResource.get(roleAssign)).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(assignRep);
        when(rolesResource.get(roleRemove)).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(removeRep);

        doNothing().when(realmLevelResource).add(anyList());
        doNothing().when(realmLevelResource).remove(anyList());

        assertDoesNotThrow(() ->
                keycloakAssignRoleService.updateUserRoles(EMPLOYEE_CODE, List.of(roleAssign), List.of(roleRemove))
        );
    }

    @Test
    void hasManagerRole_shouldReturnTrueIfRoleExists() {
        String roleName = "manager";

        UserRepresentation user = new UserRepresentation();
        user.setId(USER_ID);

        RoleRepresentation role = new RoleRepresentation();
        role.setName(roleName);

        when(usersResource.search(EMPLOYEE_CODE)).thenReturn(List.of(user));
        when(usersResource.get(USER_ID)).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(realmLevelResource);
        when(realmLevelResource.listEffective()).thenReturn(List.of(role));

        assertTrue(keycloakAssignRoleService.hasManagerRole(EMPLOYEE_CODE, roleName));
    }

    @Test
    void getUsersByRoles_shouldReturnUsersWithMatchingRoles() {
        String roleName = "developer";

        UserRepresentation user = new UserRepresentation();
        user.setId(USER_ID);
        user.setUsername(EMPLOYEE_CODE);
        user.setFirstName("John");

        RoleRepresentation role = new RoleRepresentation();
        role.setName(roleName);

        Employee emp = new Employee();
        emp.setKeycloakUserId(USER_ID);

        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.list()).thenReturn(List.of(user));
        when(employeeRepository.findAllByIsActiveTrue()).thenReturn(List.of(emp));
        when(usersResource.get(USER_ID)).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(realmLevelResource);
        when(realmLevelResource.listEffective()).thenReturn(List.of(role));

        Map<String, String> result = keycloakAssignRoleService.getUsersByRoles(List.of(roleName));
        assertEquals(1, result.size());
        assertEquals("John", result.get(EMPLOYEE_CODE));
    }

    @Test
    void assignRealmRoles_shouldThrowIfUserNotFound() {
        when(usersResource.search(EMPLOYEE_CODE)).thenReturn(Collections.emptyList());

        TimesheetException ex = assertThrows(TimesheetException.class, () ->
                keycloakAssignRoleService.assignRealmRoles(EMPLOYEE_CODE, List.of("admin"))
        );
        assertEquals(NOT_FOUND_ERROR, ex.getErrorCode());
    }
}
