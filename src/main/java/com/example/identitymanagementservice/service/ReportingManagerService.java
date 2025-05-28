package com.example.identitymanagementservice.service;

public interface ReportingManagerService {
    String addReportingManagerToEmployee(String employeeCode, String managerCode);
    String getManagerNameByEmployeeCode(String employeeCode);
}
