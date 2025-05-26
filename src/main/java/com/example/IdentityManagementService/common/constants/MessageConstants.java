package com.example.IdentityManagementService.common.constants;

public final class MessageConstants {

    // === Client Messages ===
    public static final String CLIENT_STATUS_UPDATED = "Client %s status updated";
    public static final String CLIENT_UPDATED = "Client %s updated";
    public static final String CLIENT_CREATED = "Client %s created";

    // === Cost Center Messages ===
    public static final String COST_CENTER_CREATED = "CostCenter %s created";
    public static final String COST_CENTER_UPDATED = "CostCenter %s updated";
    public static final String COST_CENTER_STATUS_UPDATED = "CostCenter %s status updated";

    // === Project Messages ===
    public static final String PROJECT_CREATED = "Project Created: ";
    public static final String PROJECT_UPDATE = "Project Updated: ";
    public static final String PROJECT_STATUS_UPDATED = "Updated Employee status of %s in project %s";

    // === Employee Assignment Messages ===
    public static final String CREATION_EMAIL = "Timesheet Application Login Credentials";
    public static final String EMPLOYEE_ALREADY_ASSIGNED = "No new employees assigned (all were already assigned)";
    public static final String EMPLOYEE_ASSIGNED = " employee(s) assigned successfully.";
    public static final String EMPLOYEE_UPDATED_SUCCESSFULLY = "Employee Updated Successfully";
    public static final String MANAGER_NOT_ASSIGNED = "Manager Not Assigned";

    // === Role Messages ===
    public static final String ROLES_UPDATED_SUCCESSFULLY = "Roles Updated successfully";
    public static final String ROLES_ASSIGNED_SUCCESSFULLY = "Roles Assigned Successfully";
    public static final String ROLES_UNASSIGNED_SUCCESSFULLY = "Role Unassigned Successfully";

    // === User Account Messages ===
    public static final String PASSWORD_UPDATED_SUCCESSFULLY = "Password Updated successfully";
    public static final String USER_STATUS_UPDATED = "User Status updated successfully";

    // === Timesheet Messages ===
    public static final String DAILY_TIMESHEET_SAVED = "Daily Timesheet saved";
    public static final String SUBMITTED_TIMESHEET =
            "Timesheet submitted for employee %s for week starting %s (Month: %s, Year: %s)";
    public static final String TIMESHEET_APPROVED_BY_MANAGER =
            "Timesheet of employee %s for week starting %s has been approved by manager %s.";
    public static final String TIMESHEET_REJECTED_BY_MANAGER =
            "Timesheet of employee %s for week starting %s has been rejected by manager %s for correction.";
    public static final String APPROVED_ALL_TIMESHEETS_FOR_WEEK = "Approve All Timesheet For The week";

    // Private constructor to prevent instantiation
    private MessageConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
