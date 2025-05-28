package com.example.identitymanagementservice.service.service.impl;

import com.example.identitymanagementservice.repository.EmployeeRepository;
import com.example.identitymanagementservice.service.EmployeeService;
import com.example.identitymanagementservice.dto.request.response.UserResponseDto;
import com.example.identitymanagementservice.dto.request.UserIdentityDto;
import com.example.identitymanagementservice.exceptions.TimesheetException;
import com.example.identitymanagementservice.model.Employee;
import com.example.identitymanagementservice.utils.SortUtil;
import com.example.identitymanagementservice.common.constants.ErrorMessage;
import com.example.identitymanagementservice.dto.request.pagenationdto.FilterRequest;
import com.example.identitymanagementservice.dto.request.pagenationdto.SortRequest;
import com.example.identitymanagementservice.dto.request.pagenationdto.response.PagedResponse;
import com.example.identitymanagementservice.utils.FilterSpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;


import java.util.stream.Collectors;

import static com.example.identitymanagementservice.common.constants.ErrorCode.NOT_FOUND_ERROR;
import static com.example.identitymanagementservice.common.constants.ErrorMessage.USER_NOT_FOUND;


@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Override
    public UserIdentityDto getUserByEmployeeCodedb(String employeeCode) {
        Employee user = employeeRepository.findByEmployeeCodeAndIsActiveTrue(employeeCode)
                .orElseThrow(() -> new TimesheetException(NOT_FOUND_ERROR, USER_NOT_FOUND + employeeCode));

        UserIdentityDto dto = new UserIdentityDto();
        dto.setKeycloakUserId(user.getKeycloakUserId());
        dto.setEmployeeCode(user.getEmployeeCode());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setManagerCode(user.getManagerCode());
        dto.setEmployeeType(user.getEmployeeType());

        return dto;
    }

    public void updateActiveStatus(String employeeCode, boolean isActive) {
        Employee employee = employeeRepository.findById(employeeCode)
                .orElseThrow(() -> new TimesheetException(NOT_FOUND_ERROR, USER_NOT_FOUND));

        employee.setActive(isActive);
        employeeRepository.save(employee);
    }

    @Override
    public UserIdentityDto getUserByKeycloakUserId(String keycloakUserId) {
        Employee employee = employeeRepository.findByKeycloakUserIdAndIsActiveTrue(keycloakUserId)
                .orElseThrow(() -> new TimesheetException(NOT_FOUND_ERROR, USER_NOT_FOUND + keycloakUserId));

        UserIdentityDto dto = new UserIdentityDto();
        dto.setEmployeeCode(employee.getEmployeeCode());
        dto.setFirstName(employee.getFirstName());
        dto.setLastName(employee.getLastName());
        dto.setEmail(employee.getEmail());
        dto.setEmployeeType(employee.getEmployeeType());
        dto.setManagerCode(employee.getManagerCode());
        return dto;
    }


    @Override
    public PagedResponse<UserResponseDto> getAllUsers(
            int offset,
            int limit,
            List<FilterRequest> filters,
            List<SortRequest> sorts) {

        if (offset < 0){
            offset = 0;
        }
        if (limit <= 0){
            limit = 10;
        }

        int page = offset / limit;

        Sort sort = SortUtil.getSort(sorts);

        Pageable pageable = PageRequest.of(page, limit, sort);

        Specification<Employee> dynamicSpec = new FilterSpecificationBuilder<Employee>().build(filters);

        Specification<Employee> isActiveSpec = (root, query, cb) ->
                cb.isTrue(root.get("isActive"));

        Specification<Employee> finalSpec = Specification.where(isActiveSpec).and(dynamicSpec);

        Page<Employee> employeePage = employeeRepository.findAll(finalSpec, pageable);

        if (employeePage.isEmpty()) {
            throw new TimesheetException(
                    NOT_FOUND_ERROR,
                    ErrorMessage.NO_ACTIVE_USERS_FOUND
            );
        }

        List<UserResponseDto> content = employeePage.getContent().stream()
                .map(emp -> new UserResponseDto(
                        emp.getEmployeeCode(),
                        emp.getFirstName(),
                        emp.getLastName(),
                        emp.getEmail(),
                        emp.getManagerCode(),
                        emp.getEmployeeType()
                ))
                .toList();

        return new PagedResponse<>(
                content,
                employeePage.getNumber(),
                employeePage.getSize(),
                employeePage.getTotalElements()
        );
    }


    @Override
    public List<Map<String, String>> getAllUsersList() {
        List<Employee> employees = employeeRepository.findAllByIsActiveTrue();
        List<Map<String, String>> userList = new ArrayList<>();

        for (Employee employee : employees) {
            Map<String, String> userMap = new HashMap<>();
            userMap.put("employeeCode", employee.getEmployeeCode());
            userMap.put("firstName", employee.getFirstName());
            userMap.put("lastName", employee.getLastName());
            userMap.put("email", employee.getEmail());
            userMap.put("managerCode", employee.getManagerCode());
            userMap.put("employeeType", employee.getEmployeeType());
            userList.add(userMap);
        }

        return userList;
    }




    @Override
    public List<UserIdentityDto> getActiveEmployeesUnderManager(String managerCode) {
        List<Employee> employees = employeeRepository.findByManagerCodeAndIsActiveTrue(managerCode);
        return mapToUserIdentityDtos(employees);
    }

    private List<UserIdentityDto> mapToUserIdentityDtos(List<Employee> employees) {
        return employees.stream()
                .map(this::mapToUserIdentityDto)
                .collect(Collectors.toList());
    }

    private UserIdentityDto mapToUserIdentityDto(Employee employee) {
        return UserIdentityDto.builder()
                .keycloakUserId(employee.getKeycloakUserId())
                .employeeCode(employee.getEmployeeCode())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .employeeType(employee.getEmployeeType())
                .managerCode(employee.getManagerCode())
                .build();
    }


}
