//package com.example.IdentityManagementService.service;
//
//import com.example.IdentityManagementService.Service.ServiceImpl.KeycloakAssignRoleServiceImpl;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.keycloak.admin.client.Keycloak;
//import org.keycloak.admin.client.resource.*;
//import org.keycloak.representations.idm.RoleRepresentation;
//import org.keycloak.representations.idm.UserRepresentation;
//import org.mockito.*;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.Collections;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class KeycloakAssignRoleServiceImplTest {
//
//    @InjectMocks
//    private KeycloakAssignRoleServiceImpl keycloakAssignRoleService;
//
//    @Mock
//    private Keycloak keycloak;
//
//    @Mock
//    private RealmResource realmResource;
//
//    @Mock
//    private UsersResource usersResource;
//
//    @Mock
//    private UserResource userResource;
//
//    @Mock
//    private RoleMappingResource roleMappingResource;
//
//    @Mock
//    private RoleScopeResource roleScopeResource;
//
//    @Mock
//    private RolesResource rolesResource;
//
//    @Mock
//    private RoleResource roleResource;
//
//    private final String realm = "your-realm";
//    private final String employeeCode = "emp123";
//    private final String userId = "user-uuid";
//    private final String roleName = "ROLE_MANAGER";
//
//    @BeforeEach
//    void setup() {
//        // Setup base mock chain
//        when(keycloak.realm(realm)).thenReturn(realmResource);
//        when(realmResource.users()).thenReturn(usersResource);
//        when(usersResource.search(employeeCode)).thenReturn(List.of(new UserRepresentation() {{
//            setId(userId);
//        }}));
//        when(usersResource.get(userId)).thenReturn(userResource);
//
//        when(userResource.roles()).thenReturn(roleMappingResource);
//        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
//
//        when(realmResource.roles()).thenReturn(rolesResource);
//        when(rolesResource.get(roleName)).thenReturn(roleResource);
//
//        RoleRepresentation mockRole = new RoleRepresentation();
//        mockRole.setName(roleName);
//        when(roleResource.toRepresentation()).thenReturn(mockRole);
//    }
//
//    @Test
//    void testAssignRealmRoles_success() {
//        // No matching role initially assigned
//        when(roleScopeResource.listEffective()).thenReturn(List.of());
//
//        // Act
//        keycloakAssignRoleService.assignRealmRoles(employeeCode, List.of(roleName));
//
//        // Assert
//        verify(roleScopeResource).add(argThat(roles ->
//                roles.size() == 1 && roles.get(0).getName().equals(roleName)
//        ));
//    }
//
//    @Test
//    void testUnassignRealmRoles_success() {
//        RoleRepresentation mockRole = new RoleRepresentation();
//        mockRole.setName(roleName);
//
//        when(roleScopeResource.listEffective()).thenReturn(List.of(mockRole));
//
//        // Act
//        keycloakAssignRoleService.unassignRealmRoles(employeeCode, List.of(roleName));
//
//        // Assert
//        verify(roleScopeResource).remove(argThat(roles ->
//                roles.size() == 1 && roles.get(0).getName().equals(roleName)
//        ));
//    }
//
//    @Test
//    void testUpdateUserRoles_success() {
//        // Mock old role
//        RoleRepresentation oldRole = new RoleRepresentation();
//        oldRole.setName("OLD_ROLE");
//
//        // Mock new role
//        RoleRepresentation newRole = new RoleRepresentation();
//        newRole.setName(roleName);
//
//        when(roleScopeResource.listEffective()).thenReturn(List.of(oldRole));
//        when(rolesResource.get("OLD_ROLE").toRepresentation()).thenReturn(oldRole);
//        when(rolesResource.get(roleName).toRepresentation()).thenReturn(newRole);
//
//        // Act
//        keycloakAssignRoleService.updateUserRoles(employeeCode, List.of(roleName), List.of("OLD_ROLE"));
//
//        // Assert remove old, add new
//        verify(roleScopeResource).remove(List.of(oldRole));
//        verify(roleScopeResource).add(List.of(newRole));
//    }
//
//    @Test
//    void testHasManagerRole_true() {
//        RoleRepresentation mockRole = new RoleRepresentation();
//        mockRole.setName("ROLE_MANAGER");
//
//        when(roleScopeResource.listEffective()).thenReturn(List.of(mockRole));
//
//        // Act
//        boolean result = keycloakAssignRoleService.hasManagerRole(employeeCode, "ROLE_MANAGER");
//
//        // Assert
//        assertTrue(result);
//    }
//
//    @Test
//    void testHasManagerRole_false() {
//        RoleRepresentation mockRole = new RoleRepresentation();
//        mockRole.setName("ROLE_EMPLOYEE");
//
//        when(roleScopeResource.listEffective()).thenReturn(List.of(mockRole));
//
//        // Act
//        boolean result = keycloakAssignRoleService.hasManagerRole(employeeCode, "ROLE_MANAGER");
//
//        // Assert
//        assertFalse(result);
//    }
//
//    @Test
//    void testGetAssignedRealmRoles_filtersOutDefaultRoles() {
//        RoleRepresentation mockRole1 = new RoleRepresentation();
//        mockRole1.setName("default-role");
//
//        RoleRepresentation mockRole2 = new RoleRepresentation();
//        mockRole2.setName("ROLE_USER");
//
//        when(roleScopeResource.listEffective()).thenReturn(List.of(mockRole1, mockRole2));
//
//        // Act
//        List<String> assigned = keycloakAssignRoleService.getAssignedRealmRoles(employeeCode);
//
//        // Assert: only ROLE_USER remains
//        assertEquals(1, assigned.size());
//        assertTrue(assigned.contains("ROLE_USER"));
//    }
//}
