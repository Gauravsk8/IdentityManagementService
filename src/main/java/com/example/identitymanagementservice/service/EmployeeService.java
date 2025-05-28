package com.example.identitymanagementservice.service;

import com.example.identitymanagementservice.dto.request.response.UserResponseDto;
import com.example.identitymanagementservice.dto.request.UserIdentityDto;
import com.example.identitymanagementservice.dto.request.pagenationdto.FilterRequest;
import com.example.identitymanagementservice.dto.request.pagenationdto.SortRequest;
import com.example.identitymanagementservice.dto.request.pagenationdto.response.PagedResponse;

import java.util.List;
import java.util.Map;

public interface EmployeeService {
    UserIdentityDto getUserByEmployeeCodedb(String employeeCode);
    void updateActiveStatus(String employeeCode, boolean isActive);

    UserIdentityDto getUserByKeycloakUserId(String keycloakUserId);


     PagedResponse<UserResponseDto> getAllUsers(
            int offset,
            int limit,
            List<FilterRequest> filters,
            List<SortRequest> sorts);

    List<Map<String, String>> getAllUsersList();
    List<UserIdentityDto> getActiveEmployeesUnderManager(String managerCode);

}
