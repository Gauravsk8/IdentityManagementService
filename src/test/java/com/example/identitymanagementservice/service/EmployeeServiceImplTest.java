package com.example.identitymanagementservice.service;

import com.example.identitymanagementservice.dto.request.UserIdentityDto;
import com.example.identitymanagementservice.exceptions.TimesheetException;
import com.example.identitymanagementservice.model.Employee;
import com.example.identitymanagementservice.repository.EmployeeRepository;
import com.example.identitymanagementservice.service.service.impl.EmployeeServiceImpl;
import com.example.identitymanagementservice.utils.FilterSpecificationBuilder;
import com.example.identitymanagementservice.utils.SortUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.example.identitymanagementservice.common.constants.ErrorCode.NOT_FOUND_ERROR;
import static com.example.identitymanagementservice.common.constants.ErrorMessage.USER_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    private static final String KEYCLOAK_USER_ID = "keycloak-123";
    private static final String EMPLOYEE_CODE = "E123";

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
                .keycloakUserId(KEYCLOAK_USER_ID)
                .employeeCode(EMPLOYEE_CODE)
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

        when(employeeRepository.findByEmployeeCodeAndIsActiveTrue(EMPLOYEE_CODE))
                .thenReturn(Optional.of(employee));

        UserIdentityDto dto = employeeService.getUserByEmployeeCodedb(EMPLOYEE_CODE);

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
        when(employeeRepository.findByEmployeeCodeAndIsActiveTrue(EMPLOYEE_CODE))
                .thenReturn(Optional.empty());

        TimesheetException ex = assertThrows(TimesheetException.class,
                () -> employeeService.getUserByEmployeeCodedb(EMPLOYEE_CODE));

        assertEquals(NOT_FOUND_ERROR, ex.getErrorCode());
        assertTrue(ex.getMessage().contains(USER_NOT_FOUND + EMPLOYEE_CODE));
    }

    @Test
    void testUpdateActiveStatus_UserFound() {
        Employee employee = createEmployeeFromDto(userDto);

        when(employeeRepository.findById(EMPLOYEE_CODE)).thenReturn(Optional.of(employee));

        employeeService.updateActiveStatus(EMPLOYEE_CODE, false);

        assertFalse(employee.isActive());
        verify(employeeRepository).save(employee);
    }

    @Test
    void testUpdateActiveStatus_UserNotFound() {
        when(employeeRepository.findById(EMPLOYEE_CODE)).thenReturn(Optional.empty());

        TimesheetException ex = assertThrows(TimesheetException.class,
                () -> employeeService.updateActiveStatus(EMPLOYEE_CODE, true));

        assertEquals(NOT_FOUND_ERROR, ex.getErrorCode());
        assertEquals(USER_NOT_FOUND, ex.getMessage());
    }

    @Test
    void testGetUserByKeycloakUserId_UserFound() {
        Employee employee = createEmployeeFromDto(userDto);

        when(employeeRepository.findByKeycloakUserIdAndIsActiveTrue(KEYCLOAK_USER_ID))
                .thenReturn(Optional.of(employee));

        UserIdentityDto dto = employeeService.getUserByKeycloakUserId(KEYCLOAK_USER_ID);

        assertNotNull(dto);
        assertEquals(userDto.getEmployeeCode(), dto.getEmployeeCode());
        assertEquals(userDto.getFirstName(), dto.getFirstName());
    }

    @Test
    void testGetUserByKeycloakUserId_UserNotFound() {
        when(employeeRepository.findByKeycloakUserIdAndIsActiveTrue(KEYCLOAK_USER_ID))
                .thenReturn(Optional.empty());

        TimesheetException ex = assertThrows(TimesheetException.class,
                () -> employeeService.getUserByKeycloakUserId(KEYCLOAK_USER_ID));

        assertEquals(NOT_FOUND_ERROR, ex.getErrorCode());
        assertTrue(ex.getMessage().contains(USER_NOT_FOUND + KEYCLOAK_USER_ID));
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
