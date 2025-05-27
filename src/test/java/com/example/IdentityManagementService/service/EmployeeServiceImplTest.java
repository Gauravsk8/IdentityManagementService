package com.example.IdentityManagementService.service;

import com.example.IdentityManagementService.Repository.EmployeeRepository;
import com.example.IdentityManagementService.Service.ServiceImpl.EmployeeServiceImpl;
import com.example.IdentityManagementService.dto.request.UserIdentityDto;
import com.example.IdentityManagementService.exceptions.TimesheetException;
import com.example.IdentityManagementService.model.Employee;
import com.example.IdentityManagementService.utils.FilterSpecificationBuilder;
import com.example.IdentityManagementService.utils.SortUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.*;

import static com.example.IdentityManagementService.common.constants.ErrorCode.NOT_FOUND_ERROR;
import static com.example.IdentityManagementService.common.constants.ErrorMessage.USER_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    @Mock
    private EmployeeRepository employeeRepository;

    @Spy
    private FilterSpecificationBuilder<Employee> filterSpecificationBuilder = new FilterSpecificationBuilder<>();

    @Mock
    private SortUtil sortUtil;

    private UserIdentityDto userDto;

    @BeforeEach
    void setUp() {
        userDto = UserIdentityDto.builder()
                .keycloakUserId("keycloak-123")
                .employeeCode("E123")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .managerCode("M123")
                .employeeType("FullTime")
                .build();
    }

    private Employee createEmployeeFromDto(UserIdentityDto dto) {
        Employee e = new Employee();
        e.setKeycloakUserId(dto.getKeycloakUserId());
        e.setEmployeeCode(dto.getEmployeeCode());
        e.setFirstName(dto.getFirstName());
        e.setLastName(dto.getLastName());
        e.setEmail(dto.getEmail());
        e.setManagerCode(dto.getManagerCode());
        e.setEmployeeType(dto.getEmployeeType());
        e.setActive(true);
        return e;
    }

    @Test
    void testGetUserByEmployeeCodedb_UserFound() {
        Employee employee = createEmployeeFromDto(userDto);

        when(employeeRepository.findByEmployeeCodeAndIsActiveTrue("E123"))
                .thenReturn(Optional.of(employee));

        UserIdentityDto dto = employeeService.getUserByEmployeeCodedb("E123");

        assertNotNull(dto);
        assertEquals(userDto.getKeycloakUserId(), dto.getKeycloakUserId());
        assertEquals(userDto.getEmployeeCode(), dto.getEmployeeCode());
        assertEquals(userDto.getFirstName(), dto.getFirstName());
        assertEquals(userDto.getLastName(), dto.getLastName());
        assertEquals(userDto.getEmail(), dto.getEmail());
        assertEquals(userDto.getManagerCode(), dto.getManagerCode());
        assertEquals(userDto.getEmployeeType(), dto.getEmployeeType());
    }

    @Test
    void testGetUserByEmployeeCodedb_UserNotFound() {
        when(employeeRepository.findByEmployeeCodeAndIsActiveTrue("E123"))
                .thenReturn(Optional.empty());

        TimesheetException ex = assertThrows(TimesheetException.class,
                () -> employeeService.getUserByEmployeeCodedb("E123"));

        assertEquals(NOT_FOUND_ERROR, ex.getErrorCode());
        assertTrue(ex.getMessage().contains(USER_NOT_FOUND + "E123"));
    }

    @Test
    void testUpdateActiveStatus_UserFound() {
        Employee employee = createEmployeeFromDto(userDto);

        when(employeeRepository.findById("E123")).thenReturn(Optional.of(employee));

        employeeService.updateActiveStatus("E123", false);

        assertFalse(employee.isActive());
        verify(employeeRepository).save(employee);
    }

    @Test
    void testUpdateActiveStatus_UserNotFound() {
        when(employeeRepository.findById("E123")).thenReturn(Optional.empty());

        TimesheetException ex = assertThrows(TimesheetException.class,
                () -> employeeService.updateActiveStatus("E123", true));

        assertEquals(NOT_FOUND_ERROR, ex.getErrorCode());
        assertEquals(USER_NOT_FOUND, ex.getMessage());
    }

    @Test
    void testGetUserByKeycloakUserId_UserFound() {
        Employee employee = createEmployeeFromDto(userDto);

        when(employeeRepository.findByKeycloakUserIdAndIsActiveTrue("keycloak-123"))
                .thenReturn(Optional.of(employee));

        UserIdentityDto dto = employeeService.getUserByKeycloakUserId("keycloak-123");

        assertNotNull(dto);
        assertEquals(userDto.getEmployeeCode(), dto.getEmployeeCode());
        assertEquals(userDto.getFirstName(), dto.getFirstName());
    }

    @Test
    void testGetUserByKeycloakUserId_UserNotFound() {
        when(employeeRepository.findByKeycloakUserIdAndIsActiveTrue("keycloak-123"))
                .thenReturn(Optional.empty());

        TimesheetException ex = assertThrows(TimesheetException.class,
                () -> employeeService.getUserByKeycloakUserId("keycloak-123"));

        assertEquals(NOT_FOUND_ERROR, ex.getErrorCode());
        assertTrue(ex.getMessage().contains(USER_NOT_FOUND + "keycloak-123"));
    }

    @Test
    void testGetAllUsersList_Success() {
        Employee employee = createEmployeeFromDto(userDto);

        when(employeeRepository.findAllByIsActiveTrue()).thenReturn(List.of(employee));

        List<Map<String, String>> result = employeeService.getAllUsersList();

        assertEquals(1, result.size());
        assertEquals(userDto.getEmployeeCode(), result.get(0).get("employeeCode"));
        assertEquals(userDto.getFirstName(), result.get(0).get("firstName"));
    }

    @Test
    void testGetActiveEmployeesUnderManager_Success() {
        Employee employee = createEmployeeFromDto(userDto);

        when(employeeRepository.findByManagerCodeAndIsActiveTrue("M123"))
                .thenReturn(List.of(employee));

        List<UserIdentityDto> result = employeeService.getActiveEmployeesUnderManager("M123");

        assertEquals(1, result.size());
        assertEquals(userDto.getEmployeeCode(), result.get(0).getEmployeeCode());
    }
}
