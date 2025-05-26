package com.example.IdentityManagementService.Service;

public interface ReportingManagerService {
    String addReportingManagerToEmployee(String employeeCode, String managerCode);
    String getManagerNameByEmployeeCode(String employeeCode);
}
