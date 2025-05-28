package com.example.IdentityManagementService.service;

import com.example.IdentityManagementService.Repository.EmployeeRepository;
import com.example.IdentityManagementService.Service.ReportingManagerService;
import com.example.IdentityManagementService.Service.ServiceImpl.KeycloakAssignRoleServiceImpl;
import com.example.IdentityManagementService.Service.ServiceImpl.ReportingManagerServiceImpl;
import com.example.IdentityManagementService.exceptions.TimesheetException;
import com.example.IdentityManagementService.model.Employee;
import com.example.IdentityManagementService.common.constants.MessageConstants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.*;

import static com.example.IdentityManagementService.common.constants.ErrorCode.*;
import static com.example.IdentityManagementService.common.constants.ErrorMessage.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportingManagerServiceImplTest {

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
        String employeeCode = "EMP001";
        String managerCode = "MGR001";

        Employee employee = new Employee();
        employee.setEmployeeCode(employeeCode);
        employee.setActive(true);

        when(keycloakAssignRoleService.getUsersByRoles(List.of("ReportingManager")))
                .thenReturn(Map.of(managerCode, "Manager Name"));

        when(employeeRepository.findByEmployeeCodeAndIsActiveTrue(employeeCode))
                .thenReturn(Optional.of(employee));

        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String result = reportingManagerService.addReportingManagerToEmployee(employeeCode, managerCode);

        assertEquals("Reporting manager is assigned to employee", result);
        assertEquals(managerCode, employee.getManagerCode());
    }

    @Test
    void testAddReportingManager_ManagerRoleMissing_ThrowsException() {
        String employeeCode = "EMP001";
        String managerCode = "MGR001";

        when(keycloakAssignRoleService.getUsersByRoles(List.of("ReportingManager")))
                .thenReturn(Map.of());

        TimesheetException exception = assertThrows(TimesheetException.class, () ->
                reportingManagerService.addReportingManagerToEmployee(employeeCode, managerCode));

        assertEquals(FORBIDDEN_ERROR, exception.getErrorCode());
        assertEquals(ROLE_NOT_FOUND, exception.getMessage());
    }

    @Test
    void testGetManagerNameByEmployeeCode_Success() {
        String employeeCode = "EMP001";
        String managerCode = "MGR001";

        Employee employee = new Employee();
        employee.setEmployeeCode(employeeCode);
        employee.setManagerCode(managerCode);
        employee.setActive(true);

        Employee manager = new Employee();
        manager.setEmployeeCode(managerCode);
        manager.setFirstName("John");
        manager.setLastName("Doe");
        manager.setActive(true);

        when(employeeRepository.findByEmployeeCodeAndIsActiveTrue(employeeCode))
                .thenReturn(Optional.of(employee));

        when(employeeRepository.findByEmployeeCodeAndIsActiveTrue(managerCode))
                .thenReturn(Optional.of(manager));

        String result = reportingManagerService.getManagerNameByEmployeeCode(employeeCode);

        assertEquals("John Doe", result);
    }

    @Test
    void testGetManagerNameByEmployeeCode_ManagerNotAssigned() {
        String employeeCode = "EMP001";

        Employee employee = new Employee();
        employee.setEmployeeCode(employeeCode);
        employee.setManagerCode(null);
        employee.setActive(true);

        when(employeeRepository.findByEmployeeCodeAndIsActiveTrue(employeeCode))
                .thenReturn(Optional.of(employee));

        String result = reportingManagerService.getManagerNameByEmployeeCode(employeeCode);

        assertEquals(MessageConstants.MANAGER_NOT_ASSIGNED, result);
    }

    @Test
    void testGetManagerNameByEmployeeCode_ManagerNotFound_ThrowsException() {
        String employeeCode = "EMP001";
        String managerCode = "MGR001";

        Employee employee = new Employee();
        employee.setEmployeeCode(employeeCode);
        employee.setManagerCode(managerCode);
        employee.setActive(true);

        when(employeeRepository.findByEmployeeCodeAndIsActiveTrue(employeeCode))
                .thenReturn(Optional.of(employee));

        when(employeeRepository.findByEmployeeCodeAndIsActiveTrue(managerCode))
                .thenReturn(Optional.empty());

        TimesheetException exception = assertThrows(TimesheetException.class, () ->
                reportingManagerService.getManagerNameByEmployeeCode(employeeCode));

        assertEquals(NOT_FOUND_ERROR, exception.getErrorCode());
        assertEquals("Reporting manager not found " + managerCode, exception.getMessage());
    }
}
