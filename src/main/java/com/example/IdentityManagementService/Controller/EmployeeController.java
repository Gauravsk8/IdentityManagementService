package com.example.IdentityManagementService.Controller;

import com.example.IdentityManagementService.Service.EmployeeService;
import com.example.IdentityManagementService.Service.ReportingManagerService;
import com.example.IdentityManagementService.common.annotations.RequiresKeycloakAuthorization;
import com.example.IdentityManagementService.common.constants.MessageConstants;
import com.example.IdentityManagementService.dto.request.AssignRMRequest;
import com.example.IdentityManagementService.dto.request.response.UserResponseDto;
import com.example.IdentityManagementService.dto.request.UserIdentityDto;

import com.example.IdentityManagementService.dto.request.pagenationDto.FilterRequest;
import com.example.IdentityManagementService.dto.request.pagenationDto.SortRequest;
import com.example.IdentityManagementService.dto.request.pagenationDto.response.PagedResponse;
import com.example.IdentityManagementService.utils.FilterUtil;
import com.example.IdentityManagementService.utils.SortUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ims")
@RequiredArgsConstructor
public class EmployeeController {

    private final ReportingManagerService employeeRmService;
    private final EmployeeService employeeService;


    //get EmployeeDetails form employeeCode
    @GetMapping("/users/{employeeCode}")
    @RequiresKeycloakAuthorization(resource = "tms:com", scope = "tms:com:get")
    public ResponseEntity<UserIdentityDto> getUserByEmployeeCode(@PathVariable String employeeCode) {
        UserIdentityDto dto = employeeService.getUserByEmployeeCodedb(employeeCode);
        return ResponseEntity.ok(dto);
    }

    //get My details
    @GetMapping("/users/my")
    @RequiresKeycloakAuthorization(resource = "idms:user", scope = "idms:user:get")
    public ResponseEntity<UserIdentityDto> getOwnProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String keycloakUserId = authentication.getName(); // This returns Keycloak UUID

        UserIdentityDto userProfile = employeeService.getUserByKeycloakUserId(keycloakUserId);
        return ResponseEntity.ok(userProfile);
    }
    //get all employees
    @GetMapping("/users")
    @RequiresKeycloakAuthorization(resource = "manager:com", scope = "com:manager:get")
    public ResponseEntity<PagedResponse<UserResponseDto>> getAllUsers(
            @RequestParam int offset,
            @RequestParam int limit,
            @RequestParam Map<String, String> allParams,
            @RequestParam(required = false, name = "sort") String sortParam) {

        List<FilterRequest> filters = FilterUtil.parseFilters(allParams);
        List<SortRequest> sorts = SortUtil.parseSort(sortParam);

        return ResponseEntity.ok(employeeService.getAllUsers(offset, limit, filters, sorts));
    }

    @GetMapping("/users/all")
    @RequiresKeycloakAuthorization(resource = "manager:com", scope = "com:manager:get")
    public ResponseEntity<List<Map<String, String>>> getAllUsers() {
        List<Map<String, String>> users = employeeService.getAllUsersList();
        return ResponseEntity.ok(users);
    }


    //Delete User
    @PutMapping("users/{employeeCode}/status")
    @RequiresKeycloakAuthorization(resource = "idms:admin", scope = "idms:user:update")
    public ResponseEntity<String> updateActiveStatus(
            @PathVariable String employeeCode,
            @RequestParam boolean active
    ) {
        employeeService.updateActiveStatus(employeeCode, active);
        return ResponseEntity.ok(MessageConstants.USER_STATUS_UPDATED);
    }


    //get managerName for employeeCode
    @GetMapping("/users/{employeeCode}/manager")
    @RequiresKeycloakAuthorization(resource = "tms:com", scope = "tms:com:get")
    public ResponseEntity<String> getManagerNameByEmployeeCode(
            @PathVariable String employeeCode
    ) {
        String managerName = employeeRmService.getManagerNameByEmployeeCode(employeeCode);
        return ResponseEntity.ok(managerName);
    }

    //assign Reporting Manager
    @PostMapping("/users/manager")
    @RequiresKeycloakAuthorization(resource = "idms:admin", scope = "idms:user:add")
    public ResponseEntity<String> assignReportingManager(@Valid @RequestBody AssignRMRequest request) {
        String response = employeeRmService.addReportingManagerToEmployee(
                request.getEmployeeCode(),
                request.getManagerCode()
        );
        return ResponseEntity.ok(response);
    }

    //Get Employees under RM
    @GetMapping("users/manager/{managerCode}")
    @RequiresKeycloakAuthorization(resource = "idms:adminrm", scope = "idms:user:get")
    public ResponseEntity<List<UserIdentityDto>> getEmployeesUnderManager(
            @PathVariable String managerCode) {
        List<UserIdentityDto> employees = employeeService.getActiveEmployeesUnderManager(managerCode);

        return ResponseEntity.ok(employees);

    }

}
