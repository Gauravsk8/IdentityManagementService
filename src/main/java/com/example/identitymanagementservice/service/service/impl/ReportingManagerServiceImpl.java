package com.example.identitymanagementservice.service.service.impl;

import com.example.identitymanagementservice.repository.EmployeeRepository;
import com.example.identitymanagementservice.service.ReportingManagerService;
import com.example.identitymanagementservice.exceptions.TimesheetException;
import com.example.identitymanagementservice.model.Employee;
import com.example.identitymanagementservice.common.constants.MessageConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.List;

import static com.example.identitymanagementservice.common.constants.ErrorCode.FORBIDDEN_ERROR;
import static com.example.identitymanagementservice.common.constants.ErrorCode.NOT_FOUND_ERROR;
import static com.example.identitymanagementservice.common.constants.ErrorMessage.ROLE_NOT_FOUND;
import static com.example.identitymanagementservice.common.constants.ErrorMessage.USER_NOT_FOUND;
import static com.example.identitymanagementservice.common.constants.ErrorMessage.REPORTING_MANAGER_ASSIGN_FAILED;
import static com.example.identitymanagementservice.common.constants.ErrorMessage.REPORTING_MANAGER_ASSIGNED;
import static com.example.identitymanagementservice.common.constants.ErrorCode.KEYCLOAK_CONNECTION_ERROR;
import static com.example.identitymanagementservice.common.constants.ErrorMessage.RM_NOT_FOUND;



@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingManagerServiceImpl implements ReportingManagerService {

    private final EmployeeRepository employeeRepository;
    private final KeycloakAssignRoleServiceImpl keycloakAssignRoleService;

    @Override
    public String addReportingManagerToEmployee(String employeeCode, String managerCode) {
        boolean isReportingManager = checkIfManagerHasRole(managerCode, "ReportingManager");

        if (!isReportingManager) {
            throw new TimesheetException(FORBIDDEN_ERROR, ROLE_NOT_FOUND);
        }

        Employee employee = employeeRepository.findByEmployeeCodeAndIsActiveTrue(employeeCode)
                .orElseThrow(() -> new TimesheetException(NOT_FOUND_ERROR, USER_NOT_FOUND + employeeCode));

        employee.setManagerCode(managerCode);
        Employee savedEmployee = employeeRepository.save(employee);

        return savedEmployee.getEmployeeCode() != null
                ? REPORTING_MANAGER_ASSIGNED
                : REPORTING_MANAGER_ASSIGN_FAILED;
    }

    @Override
    public String getManagerNameByEmployeeCode(String employeeCode) {
        Employee employee = employeeRepository.findByEmployeeCodeAndIsActiveTrue(employeeCode)
                .orElseThrow(() -> new TimesheetException(NOT_FOUND_ERROR, USER_NOT_FOUND + employeeCode));

        String managerCode = employee.getManagerCode();

        if (managerCode == null || managerCode.isEmpty()) {
            return MessageConstants.MANAGER_NOT_ASSIGNED;
        }

        Employee manager = employeeRepository.findByEmployeeCodeAndIsActiveTrue(managerCode)
                .orElseThrow(() -> new TimesheetException(NOT_FOUND_ERROR, RM_NOT_FOUND + managerCode));

        return manager.getFirstName() + " " + manager.getLastName();
    }

    private boolean checkIfManagerHasRole(String managerCode, String role) {
        try {
            Map<String, String> usersWithRole = keycloakAssignRoleService.getUsersByRoles(List.of(role));
            return usersWithRole != null &&
                    usersWithRole.keySet().stream().anyMatch(username -> username.equalsIgnoreCase(managerCode));
        } catch (Exception e) {
            throw new TimesheetException(KEYCLOAK_CONNECTION_ERROR, KEYCLOAK_CONNECTION_ERROR, e);
        }
    }
}
