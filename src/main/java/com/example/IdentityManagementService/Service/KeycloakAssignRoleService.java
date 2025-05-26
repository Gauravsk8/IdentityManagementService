package com.example.IdentityManagementService.Service;

import java.util.List;
import java.util.Map;

public interface KeycloakAssignRoleService {

    void assignRealmRoles(String employeeCode, List<String> roles);
    void unassignRealmRoles(String employeeCode, List<String> roles);


    List<String> getAssignedRealmRoles(String employeeCode);

    Map<String, String> getUsersByRoles(List<String> roleNames);

    void updateUserRoles(String employeeCode, List<String> rolesToAssign, List<String> rolesToRemove);

    boolean hasManagerRole(String employeeCode, String role);
}
