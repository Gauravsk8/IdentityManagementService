package com.example.identitymanagementservice.service;

import com.example.identitymanagementservice.common.constants.MessageConstants;
import com.example.identitymanagementservice.exceptions.TimesheetException;
import com.example.identitymanagementservice.model.Employee;
import com.example.identitymanagementservice.repository.EmployeeRepository;
import com.example.identitymanagementservice.service.service.impl.KeycloakAssignRoleServiceImpl;
import com.example.identitymanagementservice.service.service.impl.ReportingManagerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.example.identitymanagementservice.common.constants.ErrorCode.FORBIDDEN_ERROR;
import static com.example.identitymanagementservice.common.constants.ErrorCode.NOT_FOUND_ERROR;
import static com.example.identitymanagementservice.common.constants.ErrorMessage.ROLE_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ReportingManagerServiceImplTest {

    private static final String EMPLOYEE_CODE = "EMP001";
    private static final String MANAGER_CODE = "MGR001";

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private KeycloakAssignRoleServiceImpl keycloakAssignRoleService;

    @InjectMocks
    private ReportingManagerServiceImpl reportingManagerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAddReportingManager_Success() {
        Employee employee = new Employee();
        employee.setEmployeeCode(EMPLOYEE_CODE);
        employee.setActive(true);

        when(keycloakAssignRoleService.getUsersByRoles(List.of("ReportingManager")))
                .thenReturn(Map.of(MANAGER_CODE, "Manager Name"));

        when(employeeRepository.findByEmployeeCodeAndIsActiveTrue(EMPLOYEE_CODE))
                .thenReturn(Optional.of(employee));

        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String result = reportingManagerService.addReportingManagerToEmployee(EMPLOYEE_CODE, MANAGER_CODE);

        assertEquals("Reporting manager is assigned to employee", result);
        assertEquals(MANAGER_CODE, employee.getManagerCode());
    }

    @Test
    void testAddReportingManager_ManagerRoleMissing_ThrowsException() {
        when(keycloakAssignRoleService.getUsersByRoles(List.of("ReportingManager")))
                .thenReturn(Map.of());

        TimesheetException exception = assertThrows(TimesheetException.class, () ->
                reportingManagerService.addReportingManagerToEmployee(EMPLOYEE_CODE, MANAGER_CODE));

        assertEquals(FORBIDDEN_ERROR, exception.getErrorCode());
        assertEquals(ROLE_NOT_FOUND, exception.getMessage());
    }

    @Test
    void testGetManagerNameByEmployeeCode_Success() {
        Employee employee = new Employee();
        employee.setEmployeeCode(EMPLOYEE_CODE);
        employee.setManagerCode(MANAGER_CODE);
        employee.setActive(true);

        Employee manager = new Employee();
        manager.setEmployeeCode(MANAGER_CODE);
        manager.setFirstName("John");
        manager.setLastName("Doe");
        manager.setActive(true);

        when(employeeRepository.findByEmployeeCodeAndIsActiveTrue(EMPLOYEE_CODE))
                .thenReturn(Optional.of(employee));

        when(employeeRepository.findByEmployeeCodeAndIsActiveTrue(MANAGER_CODE))
                .thenReturn(Optional.of(manager));

        String result = reportingManagerService.getManagerNameByEmployeeCode(EMPLOYEE_CODE);

        assertEquals("John Doe", result);
    }

    @Test
    void testGetManagerNameByEmployeeCode_ManagerNotAssigned() {
        Employee employee = new Employee();
        employee.setEmployeeCode(EMPLOYEE_CODE);
        employee.setManagerCode(null);
        employee.setActive(true);

        when(employeeRepository.findByEmployeeCodeAndIsActiveTrue(EMPLOYEE_CODE))
                .thenReturn(Optional.of(employee));

        String result = reportingManagerService.getManagerNameByEmployeeCode(EMPLOYEE_CODE);

        assertEquals(MessageConstants.MANAGER_NOT_ASSIGNED, result);
    }

    @Test
    void testGetManagerNameByEmployeeCode_ManagerNotFound_ThrowsException() {
        Employee employee = new Employee();
        employee.setEmployeeCode(EMPLOYEE_CODE);
        employee.setManagerCode(MANAGER_CODE);
        employee.setActive(true);

        when(employeeRepository.findByEmployeeCodeAndIsActiveTrue(EMPLOYEE_CODE))
                .thenReturn(Optional.of(employee));

        when(employeeRepository.findByEmployeeCodeAndIsActiveTrue(MANAGER_CODE))
                .thenReturn(Optional.empty());

        TimesheetException exception = assertThrows(TimesheetException.class, () ->
                reportingManagerService.getManagerNameByEmployeeCode(EMPLOYEE_CODE));

        assertEquals(NOT_FOUND_ERROR, exception.getErrorCode());
        assertEquals("Reporting manager not found " + MANAGER_CODE, exception.getMessage());
    }
}
