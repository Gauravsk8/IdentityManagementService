package com.example.identitymanagementservice.common.constants;

public final class ErrorMessage {

    // === Authentication & Authorization Errors ===
    public static final String MISSING_BEARER_TOKEN = "Missing Bearer token";
    public static final String MALFORMED_BEARER_TOKEN = "Access token is expired or malformed";
    public static final String UNAUTHORIZED_ACCESS = "User not authorized to access the resource";
    public static final String USERID_EXTRACTION_FAILED = "Failed to extract user ID";

    // === Keycloak Errors ===
    public static final String KEYCLOAK_USER_ALREADY_EXISTS = "Keycloak user already exists: ";
    public static final String KEYCLOAK_ADMIN_CONNECTION_FAILED = "Keycloak admin connection failed";
    public static final String USER_UPDATE_FAILED = "User update failed";
    public static final String PASSWORD_UPDATE_FAILED = "Password update failed";

    // === User & Role Errors ===
    public static final String USER_NOT_FOUND = "User not found";
    public static final String ROLE_NOT_FOUND = "Role not found";
    public static final String ROLE_NOT_ASSIGNED = "Role not assigned";
    public static final String ROLE_ALREADY_ASSIGNED = "Role already assigned";

    // === Employee Errors ===
    public static final String EMPLOYEE_ALREADY_EXISTS = "Employee already exists with %s";
    public static final String EMPLOYEE_SAVE_FAILED = "Failed to save employee to database";
    public static final String EMPLOYEE_CREATION_FAILED_LOG = "Error creating employee";

    // === Reporting Manager Errors ===
    public static final String REPORTING_MANAGER_ASSIGN_FAILED = "Failed to assign reporting manager to employee";
    public static final String REPORTING_MANAGER_ASSIGNED = "Reporting manager is assigned to employee";
    public static final String RM_NOT_ASSIGNED = "Reporting manager not assigned for employee: %s";
    public static final String RM_NOT_FOUND = "Reporting manager not found ";
    public static final String NO_MANAGER_ASSIGNED = "No manager assigned";

    // === Timesheet & Assignment Errors ===
    public static final String DAILY_TIMESHEET_NOT_FOUND =
            "Daily time sheets not found for this employee between these dates, unable to fetch weekly hours spent";
    public static final String TIMESHEET_SUMMARY_NOT_FOUND =
            "No weekly summary found for employee %s (week %s)";
    public static final String ASSIGNMENT_NOT_FOUND =
            "Assignment not found for project '%s' and employee '%s'";
    public static final String DAILY_TIME_SHEET_NOT_FOUND_FOR_EMPLOYEE_WITHIN_DATES = "Daily Timesheet Not Found For employees Within dates";


    // === Project, Client, Cost Center Errors ===
    public static final String CLIENT_NOT_FOUND = "Client not found with id: %s";
    public static final String COST_CENTER_NOT_FOUND = "Cost center not found with code: %s";
    public static final String PROJECT_NOT_FOUND = "Project not found with code: %s";
    public static final String PROJECT_ALREADY_EXISTS = "Project with code '%s' already exists";
    public static final String NO_ACTIVE_CLIENTS_FOUND = "No active clients found";
    public static final String NO_ACTIVE_COST_CENTERS_FOUND = "No active cost centers found";
    public static final String NO_ACTIVE_PROJECTS_FOUND = "No active projects found";

    // === Date Validation Errors ===
    public static final String START_DATE_REQUIRED = "Start date is required for employee %s";
    public static final String EMP_START_BEFORE_PROJECT =
            "Start date for employee %s is before the project %s start date";
    public static final String EMP_END_AFTER_PROJECT =
            "End date for employee %s is after the project %s end date";
    public static final String END_BEFORE_START = "End date is before start date for employee %s";

    // === Generic / Fallback Errors ===
    public static final String STATUS_NOT_FOUND = "Status not found";
    public static final String NO_ACTIVE_USERS_FOUND = "No active users found";
    public static final String PROJECT_ROLE_ALREADY_CREATED = "Project Role already Created";

    private ErrorMessage() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
