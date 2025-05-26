package com.example.IdentityManagementService.common.constants;

public final class ErrorCode {

    // === General Errors ===
    public static final String VALIDATION_ERROR = "TIMESHEET_VALIDATION_ERROR";
    public static final String INTERNAL_SERVER_ERROR = "TIMESHEET_INTERNAL_SERVER_ERROR";
    public static final String SERVICE_UNAVAILABLE_ERROR = "TIMESHEET_SERVICE_UNAVAILABLE_ERROR";
    public static final String CONFLICT_ERROR = "TIMESHEET_CONFLICT_ERROR";
    public static final String FORBIDDEN_ERROR = "TIMESHEET_FORBIDDEN_ERROR";
    public static final String UNAUTHORIZED_ERROR = "TIMESHEET_UNAUTHORIZED_ERROR";
    public static final String NOT_FOUND_ERROR = "TIMESHEET_NOT_FOUND_ERROR";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String FAILED_TO_FETCH_DETAILS = "FAILED_TO_FETCH_DETAILS";
    public static final String INVALID_INPUT = "INVALID_INPUT";
    public static final String INVALID_MONTH_YEAR_FORMAT = "INVALID_MONTH_YEAR_FORMAT";
    public static final String NOT_NULL = "NOT_NULL";

    // === Authentication & Authorization Errors ===
    public static final String MISSING_BEARER_TOKEN = "BEARER_TOKEN_MISSING";

    // === Keycloak Errors ===
    public static final String KEYCLOAK_USER_CREATION_FAILED = "KEYCLOAK_USER_CREATION_FAILED";
    public static final String KEYCLOAK_RESPONSE_PARSING_ERROR = "KEYCLOAK_RESPONSE_PARSING_ERROR";
    public static final String KEYCLOAK_USER_UPDATE_FAILED = "KEYCLOAK_USER_UPDATE_FAILED";
    public static final String KEYCLOAK_CONNECTION_ERROR = "KEYCLOAK_CONNECTION_ERROR";

    // === Database & Save Errors ===
    public static final String TIMESHEET_SAVING_DATA_TO_DATABASE_FAILED = "SAVING_TO_DATABASE_FAILED";
    public static final String SAVE_ERROR = "SAVING_DATA_ERROR";

    // === Role & Manager Errors ===
    public static final String ROLE_ASSIGNMENT_FAILED = "ROLE_ASSIGNMENT_FAILED";
    public static final String MANAGER_ROLE = "ERROR_MANAGER_ROLE";

    // === Project, Client, and Cost Center Errors ===
    public static final String PROJECT_CLIENT_ERROR = "PROJECT_CLIENT_ERROR";
    public static final String PROJECT_COST_CENTER_ERROR = "PROJECT_COST_CENTER_ERROR";
    public static final String PROJECT_ALREADY_EXISTS = "PROJECT_ALREADY_EXISTS";

    // Private constructor to prevent instantiation
    private ErrorCode() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
