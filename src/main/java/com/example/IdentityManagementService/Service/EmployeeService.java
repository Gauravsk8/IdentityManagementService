package com.example.IdentityManagementService.Service;

import com.example.IdentityManagementService.dto.request.response.UserResponseDto;
import com.example.IdentityManagementService.dto.request.UserIdentityDto;
import com.example.IdentityManagementService.dto.request.pagenationDto.FilterRequest;
import com.example.IdentityManagementService.dto.request.pagenationDto.SortRequest;
import com.example.IdentityManagementService.dto.request.pagenationDto.response.PagedResponse;

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
